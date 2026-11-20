package pe.gob.munquinua.sigmud.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import pe.gob.munquinua.sigmud.util.PanelDatos;

/**
 * Pruebas de PanelDatos (lo que recibe cada rol) contra mun_quinua_test: cada rol obtiene SOLO sus propios datos.
 * Las pruebas que insertan citas las marcan con TEST-PANEL y las borran al terminar.
 */
class PanelDatosTest extends BaseDaoTest {

    private final PanelDatos panel = new PanelDatos();

    @AfterEach
    void limpiar() {
        ejecutar("DELETE FROM cita WHERE motivo = ?", "TEST-PANEL");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> seccion(Map<String, Object> p, String nombre) {
        return (Map<String, Object>) p.get(nombre);
    }

    @Test
    void elPanelDelAdminTieneSusIndicadoresGraficosYListas() {
        Map<String, Object> p = panel.construir("ADMIN", 1);
        assertEquals(Set.of("usuarios", "contribuyentes", "materiales", "criticos", "citasHoy", "movimientosHoy", "bloqueadas", "loginFallidos"),
                seccion(p, "kpis").keySet());
        assertEquals(Set.of("citasPorEstado", "movimientosPorDia", "contribuyentesPorMes"), seccion(p, "graficos").keySet());
        assertEquals(Set.of("bloqueadas", "accesos", "actividad"), seccion(p, "listas").keySet());
    }

    @Test
    void elPanelDeAtencionNoContieneNingunDatoDelAdminNiDelAlmacen() {
        int idAtencion1 = contar("SELECT id_usuario FROM usuario WHERE username = 'atencion1'");
        Map<String, Object> p = panel.construir("ATENCION", idAtencion1);
        assertEquals(Set.of("total", "programadas", "atendidas", "canceladas"), seccion(p, "kpis").keySet());
        assertEquals(Set.of("atendidasPorDia"), seccion(p, "graficos").keySet());
        assertEquals(Set.of("proximas"), seccion(p, "listas").keySet());
        String texto = p.toString();
        assertFalse(texto.contains("usuarios=") || texto.contains("bloqueadas") || texto.contains("accesos")
                || texto.contains("materiales") || texto.contains("entradasMes"), "sin datos de otros roles: " + texto);
    }

    @Test
    void elPanelDeAtencionUsaSoloElFuncionarioIndicado() {
        int id1 = contar("SELECT id_usuario FROM usuario WHERE username = 'atencion1'");
        int id2 = contar("SELECT id_usuario FROM usuario WHERE username = 'atencion2'");
        ejecutar("INSERT INTO cita (id_contribuyente, id_area, id_funcionario, ventanilla, fecha, hora, motivo) "
                + "VALUES (1, 2, ?, 1, ?, ?, 'TEST-PANEL')", id2, LocalDate.now(), LocalTime.of(6, 0));   // cita de atencion2
        for (int id : List.of(id1, id2)) {
            Map<String, Object> kpis = seccion(panel.construir("ATENCION", id), "kpis");
            long esperado = consultarNumero("SELECT COUNT(*) FROM cita WHERE id_funcionario = ? AND fecha = ?", id, LocalDate.now());
            assertEquals(esperado, (Long) kpis.get("total"), "las citas del funcionario " + id);
        }
        assertEquals(consultarNumero("SELECT COUNT(*) FROM cita WHERE id_funcionario = ? AND fecha = ?", id1, LocalDate.now()),
                (Long) seccion(panel.construir("ATENCION", id1), "kpis").get("total"), "la cita de atencion2 no suma a atencion1");
    }

    @Test
    void elPanelDelAlmacenTieneSusIndicadoresGraficosYListas() {
        Map<String, Object> p = panel.construir("ALMACEN", 4);
        assertEquals(Set.of("materiales", "criticos", "movimientosHoy", "entradasMes", "salidasMes"), seccion(p, "kpis").keySet());
        assertEquals(Set.of("salidasPorArea"), seccion(p, "graficos").keySet());
        assertEquals(Set.of("reponer", "movimientos"), seccion(p, "listas").keySet());
    }

    @Test
    void unRolDesconocidoONuloNoTienePanel() {
        assertThrows(IllegalArgumentException.class, () -> panel.construir("OTRO", 1));
        assertThrows(IllegalArgumentException.class, () -> panel.construir(null, 1));
        assertThrows(IllegalArgumentException.class, () -> panel.construir("admin", 1), "el rol distingue mayúsculas");
    }

    @Test
    @SuppressWarnings("unchecked")
    void elPanelDelAdminCuentaLosLoginFallidosDe24HorasYListaLaActividadReciente() {
        ejecutar("TRUNCATE TABLE auditoria");
        try {
            var dao = new pe.gob.munquinua.sigmud.dao.impl.AuditoriaDAOImpl();
            for (int i = 0; i < 3; i++) {
                dao.registrar(pe.gob.munquinua.sigmud.util.Auditoria.construir(null, "intruso" + i, "LOGIN_FALLIDO", "SESION", null, "Login fallido", null, "1.1.1.1"));
            }
            for (int i = 0; i < 10; i++) {
                dao.registrar(pe.gob.munquinua.sigmud.util.Auditoria.construir(null, "admin", "LOGIN_OK", "SESION", null, "Inicio " + i, null, "1.1.1.1"));
            }
            Map<String, Object> p = panel.construir("ADMIN", 1);
            assertEquals(3L, seccion(p, "kpis").get("loginFallidos"));
            List<Map<String, Object>> actividad = (List<Map<String, Object>>) seccion(p, "listas").get("actividad");
            assertEquals(PanelDatos.ACTIVIDAD_RECIENTE, actividad.size(), "solo los ultimos 8 eventos");
            assertEquals("Inicio 9", actividad.get(0).get("descripcion"));
        } finally {
            ejecutar("TRUNCATE TABLE auditoria");
        }
    }

    @Test
    void elPanelDeAtencionNoTraeLosAccesosFallidosNiLaActividad() {
        int id = contar("SELECT id_usuario FROM usuario WHERE username = 'atencion1'");
        String texto = panel.construir("ATENCION", id).toString();
        assertFalse(texto.contains("loginFallidos") || texto.contains("actividad"));
    }
}

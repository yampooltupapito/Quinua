package pe.gob.munquinua.sigmud.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.gob.munquinua.sigmud.dao.impl.AuditoriaDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.CitaDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.ContribuyenteDAOImpl;
import pe.gob.munquinua.sigmud.model.Cita;
import pe.gob.munquinua.sigmud.model.Contribuyente;
import pe.gob.munquinua.sigmud.model.FiltrosAuditoria;
import pe.gob.munquinua.sigmud.model.RegistroAuditoria;
import pe.gob.munquinua.sigmud.model.ResumenCitas;
import pe.gob.munquinua.sigmud.util.Auditoria;

/**
 * Pruebas de lo que usa la FICHA del contribuyente (mun_quinua_test): resumen de citas, próxima cita, filtro por
 * estado, filtro de auditoría por entidad e id, y listado con dados de baja.
 * Usa dos contribuyentes propios (DNI 99000771 y 99000772) y citas con el motivo "TEST-FICHA"; todo se borra al final.
 */
class FichaContribuyenteDAOTest extends BaseDaoTest {

    private static final String MOTIVO = "TEST-FICHA";

    private final CitaDAO citaDao = new CitaDAOImpl();
    private final AuditoriaDAO auditoriaDao = new AuditoriaDAOImpl();
    private final ContribuyenteDAO contribuyenteDao = new ContribuyenteDAOImpl();
    private int conCitas;
    private int sinCitas;
    private int idFuncionario;
    private final LocalDate hoy = LocalDate.now();

    @BeforeEach
    void preparar() {
        limpiarTodo();
        ejecutar("INSERT INTO contribuyente (tipo_persona, dni_ruc, nombres_razon_social) VALUES ('NATURAL', '99000771', 'Ficha Con Citas')");
        ejecutar("INSERT INTO contribuyente (tipo_persona, dni_ruc, nombres_razon_social) VALUES ('NATURAL', '99000772', 'Ficha Sin Citas')");
        conCitas = contar("SELECT id_contribuyente FROM contribuyente WHERE dni_ruc = '99000771'");
        sinCitas = contar("SELECT id_contribuyente FROM contribuyente WHERE dni_ruc = '99000772'");
        idFuncionario = contar("SELECT id_usuario FROM usuario WHERE username = 'atencion1'");
    }

    @AfterEach
    void limpiarTodo() {
        ejecutar("DELETE FROM cita WHERE motivo = ?", MOTIVO);
        ejecutar("DELETE FROM contribuyente WHERE dni_ruc IN ('99000771', '99000772')");
        ejecutar("TRUNCATE TABLE auditoria");
    }

    private void cita(int contribuyente, LocalDate fecha, LocalTime hora, String estado) {
        ejecutar("INSERT INTO cita (id_contribuyente, id_area, id_funcionario, ventanilla, fecha, hora, motivo, estado) "
                + "VALUES (?, 1, ?, 1, ?, ?, ?, ?)", contribuyente, idFuncionario, Date.valueOf(fecha), Time.valueOf(hora), MOTIVO, estado);
    }

    /** 2 programadas futuras, 1 programada pasada, 2 atendidas y 1 cancelada = 6 citas. */
    private void sembrarCitas() {
        cita(conCitas, hoy.plusDays(9), LocalTime.of(5, 0), "PROGRAMADA");
        cita(conCitas, hoy.plusDays(3), LocalTime.of(5, 30), "PROGRAMADA");   // la más cercana
        cita(conCitas, hoy.minusDays(20), LocalTime.of(6, 0), "PROGRAMADA");  // vencida: no es "próxima"
        cita(conCitas, hoy.minusDays(10), LocalTime.of(6, 30), "ATENDIDA");
        cita(conCitas, hoy.minusDays(5), LocalTime.of(7, 0), "ATENDIDA");
        cita(conCitas, hoy.plusDays(1), LocalTime.of(7, 30), "CANCELADA");    // más cercana, pero cancelada
    }

    // ---------- resumenPorContribuyente ----------

    @Test
    void elResumenCuentaCadaEstado() {
        sembrarCitas();
        ResumenCitas r = citaDao.resumenPorContribuyente(conCitas);
        assertEquals(6, r.getTotal());
        assertEquals(3, r.getProgramadas());
        assertEquals(2, r.getAtendidas());
        assertEquals(1, r.getCanceladas());
        assertEquals(r.getTotal(), r.getProgramadas() + r.getAtendidas() + r.getCanceladas());
    }

    @Test
    void elResumenSinCitasEsCeroYNoNulo() {
        ResumenCitas r = citaDao.resumenPorContribuyente(sinCitas);
        assertEquals(0, r.getTotal());
        assertEquals(0, r.getProgramadas());
        assertEquals(0, r.getAtendidas());
        assertEquals(0, r.getCanceladas());
    }

    @Test
    void elResumenNoMezclaCitasDeOtroContribuyente() {
        sembrarCitas();
        cita(sinCitas, hoy.plusDays(2), LocalTime.of(4, 0), "PROGRAMADA");
        assertEquals(6, citaDao.resumenPorContribuyente(conCitas).getTotal());
        assertEquals(1, citaDao.resumenPorContribuyente(sinCitas).getTotal());
    }

    // ---------- proximaCita ----------

    @Test
    void laProximaCitaEsLaProgramadaMasCercanaDesdeHoy() {
        sembrarCitas();
        Cita p = citaDao.proximaCita(conCitas);
        assertNotNull(p);
        assertEquals(hoy.plusDays(3), p.getFecha());
        assertEquals(LocalTime.of(5, 30), p.getHora());
        assertEquals("PROGRAMADA", p.getEstado());
        assertNotNull(p.getNombreArea());
        assertNotNull(p.getNombreFuncionario());
    }

    @Test
    void sinCitasProgramadasLaProximaEsNull() {
        assertNull(citaDao.proximaCita(sinCitas));
        cita(sinCitas, hoy.minusDays(3), LocalTime.of(5, 0), "ATENDIDA");
        cita(sinCitas, hoy.plusDays(3), LocalTime.of(7, 0), "CANCELADA");
        cita(sinCitas, hoy.minusDays(1), LocalTime.of(7, 30), "PROGRAMADA");   // ya pasó
        assertNull(citaDao.proximaCita(sinCitas), "ni canceladas, ni atendidas, ni vencidas cuentan");
    }

    // ---------- listarPorContribuyente(id, estado) ----------

    @Test
    void sinFiltroListaTodasDeLaMasRecienteALaMasAntigua() {
        sembrarCitas();
        List<Cita> todas = citaDao.listarPorContribuyente(conCitas, null);
        assertEquals(6, todas.size());
        assertEquals(hoy.plusDays(9), todas.get(0).getFecha());
        assertEquals(hoy.minusDays(20), todas.get(5).getFecha());
        for (int i = 1; i < todas.size(); i++) {
            assertTrue(!todas.get(i).getFecha().isAfter(todas.get(i - 1).getFecha()), "orden descendente por fecha");
        }
        assertEquals(6, citaDao.listarPorContribuyente(conCitas, "").size(), "estado vacío = todos");
        assertNotNull(todas.get(0).getNombreArea());
        assertNotNull(todas.get(0).getNombreFuncionario());
    }

    @Test
    void elFiltroPorEstadoDevuelveSoloEseEstado() {
        sembrarCitas();
        assertEquals(3, citaDao.listarPorContribuyente(conCitas, "PROGRAMADA").size());
        List<Cita> atendidas = citaDao.listarPorContribuyente(conCitas, "ATENDIDA");
        assertEquals(2, atendidas.size());
        assertTrue(atendidas.stream().allMatch(c -> "ATENDIDA".equals(c.getEstado())));
        assertEquals(1, citaDao.listarPorContribuyente(conCitas, "CANCELADA").size());
        assertEquals(0, citaDao.listarPorContribuyente(sinCitas, "ATENDIDA").size());
    }

    @Test
    void unEstadoConInyeccionSqlSoloNoCoincide() {
        sembrarCitas();
        assertEquals(0, citaDao.listarPorContribuyente(conCitas, "PROGRAMADA' OR '1'='1").size());
        assertEquals(6, contar("SELECT COUNT(*) FROM cita WHERE motivo = ?", MOTIVO), "no se tocó nada");
    }

    @Test
    void elMetodoAnteriorSigueFuncionando() {
        sembrarCitas();
        assertEquals(6, citaDao.listarPorContribuyente(conCitas).size());
    }

    // ---------- auditoría por entidad e id ----------

    private void auditar(String accion, String entidad, Integer id, String descripcion) {
        auditoriaDao.registrar(Auditoria.construir(null, "admin", accion, entidad, id, descripcion, null, "127.0.0.1"));
    }

    @Test
    void elFiltroPorEntidadEIdSoloDevuelveEseContribuyente() {
        auditar(Auditoria.CONSULTA_FICHA, Auditoria.CONTRIBUYENTE, conCitas, "ficha A");
        auditar(Auditoria.ACTUALIZAR, Auditoria.CONTRIBUYENTE, conCitas, "cambio A");
        auditar(Auditoria.CONSULTA_FICHA, Auditoria.CONTRIBUYENTE, sinCitas, "ficha B");
        auditar(Auditoria.CREAR, Auditoria.MATERIAL, conCitas, "material con el mismo id");   // otra entidad

        FiltrosAuditoria f = new FiltrosAuditoria();
        f.setEntidad(Auditoria.CONTRIBUYENTE);
        f.setIdEntidad(conCitas);
        assertEquals(2, auditoriaDao.contar(f));
        assertTrue(auditoriaDao.buscar(f, 1, 20).stream().allMatch(r -> r.getIdEntidad() == conCitas
                && Auditoria.CONTRIBUYENTE.equals(r.getEntidad())));
    }

    @Test
    void ultimosPorEntidadDevuelveMaximo10LosMasNuevosPrimero() {
        for (int i = 1; i <= 12; i++) {
            auditar(Auditoria.CONSULTA_FICHA, Auditoria.CONTRIBUYENTE, conCitas, "consulta " + i);
        }
        auditar(Auditoria.CONSULTA_FICHA, Auditoria.CONTRIBUYENTE, sinCitas, "otro");
        List<RegistroAuditoria> r = auditoriaDao.ultimosPorEntidad(Auditoria.CONTRIBUYENTE, conCitas, 10);
        assertEquals(10, r.size());
        assertEquals("consulta 12", r.get(0).getDescripcion());
        assertEquals("consulta 3", r.get(9).getDescripcion());
        assertEquals(0, auditoriaDao.ultimosPorEntidad(Auditoria.CONTRIBUYENTE, 987654, 10).size());
    }

    @Test
    void consultaFichaEsUnaAccionValidaYSeGuarda() {
        assertTrue(Auditoria.ACCIONES.contains(Auditoria.CONSULTA_FICHA));
        auditar(Auditoria.CONSULTA_FICHA, Auditoria.CONTRIBUYENTE, conCitas, "ficha");
        assertEquals("CONSULTA_FICHA", auditoriaDao.recientes(1).get(0).getAccion());
    }

    // ---------- contribuyentes con dados de baja ----------

    @Test
    void listarConInactivosIncluyeLosDadosDeBajaYListarNo() {
        contribuyenteDao.darDeBaja(sinCitas);
        assertTrue(contribuyenteDao.listar().stream().noneMatch(c -> c.getIdContribuyente() == sinCitas),
                "el listado normal no cambia: solo activos");
        List<Contribuyente> todos = contribuyenteDao.listarConInactivos();
        Contribuyente baja = todos.stream().filter(c -> c.getIdContribuyente() == sinCitas).findFirst().orElseThrow();
        assertEquals(false, baja.isEstado());
        assertTrue(todos.stream().anyMatch(c -> c.getIdContribuyente() == conCitas));
    }

    @Test
    void buscarConInactivosEncuentraAlDadoDeBajaYBuscarNo() {
        contribuyenteDao.darDeBaja(sinCitas);
        assertEquals(0, contribuyenteDao.buscar("99000772").size());
        assertEquals(1, contribuyenteDao.buscarConInactivos("99000772").size());
        assertEquals(1, contribuyenteDao.buscarConInactivos("Ficha Con").size());
    }

    @Test
    void reactivarLoDevuelveAlListadoNormal() {
        contribuyenteDao.darDeBaja(sinCitas);
        contribuyenteDao.reactivar(sinCitas);
        assertTrue(contribuyenteDao.buscar("99000772").size() == 1);
    }
}

package pe.gob.munquinua.sigmud.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.gob.munquinua.sigmud.dao.impl.DashboardDAOImpl;
import pe.gob.munquinua.sigmud.model.Cita;
import pe.gob.munquinua.sigmud.model.EtiquetaValor;
import pe.gob.munquinua.sigmud.model.FilaStockCritico;
import pe.gob.munquinua.sigmud.model.MovimientoStock;
import pe.gob.munquinua.sigmud.model.PuntoDiario;
import pe.gob.munquinua.sigmud.model.Usuario;

/**
 * Pruebas de DashboardDAO (consultas de los paneles) contra mun_quinua_test.
 * Casi todas comparan contra un "antes" y un "después" de insertar datos conocidos, y contra consultas SQL directas.
 * Todo lo que se inserta lleva la marca TEST-DASH (o TST-9xx / 98xxxxxx) y se borra al terminar; los usuarios
 * se restauran a su estado original.
 */
class DashboardDAOTest extends BaseDaoTest {

    private static final String MARCA = "TEST-DASH";

    private final DashboardDAO dao = new DashboardDAOImpl();
    private final LocalDate hoy = LocalDate.now();
    private int idAtencion1;
    private int idAtencion2;
    private int idAlmacen;
    private int idArea1;
    private int idArea2;

    @BeforeEach
    void preparar() {
        idAtencion1 = contar("SELECT id_usuario FROM usuario WHERE username = 'atencion1'");
        idAtencion2 = contar("SELECT id_usuario FROM usuario WHERE username = 'atencion2'");
        idAlmacen = contar("SELECT id_usuario FROM usuario WHERE username = 'almacen1'");
        idArea1 = contar("SELECT id_area FROM area WHERE nombre = 'Mesa de Partes'");
        idArea2 = contar("SELECT id_area FROM area WHERE nombre = 'Rentas y Tributacion'");
    }

    @AfterEach
    void limpiar() {
        ejecutar("DELETE FROM movimiento_stock WHERE observacion = ?", MARCA);
        ejecutar("DELETE FROM material WHERE codigo LIKE 'TST-9%'");
        ejecutar("DELETE FROM cita WHERE motivo = ?", MARCA);
        ejecutar("DELETE FROM contribuyente WHERE dni_ruc LIKE '9800000%'");
        ejecutar("DELETE FROM usuario WHERE username LIKE 'tst_dash%'");
        ejecutar("UPDATE usuario SET intentos_fallidos = 0, estado = 1, ultimo_acceso = NULL");
    }

    // ---------- inserciones de datos de prueba ----------

    private int insertarMaterial(String codigo, int stock, int minimo, int estado) {
        ejecutar("INSERT INTO material (codigo, nombre, unidad, stock, stock_minimo, estado) VALUES (?, ?, 'UNIDAD', ?, ?, ?)",
                codigo, "Material " + codigo, stock, minimo, estado);
        return contar("SELECT id_material FROM material WHERE codigo = ?", codigo);
    }

    private void insertarMovimiento(int idMaterial, String tipo, int cantidad, Integer idArea, LocalDateTime fecha) {
        ejecutar("INSERT INTO movimiento_stock (id_material, tipo, cantidad, fecha, id_area, id_usuario, observacion) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)", idMaterial, tipo, cantidad, fecha, idArea, idAlmacen, MARCA);
    }

    private void insertarCita(int funcionario, int area, LocalDate fecha, LocalTime hora, String estado) {
        ejecutar("INSERT INTO cita (id_contribuyente, id_area, id_funcionario, ventanilla, fecha, hora, motivo, estado) "
                + "VALUES (1, ?, ?, 1, ?, ?, ?, ?)", area, funcionario, fecha, hora, MARCA, estado);
    }

    private void insertarContribuyente(String dni, LocalDateTime registro) {
        ejecutar("INSERT INTO contribuyente (tipo_persona, dni_ruc, nombres_razon_social, fecha_registro) "
                + "VALUES ('NATURAL', ?, 'Prueba Dashboard', ?)", dni, registro);
    }

    private LocalDateTime inicio(LocalDate d) {
        return d.atStartOfDay();
    }

    // =============================== ADMIN ===============================

    @Test
    void losTotalesDelAdminCoincidenConConsultasDirectas() {
        Map<String, Long> t = dao.totalesAdmin();
        assertEquals(contar("SELECT COUNT(*) FROM usuario WHERE estado = 1"), t.get("usuariosActivos"));
        assertEquals(contar("SELECT COUNT(*) FROM contribuyente WHERE estado = 1"), t.get("contribuyentesActivos"));
        assertEquals(contar("SELECT COUNT(*) FROM material WHERE estado = 1"), t.get("materialesActivos"));
        assertEquals(contar("SELECT COUNT(*) FROM material WHERE estado = 1 AND stock <= stock_minimo"), t.get("materialesCriticos"));
        assertEquals(contar("SELECT COUNT(*) FROM cita WHERE fecha = ?", hoy), t.get("citasHoy"));
        assertEquals(contar("SELECT COUNT(*) FROM movimiento_stock WHERE fecha >= ? AND fecha < ?", inicio(hoy), inicio(hoy.plusDays(1))),
                t.get("movimientosHoy"));
        assertEquals(4L, t.get("usuariosActivos"), "los datos de ejemplo tienen 4 usuarios activos");
    }

    @Test
    void losTotalesDelAdminSubenAlCrearDatos() {
        Map<String, Long> antes = dao.totalesAdmin();
        int material = insertarMaterial("TST-951", 1, 5, 1);                // activo y crítico (1 <= 5)
        insertarMaterial("TST-952", 50, 5, 1);                              // activo, NO crítico
        insertarMaterial("TST-953", 0, 9, 0);                               // dado de baja: no cuenta
        insertarContribuyente("98000001", LocalDateTime.now());
        insertarMovimiento(material, "ENTRADA", 3, null, LocalDateTime.now());
        insertarCita(idAtencion1, idArea1, hoy, LocalTime.of(6, 0), "PROGRAMADA");
        Map<String, Long> despues = dao.totalesAdmin();

        assertEquals(antes.get("materialesActivos") + 2, despues.get("materialesActivos"));
        assertEquals(antes.get("materialesCriticos") + 1, despues.get("materialesCriticos"));
        assertEquals(antes.get("contribuyentesActivos") + 1, despues.get("contribuyentesActivos"));
        assertEquals(antes.get("movimientosHoy") + 1, despues.get("movimientosHoy"));
        assertEquals(antes.get("citasHoy") + 1, despues.get("citasHoy"));
        assertEquals(antes.get("usuariosActivos"), despues.get("usuariosActivos"));
    }

    @Test
    void citasPorEstadoSiempreTrae3EstadosYCuentaSoloLosUltimos30Dias() {
        List<EtiquetaValor> antes = dao.citasPorEstado(30);
        assertEquals(List.of("PROGRAMADA", "ATENDIDA", "CANCELADA"),
                antes.stream().map(EtiquetaValor::getEtiqueta).collect(Collectors.toList()));
        insertarCita(idAtencion1, idArea1, hoy, LocalTime.of(6, 0), "PROGRAMADA");
        insertarCita(idAtencion1, idArea1, hoy, LocalTime.of(6, 30), "PROGRAMADA");
        insertarCita(idAtencion1, idArea1, hoy, LocalTime.of(7, 0), "ATENDIDA");
        insertarCita(idAtencion1, idArea1, hoy.minusDays(29), LocalTime.of(7, 30), "CANCELADA");   // dentro del rango
        insertarCita(idAtencion1, idArea1, hoy.minusDays(31), LocalTime.of(8, 0), "CANCELADA");    // fuera del rango
        List<EtiquetaValor> despues = dao.citasPorEstado(30);
        assertEquals(antes.get(0).getValor() + 2, despues.get(0).getValor());
        assertEquals(antes.get(1).getValor() + 1, despues.get(1).getValor());
        assertEquals(antes.get(2).getValor() + 1, despues.get(2).getValor());
    }

    @Test
    void movimientosPorDiaDevuelve14DiasConsecutivosCompletandoConCero() {
        List<PuntoDiario> antes = dao.movimientosPorDia(14);
        assertEquals(14, antes.size());
        assertEquals(hoy, antes.get(13).getFecha());
        assertEquals(hoy.minusDays(13), antes.get(0).getFecha());
        for (int i = 1; i < 14; i++) {
            assertEquals(antes.get(i - 1).getFecha().plusDays(1), antes.get(i).getFecha(), "días consecutivos");
        }
        int material = insertarMaterial("TST-954", 100, 5, 1);
        insertarMovimiento(material, "ENTRADA", 10, null, LocalDateTime.now());
        insertarMovimiento(material, "SALIDA", 4, idArea1, LocalDateTime.now());
        insertarMovimiento(material, "ENTRADA", 7, null, inicio(hoy.minusDays(5)).plusHours(10));
        insertarMovimiento(material, "ENTRADA", 1000, null, inicio(hoy.minusDays(20)));     // fuera de los 14 días
        List<PuntoDiario> despues = dao.movimientosPorDia(14);
        assertEquals(antes.get(13).getEntradas() + 10, despues.get(13).getEntradas());
        assertEquals(antes.get(13).getSalidas() + 4, despues.get(13).getSalidas());
        assertEquals(antes.get(8).getEntradas() + 7, despues.get(8).getEntradas(), "el movimiento de hace 5 días");
        long entradasEnRango = despues.stream().mapToLong(PuntoDiario::getEntradas).sum();
        assertEquals(contar("SELECT COALESCE(SUM(cantidad), 0) FROM movimiento_stock WHERE tipo = 'ENTRADA' AND fecha >= ? AND fecha < ?",
                inicio(hoy.minusDays(13)), inicio(hoy.plusDays(1))), entradasEnRango, "coincide con la suma directa");
    }

    @Test
    void contribuyentesPorMesDevuelve6MesesConLosRegistrosDeCadaMes() {
        List<EtiquetaValor> antes = dao.contribuyentesPorMes(6);
        assertEquals(6, antes.size());
        assertEquals(YearMonth.from(hoy).toString(), antes.get(5).getEtiqueta());
        assertEquals(YearMonth.from(hoy).minusMonths(5).toString(), antes.get(0).getEtiqueta());
        insertarContribuyente("98000001", LocalDateTime.now());
        insertarContribuyente("98000002", inicio(hoy.withDayOfMonth(1).minusMonths(2)).plusDays(3));   // hace 2 meses
        insertarContribuyente("98000003", inicio(hoy.withDayOfMonth(1).minusMonths(8)));               // fuera del rango
        List<EtiquetaValor> despues = dao.contribuyentesPorMes(6);
        assertEquals(antes.get(5).getValor() + 1, despues.get(5).getValor());
        assertEquals(antes.get(3).getValor() + 1, despues.get(3).getValor());
        assertEquals(antes.stream().mapToLong(EtiquetaValor::getValor).sum() + 2,
                despues.stream().mapToLong(EtiquetaValor::getValor).sum(), "el de hace 8 meses no entra");
    }

    @Test
    void cuentasBloqueadasListaSoloLasQueTienen5IntentosOMas() {
        assertTrue(dao.cuentasBloqueadas().isEmpty());
        ejecutar("UPDATE usuario SET intentos_fallidos = 5, estado = 0 WHERE username = 'almacen1'");
        ejecutar("UPDATE usuario SET intentos_fallidos = 4 WHERE username = 'atencion1'");   // 4: todavía no está bloqueada
        List<Usuario> bloqueadas = dao.cuentasBloqueadas();
        assertEquals(1, bloqueadas.size());
        assertEquals("almacen1", bloqueadas.get(0).getUsername());
        assertEquals(5, bloqueadas.get(0).getIntentosFallidos());
        assertEquals(null, bloqueadas.get(0).getPasswordHash(), "el hash nunca se consulta");
    }

    @Test
    void ultimosAccesosTrae5UsuariosDelMasRecienteAlMasAntiguo() {
        for (int i = 1; i <= 3; i++) {   // con los 4 de ejemplo hay 7 usuarios: debe devolver solo 5
            ejecutar("INSERT INTO usuario (username, password_hash, nombres, rol) VALUES (?, REPEAT('a', 64), ?, 'ATENCION')",
                    "tst_dash" + i, "Temporal " + i);
        }
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 8, 0);
        String[] usuarios = {"admin", "atencion1", "atencion2", "almacen1", "tst_dash1", "tst_dash2"};
        for (int i = 0; i < usuarios.length; i++) {
            ejecutar("UPDATE usuario SET ultimo_acceso = ? WHERE username = ?", base.plusHours(i), usuarios[i]);
        }                                       // tst_dash3 queda sin acceso (NULL): no debe aparecer
        List<Usuario> accesos = dao.ultimosAccesos();
        assertEquals(5, accesos.size());
        assertEquals("tst_dash2", accesos.get(0).getUsername(), "el más reciente va primero");
        assertEquals("atencion1", accesos.get(4).getUsername());
        for (int i = 1; i < 5; i++) {
            assertTrue(accesos.get(i - 1).getUltimoAcceso().isAfter(accesos.get(i).getUltimoAcceso()));
        }
    }

    // ============================== ATENCION ==============================

    @Test
    void misCitasHoySoloCuentaLasDelFuncionarioIndicado() {
        Map<String, Long> antes1 = dao.misCitasHoy(idAtencion1);
        Map<String, Long> antes2 = dao.misCitasHoy(idAtencion2);
        insertarCita(idAtencion1, idArea1, hoy, LocalTime.of(6, 0), "PROGRAMADA");
        insertarCita(idAtencion1, idArea1, hoy, LocalTime.of(6, 30), "PROGRAMADA");
        insertarCita(idAtencion1, idArea1, hoy, LocalTime.of(7, 0), "ATENDIDA");
        insertarCita(idAtencion1, idArea1, hoy, LocalTime.of(7, 30), "CANCELADA");
        insertarCita(idAtencion2, idArea2, hoy, LocalTime.of(6, 0), "PROGRAMADA");   // de OTRO funcionario
        insertarCita(idAtencion1, idArea1, hoy.plusDays(1), LocalTime.of(6, 0), "PROGRAMADA");   // de mañana: no cuenta
        Map<String, Long> d1 = dao.misCitasHoy(idAtencion1);
        Map<String, Long> d2 = dao.misCitasHoy(idAtencion2);

        assertEquals(antes1.get("total") + 4, d1.get("total"));
        assertEquals(antes1.get("programadas") + 2, d1.get("programadas"));
        assertEquals(antes1.get("atendidas") + 1, d1.get("atendidas"));
        assertEquals(antes1.get("canceladas") + 1, d1.get("canceladas"));
        assertEquals(antes2.get("total") + 1, d2.get("total"), "el otro funcionario solo suma su propia cita");
        assertEquals(contar("SELECT COUNT(*) FROM cita WHERE id_funcionario = ? AND fecha = ?", idAtencion1, hoy), d1.get("total"));
        assertEquals(0L, dao.misCitasHoy(idAlmacen).get("total"), "un usuario sin citas ve ceros (no las de otros)");
    }

    @Test
    void misProximasCitasSonSoloLasProgramadasDelFuncionarioDesdeLaHoraIndicada() {
        insertarCita(idAtencion1, idArea1, hoy, LocalTime.of(19, 0), "ATENDIDA");      // atendida: no es "próxima"
        insertarCita(idAtencion1, idArea1, hoy, LocalTime.of(21, 0), "PROGRAMADA");
        insertarCita(idAtencion1, idArea1, hoy, LocalTime.of(20, 0), "PROGRAMADA");
        insertarCita(idAtencion1, idArea1, hoy, LocalTime.of(20, 30), "CANCELADA");    // cancelada: no
        insertarCita(idAtencion2, idArea2, hoy, LocalTime.of(20, 15), "PROGRAMADA");   // de otro funcionario: no
        insertarCita(idAtencion1, idArea1, hoy.plusDays(1), LocalTime.of(20, 45), "PROGRAMADA");   // de mañana: no

        List<Cita> proximas = dao.misProximasCitasHoy(idAtencion1, LocalTime.of(19, 30));
        assertEquals(List.of(LocalTime.of(20, 0), LocalTime.of(21, 0)),
                proximas.stream().map(Cita::getHora).collect(Collectors.toList()), "ordenadas por hora");
        assertTrue(proximas.stream().allMatch(c -> c.getIdFuncionario() == idAtencion1 && "PROGRAMADA".equals(c.getEstado())));
        assertTrue(proximas.get(0).getNombreContribuyente() != null, "trae el nombre del contribuyente");
        assertEquals(1, dao.misProximasCitasHoy(idAtencion1, LocalTime.of(20, 30)).size(), "desde las 20:30 solo queda la de las 21:00");
        assertEquals(1, dao.misProximasCitasHoy(idAtencion2, LocalTime.of(19, 30)).size());
    }

    @Test
    void misProximasCitasTienenUnMaximoDe5() {
        for (int h = 15; h <= 21; h++) {
            insertarCita(idAtencion1, idArea1, hoy, LocalTime.of(h, 0), "PROGRAMADA");
        }
        List<Cita> proximas = dao.misProximasCitasHoy(idAtencion1, LocalTime.of(14, 0));
        assertEquals(5, proximas.size());
        assertEquals(LocalTime.of(15, 0), proximas.get(0).getHora());
    }

    @Test
    void misCitasAtendidasPorDiaSoloCuentaLasAtendidasDeEseFuncionario() {
        List<EtiquetaValor> antes = dao.misCitasAtendidasPorDia(idAtencion1, 7);
        assertEquals(7, antes.size());
        assertEquals(hoy.toString(), antes.get(6).getEtiqueta());
        assertEquals(hoy.minusDays(6).toString(), antes.get(0).getEtiqueta());
        insertarCita(idAtencion1, idArea1, hoy, LocalTime.of(6, 0), "ATENDIDA");
        insertarCita(idAtencion1, idArea1, hoy, LocalTime.of(6, 30), "PROGRAMADA");                  // no es atendida
        insertarCita(idAtencion1, idArea1, hoy.minusDays(3), LocalTime.of(6, 0), "ATENDIDA");
        insertarCita(idAtencion1, idArea1, hoy.minusDays(10), LocalTime.of(6, 0), "ATENDIDA");        // fuera de los 7 días
        insertarCita(idAtencion2, idArea2, hoy, LocalTime.of(6, 0), "ATENDIDA");                      // de otro funcionario
        List<EtiquetaValor> despues = dao.misCitasAtendidasPorDia(idAtencion1, 7);
        assertEquals(antes.get(6).getValor() + 1, despues.get(6).getValor());
        assertEquals(antes.get(3).getValor() + 1, despues.get(3).getValor());
        assertEquals(antes.stream().mapToLong(EtiquetaValor::getValor).sum() + 2,
                despues.stream().mapToLong(EtiquetaValor::getValor).sum());
    }

    // ============================== ALMACEN ==============================

    @Test
    void losTotalesDelAlmacenCoincidenConLasSumasDirectas() {
        int material = insertarMaterial("TST-961", 100, 5, 1);
        LocalDate mes = hoy.withDayOfMonth(1);
        Map<String, Long> antes = dao.totalesAlmacen();
        insertarMovimiento(material, "ENTRADA", 7, null, LocalDateTime.now());
        insertarMovimiento(material, "SALIDA", 3, idArea1, LocalDateTime.now());
        insertarMovimiento(material, "SALIDA", 100, idArea1, inicio(mes.minusDays(1)));   // del mes pasado: no cuenta
        Map<String, Long> despues = dao.totalesAlmacen();

        assertEquals(antes.get("entradasMes") + 7, despues.get("entradasMes"));
        assertEquals(antes.get("salidasMes") + 3, despues.get("salidasMes"));
        assertEquals(antes.get("movimientosHoy") + 2, despues.get("movimientosHoy"));
        assertEquals(antes.get("materialesActivos"), despues.get("materialesActivos"), "el material ya existía al tomar el \"antes\"");
        assertEquals(contar("SELECT COALESCE(SUM(cantidad), 0) FROM movimiento_stock WHERE tipo = 'ENTRADA' AND fecha >= ? AND fecha < ?",
                inicio(mes), inicio(mes.plusMonths(1))), despues.get("entradasMes"));
    }

    @Test
    void losMaterialesParaReponerSonLos5CriticosConMayorFaltante() {
        for (int k = 1; k <= 7; k++) {
            insertarMaterial("TST-97" + k, 0, k, 1);                  // stock 0, mínimo k: faltante k
        }
        insertarMaterial("TST-978", 0, 50, 0);                        // faltante 50 pero dado de baja: no entra
        insertarMaterial("TST-979", 100, 5, 1);                       // no es crítico: no entra
        List<FilaStockCritico> reponer = dao.materialesParaReponer();
        assertEquals(5, reponer.size());
        assertEquals("TST-977", reponer.get(0).getCodigo());
        assertEquals(7, reponer.get(0).getFaltante());
        for (int i = 1; i < 5; i++) {
            assertTrue(reponer.get(i - 1).getFaltante() >= reponer.get(i).getFaltante(), "de mayor a menor faltante");
        }
        assertTrue(reponer.stream().noneMatch(m -> m.getCodigo().equals("TST-978") || m.getCodigo().equals("TST-979")));
    }

    @Test
    void salidasPorAreaMesSumaPorAreaDeMayorAMenorYSoloDelMes() {
        int material = insertarMaterial("TST-962", 100, 5, 1);
        LocalDate mes = hoy.withDayOfMonth(1);
        List<EtiquetaValor> antes = dao.salidasPorAreaMes();
        insertarMovimiento(material, "SALIDA", 5, idArea1, LocalDateTime.now());
        insertarMovimiento(material, "SALIDA", 4, idArea1, LocalDateTime.now());
        insertarMovimiento(material, "SALIDA", 20, idArea2, LocalDateTime.now());
        insertarMovimiento(material, "ENTRADA", 99, null, LocalDateTime.now());                      // las entradas no cuentan
        insertarMovimiento(material, "SALIDA", 500, idArea2, inicio(mes.minusDays(1)));               // mes pasado
        List<EtiquetaValor> despues = dao.salidasPorAreaMes();

        assertEquals(valor(antes, "Mesa de Partes") + 9, valor(despues, "Mesa de Partes"));
        assertEquals(valor(antes, "Rentas y Tributacion") + 20, valor(despues, "Rentas y Tributacion"));
        for (int i = 1; i < despues.size(); i++) {
            assertTrue(despues.get(i - 1).getValor() >= despues.get(i).getValor(), "de mayor a menor");
        }
    }

    private long valor(List<EtiquetaValor> lista, String etiqueta) {
        return lista.stream().filter(e -> e.getEtiqueta().equals(etiqueta)).mapToLong(EtiquetaValor::getValor).findFirst().orElse(0);
    }

    @Test
    void losUltimosMovimientosSonLosMasRecientesPrimeroConUnLimite() {
        int material = insertarMaterial("TST-963", 100, 5, 1);
        LocalDateTime ahora = LocalDateTime.now().withNano(0);
        for (int i = 1; i <= 7; i++) {
            insertarMovimiento(material, i % 2 == 0 ? "SALIDA" : "ENTRADA", i, i % 2 == 0 ? idArea1 : null, ahora.plusMinutes(i));
        }
        List<MovimientoStock> ultimos = dao.ultimosMovimientos(5);
        assertEquals(5, ultimos.size());
        assertEquals(7, ultimos.get(0).getCantidad(), "el más reciente primero");
        assertEquals(3, ultimos.get(4).getCantidad());
        assertTrue(ultimos.get(0).getNombreMaterial().startsWith("TST-963"));
        assertEquals("Encargado de Almacen", ultimos.get(0).getNombreUsuario());
        assertFalse(dao.ultimosMovimientos(2).size() > 2);
    }
}

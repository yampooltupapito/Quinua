package pe.gob.munquinua.sigmud.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.gob.munquinua.sigmud.dao.impl.AuditoriaDAOImpl;
import pe.gob.munquinua.sigmud.model.FiltrosAuditoria;
import pe.gob.munquinua.sigmud.model.RegistroAuditoria;
import pe.gob.munquinua.sigmud.util.Auditoria;
import pe.gob.munquinua.sigmud.util.ConexionBD;

/**
 * Pruebas de AuditoriaDAO contra mun_quinua_test: registrar, cada filtro, paginación, conteo, inmutabilidad por
 * triggers y registro dentro de una transacción con rollback.
 * La tabla se vacía con TRUNCATE (que no dispara el trigger de DELETE) antes y después de cada prueba.
 */
class AuditoriaDAOTest extends BaseDaoTest {

    private final AuditoriaDAO dao = new AuditoriaDAOImpl();

    @BeforeEach
    @AfterEach
    void vaciar() {
        ejecutar("TRUNCATE TABLE auditoria");
    }

    private RegistroAuditoria registro(String usuario, String accion, String entidad, Integer id, String descripcion) {
        return Auditoria.construir(null, usuario, accion, entidad, id, descripcion, null, "127.0.0.1");
    }

    private FiltrosAuditoria todo() {
        FiltrosAuditoria f = new FiltrosAuditoria();
        f.setDesde(LocalDate.now().minusDays(1));
        f.setHasta(LocalDate.now().plusDays(1));
        return f;
    }

    // ---------- registrar ----------

    @Test
    void registrarGuardaTodosLosCampos() {
        int idUsuario = contar("SELECT id_usuario FROM usuario WHERE username = 'admin'");
        RegistroAuditoria r = Auditoria.construir(idUsuario, "admin", Auditoria.ACTUALIZAR, Auditoria.MATERIAL, 7,
                "Material actualizado", "stockMinimo: 5 -> 8", "10.0.0.9");
        dao.registrar(r);
        RegistroAuditoria g = dao.recientes(1).get(0);
        assertEquals("admin", g.getUsername());
        assertEquals(idUsuario, g.getIdUsuario());
        assertEquals("ACTUALIZAR", g.getAccion());
        assertEquals("MATERIAL", g.getEntidad());
        assertEquals(7, g.getIdEntidad());
        assertEquals("Material actualizado", g.getDescripcion());
        assertEquals("stockMinimo: 5 -> 8", g.getDetalle());
        assertEquals("10.0.0.9", g.getIp());
        assertTrue(g.getFechaHora().isAfter(LocalDateTime.now().minusMinutes(5)), "la fecha la pone la base");
    }

    @Test
    void registrarAceptaUsuarioEntidadYDetalleNulos() {
        dao.registrar(registro("intruso", Auditoria.LOGIN_FALLIDO, Auditoria.SESION, null, "Login fallido"));
        RegistroAuditoria g = dao.recientes(1).get(0);
        assertNull(g.getIdUsuario());
        assertNull(g.getIdEntidad());
        assertNull(g.getDetalle());
    }

    // ---------- filtros ----------

    private void sembrar() {
        dao.registrar(registro("admin", Auditoria.LOGIN_OK, Auditoria.SESION, null, "Inicio de sesión"));
        dao.registrar(registro("atencion1", Auditoria.CREAR, Auditoria.CONTRIBUYENTE, 1, "Contribuyente registrado: Ana"));
        dao.registrar(registro("atencion1", Auditoria.CITA_CREAR, Auditoria.CITA, 2, "Cita creada para Ana"));
        dao.registrar(registro("almacen1", Auditoria.MOVIMIENTO_STOCK, Auditoria.MATERIAL, 3, "Salida de 10 unidades"));
    }

    @Test
    void filtraPorUsuario() {
        sembrar();
        FiltrosAuditoria f = todo();
        f.setUsername("atencion");
        assertEquals(2, dao.contar(f));
        assertEquals(2, dao.buscar(f, 1, 20).size());
    }

    @Test
    void filtraPorAccion() {
        sembrar();
        FiltrosAuditoria f = todo();
        f.setAccion(Auditoria.LOGIN_OK);
        assertEquals(1, dao.contar(f));
        assertEquals("admin", dao.buscar(f, 1, 20).get(0).getUsername());
    }

    @Test
    void filtraPorEntidad() {
        sembrar();
        FiltrosAuditoria f = todo();
        f.setEntidad(Auditoria.CITA);
        assertEquals(1, dao.contar(f));
    }

    @Test
    void filtraPorTextoDeLaDescripcion() {
        sembrar();
        FiltrosAuditoria f = todo();
        f.setTexto("Ana");
        assertEquals(2, dao.contar(f));
    }

    @Test
    void elTextoConPorcentajeSeBuscaComoTextoNormal() {
        sembrar();
        FiltrosAuditoria f = todo();
        f.setTexto("%");
        assertEquals(0, dao.contar(f), "% no es comodín cuando lo escribe el usuario");
    }

    @Test
    void filtraPorRangoDeFechas() {
        sembrar();
        FiltrosAuditoria f = new FiltrosAuditoria();
        f.setDesde(LocalDate.now().minusDays(10));
        f.setHasta(LocalDate.now().minusDays(5));
        assertEquals(0, dao.contar(f), "los registros de hoy quedan fuera de un periodo pasado");
        f.setDesde(LocalDate.now());
        f.setHasta(LocalDate.now());
        assertEquals(4, dao.contar(f), "el día 'hasta' se incluye completo");
    }

    @Test
    void filtraPorMomentoDesde() {
        sembrar();
        FiltrosAuditoria f = new FiltrosAuditoria();
        f.setDesdeMomento(LocalDateTime.now().minusHours(24));
        assertEquals(4, dao.contar(f));
        f.setDesdeMomento(LocalDateTime.now().plusHours(1));
        assertEquals(0, dao.contar(f));
    }

    @Test
    void combinaVariosFiltros() {
        sembrar();
        FiltrosAuditoria f = todo();
        f.setUsername("atencion1");
        f.setEntidad(Auditoria.CONTRIBUYENTE);
        f.setAccion(Auditoria.CREAR);
        f.setTexto("Ana");
        assertEquals(1, dao.contar(f));
    }

    @Test
    void unaInyeccionSqlEnLosFiltrosSeTrataComoTexto() {
        sembrar();
        FiltrosAuditoria f = todo();
        f.setUsername("' OR '1'='1");
        assertEquals(0, dao.contar(f));
        f.setUsername("");
        f.setTexto("'; DROP TABLE auditoria; --");
        assertEquals(0, dao.contar(f));
        assertEquals(4, contar("SELECT COUNT(*) FROM auditoria"), "la tabla sigue intacta");
    }

    // ---------- paginación, conteo y orden ----------

    @Test
    void paginaYCuenta() {
        for (int i = 1; i <= 25; i++) {
            dao.registrar(registro("admin", Auditoria.LOGIN_OK, Auditoria.SESION, null, "Evento " + i));
        }
        FiltrosAuditoria f = todo();
        assertEquals(25, dao.contar(f));
        assertEquals(20, dao.buscar(f, 1, 20).size());
        List<RegistroAuditoria> segunda = dao.buscar(f, 2, 20);
        assertEquals(5, segunda.size());
        assertEquals("Evento 5", segunda.get(0).getDescripcion(), "orden descendente: lo más nuevo primero");
        assertEquals("Evento 1", segunda.get(4).getDescripcion());
        assertTrue(dao.buscar(f, 3, 20).isEmpty());
    }

    @Test
    void recientesDevuelveLosUltimosEnOrden() {
        for (int i = 1; i <= 10; i++) {
            dao.registrar(registro("admin", Auditoria.LOGIN_OK, Auditoria.SESION, null, "Evento " + i));
        }
        List<RegistroAuditoria> r = dao.recientes(8);
        assertEquals(8, r.size());
        assertEquals("Evento 10", r.get(0).getDescripcion());
        assertEquals("Evento 3", r.get(7).getDescripcion());
    }

    // ---------- inmutabilidad (triggers) ----------

    @Test
    void unUpdateDirectoFallaPorElTrigger() {
        dao.registrar(registro("admin", Auditoria.LOGIN_OK, Auditoria.SESION, null, "Original"));
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ejecutar("UPDATE auditoria SET descripcion = 'Alterado'"));
        assertTrue(e.getCause().getMessage().contains("La auditoría no se puede modificar ni eliminar"));
        assertEquals("Original", dao.recientes(1).get(0).getDescripcion());
    }

    @Test
    void unDeleteDirectoFallaPorElTrigger() {
        dao.registrar(registro("admin", Auditoria.LOGIN_OK, Auditoria.SESION, null, "Original"));
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ejecutar("DELETE FROM auditoria"));
        assertTrue(e.getCause().getMessage().contains("La auditoría no se puede modificar ni eliminar"));
        assertEquals(1, contar("SELECT COUNT(*) FROM auditoria"));
    }

    // ---------- transacciones ----------

    @Test
    void registrarDentroDeUnaTransaccionConRollbackNoDejaRastro() throws SQLException {
        try (Connection cn = ConexionBD.obtener()) {
            cn.setAutoCommit(false);
            dao.registrar(cn, registro("admin", Auditoria.MOVIMIENTO_STOCK, Auditoria.MATERIAL, 1, "No debe quedar"));
            cn.rollback();
        }
        assertEquals(0, contar("SELECT COUNT(*) FROM auditoria"));
    }

    @Test
    void registrarDentroDeUnaTransaccionConCommitQueda() throws SQLException {
        try (Connection cn = ConexionBD.obtener()) {
            cn.setAutoCommit(false);
            dao.registrar(cn, registro("admin", Auditoria.MOVIMIENTO_STOCK, Auditoria.MATERIAL, 1, "Sí debe quedar"));
            cn.commit();
        }
        assertEquals(1, contar("SELECT COUNT(*) FROM auditoria"));
    }

    // ---------- clave foránea ----------

    @Test
    void siSeBorraElUsuarioElRegistroConservaElNombre() {
        ejecutar("INSERT INTO usuario (username, password_hash, nombres, rol) "
                + "VALUES ('tst_aud', REPEAT('a', 64), 'Prueba Auditoria', 'ATENCION')");
        int id = contar("SELECT id_usuario FROM usuario WHERE username = 'tst_aud'");
        dao.registrar(Auditoria.construir(id, "tst_aud", Auditoria.LOGIN_OK, Auditoria.SESION, null, "Inicio", null, "1.1.1.1"));
        ejecutar("DELETE FROM usuario WHERE id_usuario = ?", id);
        RegistroAuditoria g = dao.recientes(1).get(0);
        assertNull(g.getIdUsuario(), "ON DELETE SET NULL");
        assertEquals("tst_aud", g.getUsername(), "el nombre queda en el registro");
    }
}

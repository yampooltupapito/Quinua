package pe.gob.munquinua.sigmud.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import pe.gob.munquinua.sigmud.dao.impl.ContribuyenteDAOImpl;
import pe.gob.munquinua.sigmud.exception.RegistroDuplicadoException;
import pe.gob.munquinua.sigmud.model.Contribuyente;

/** Pruebas de ContribuyenteDAO contra mun_quinua_test. Borran los contribuyentes de prueba (DNI 9900000x). */
class ContribuyenteDAOTest extends BaseDaoTest {

    private final ContribuyenteDAO dao = new ContribuyenteDAOImpl();

    @AfterEach
    void limpiar() {
        ejecutar("DELETE FROM contribuyente WHERE dni_ruc LIKE '9900000%'");
    }

    private Contribuyente nuevo(String dni, String nombre) {
        Contribuyente c = new Contribuyente();
        c.setTipoPersona(Contribuyente.NATURAL);
        c.setDniRuc(dni);
        c.setNombresRazonSocial(nombre);
        return c;
    }

    @Test
    void insertarGuardaElContribuyenteYDevuelveSuId() throws Exception {
        Contribuyente c = nuevo("99000001", "Prueba Uno Test");
        c.setTelefono("966000111");
        dao.insertar(c);
        assertTrue(c.getIdContribuyente() > 0);
        Contribuyente guardado = dao.buscarPorId(c.getIdContribuyente()).orElseThrow();
        assertEquals("Prueba Uno Test", guardado.getNombresRazonSocial());
        assertEquals("966000111", guardado.getTelefono());
        assertTrue(guardado.isEstado(), "un contribuyente nuevo queda activo");
        assertEquals(null, guardado.getEmail(), "los opcionales vacíos se guardan como NULL");
    }

    @Test
    void dniDuplicadoLanzaRegistroDuplicadoException() throws Exception {
        dao.insertar(nuevo("99000002", "Primero Test"));
        RegistroDuplicadoException e = assertThrows(RegistroDuplicadoException.class,
                () -> dao.insertar(nuevo("99000002", "Segundo Test")));
        assertTrue(e.getMessage().contains("DNI/RUC"));
        assertEquals(1, contar("SELECT COUNT(*) FROM contribuyente WHERE dni_ruc = '99000002'"));
    }

    @Test
    void darDeBajaEsLogicaYNoBorraLaFila() throws Exception {
        Contribuyente c = nuevo("99000003", "Baja Test");
        dao.insertar(c);
        dao.darDeBaja(c.getIdContribuyente());

        assertEquals(1, contar("SELECT COUNT(*) FROM contribuyente WHERE id_contribuyente = ?", c.getIdContribuyente()),
                "la fila sigue existiendo");
        assertFalse(dao.buscarPorId(c.getIdContribuyente()).orElseThrow().isEstado());
        assertTrue(dao.listar().stream().noneMatch(x -> x.getIdContribuyente() == c.getIdContribuyente()),
                "un contribuyente dado de baja no aparece en el listado de activos");
        assertTrue(dao.buscar("Baja Test").isEmpty(), "ni en la búsqueda");
    }

    @Test
    void reactivarVuelveAPonerloActivo() throws Exception {
        Contribuyente c = nuevo("99000004", "Reactivar Test");
        dao.insertar(c);
        dao.darDeBaja(c.getIdContribuyente());
        dao.reactivar(c.getIdContribuyente());
        assertTrue(dao.listar().stream().anyMatch(x -> x.getIdContribuyente() == c.getIdContribuyente()));
    }

    @Test
    void actualizarCambiaLosDatosPeroNoElEstado() throws Exception {
        Contribuyente c = nuevo("99000005", "Antes Test");
        dao.insertar(c);
        dao.darDeBaja(c.getIdContribuyente());
        c.setNombresRazonSocial("Despues Test");
        dao.actualizar(c);
        Contribuyente guardado = dao.buscarPorId(c.getIdContribuyente()).orElseThrow();
        assertEquals("Despues Test", guardado.getNombresRazonSocial());
        assertFalse(guardado.isEstado(), "actualizar no debe reactivar el registro");
    }

    @Test
    void actualizarConDniDeOtroContribuyenteLanzaDuplicado() throws Exception {
        dao.insertar(nuevo("99000006", "Uno Test"));
        Contribuyente otro = nuevo("99000007", "Dos Test");
        dao.insertar(otro);
        otro.setDniRuc("99000006");
        assertThrows(RegistroDuplicadoException.class, () -> dao.actualizar(otro));
    }

    @Test
    void existeDniRucExcluyeElIdIndicado() throws Exception {
        Contribuyente c = nuevo("99000008", "Existe Test");
        dao.insertar(c);
        assertTrue(dao.existeDniRuc("99000008", 0));
        assertFalse(dao.existeDniRuc("99000008", c.getIdContribuyente()));
        assertFalse(dao.existeDniRuc("99000009", 0));
    }

    @Test
    void buscarPorNombreODniEscapaLosComodinesDeLike() throws Exception {
        dao.insertar(nuevo("99000001", "Zeta Prueba Busqueda"));
        assertEquals(1, dao.buscar("zeta prueba").size());
        assertEquals(1, dao.buscar("99000001").size());
        assertTrue(dao.buscar("%").isEmpty(), "el % escrito por el usuario se busca como texto, no como comodín");
        assertTrue(dao.buscar("' OR '1'='1").isEmpty());
    }

    @Test
    void buscarPorDniRucDevuelveElContribuyente() throws Exception {
        dao.insertar(nuevo("99000002", "Por Dni Test"));
        assertTrue(dao.buscarPorDniRuc("99000002").isPresent());
        assertTrue(dao.buscarPorDniRuc("00000000").isEmpty());
    }
}

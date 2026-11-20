package pe.gob.munquinua.sigmud.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.gob.munquinua.sigmud.dao.impl.MovimientoDAOImpl;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.exception.MaterialNoDisponibleException;
import pe.gob.munquinua.sigmud.exception.StockInsuficienteException;
import pe.gob.munquinua.sigmud.model.MovimientoStock;

/**
 * Pruebas de MovimientoDAO (la transacción de entradas y salidas) contra mun_quinua_test.
 * Cada prueba crea su propio material TST-901 (stock 100) y al final borra sus movimientos y el material.
 */
class MovimientoDAOTest extends BaseDaoTest {

    private final MovimientoDAO dao = new MovimientoDAOImpl();
    private int idMaterial;
    private int idUsuario;
    private int idArea;

    @BeforeEach
    void crearMaterialDePrueba() {
        ejecutar("INSERT INTO material (codigo, nombre, unidad, stock, stock_minimo) VALUES ('TST-901', 'Material de prueba', 'UNIDAD', 100, 5)");
        idMaterial = contar("SELECT id_material FROM material WHERE codigo = 'TST-901'");
        idUsuario = contar("SELECT id_usuario FROM usuario WHERE username = 'almacen1'");
        idArea = contar("SELECT MIN(id_area) FROM area");
    }

    @AfterEach
    void limpiar() {
        ejecutar("DELETE FROM movimiento_stock WHERE id_material = ?", idMaterial);
        ejecutar("DELETE FROM material WHERE id_material = ?", idMaterial);
        ejecutar("TRUNCATE TABLE auditoria");   // el movimiento ahora también genera auditoría (TRUNCATE no dispara el trigger)
    }

    private int stock() {
        return contar("SELECT stock FROM material WHERE id_material = ?", idMaterial);
    }

    private int movimientos() {
        return contar("SELECT COUNT(*) FROM movimiento_stock WHERE id_material = ?", idMaterial);
    }

    @Test
    void laEntradaSumaStockYGuardaElMovimiento() throws Exception {
        int nuevoStock = dao.registrarMovimiento(idMaterial, "ENTRADA", 30, null, idUsuario, "compra");
        assertEquals(130, nuevoStock, "devuelve el stock actualizado");
        assertEquals(130, stock());
        assertEquals(1, movimientos());
    }

    @Test
    void laEntradaIgnoraElAreaYLaGuardaComoNull() throws Exception {
        dao.registrarMovimiento(idMaterial, "ENTRADA", 10, idArea, idUsuario, null);
        assertEquals(null, dao.listarPorMaterial(idMaterial).get(0).getIdArea());
    }

    @Test
    void laSalidaRestaStockYGuardaElArea() throws Exception {
        int nuevoStock = dao.registrarMovimiento(idMaterial, "SALIDA", 40, idArea, idUsuario, "entrega");
        assertEquals(60, nuevoStock);
        assertEquals(60, stock());
        MovimientoStock mv = dao.listarPorMaterial(idMaterial).get(0);
        assertEquals("SALIDA", mv.getTipo());
        assertEquals(40, mv.getCantidad());
        assertEquals(idArea, mv.getIdArea());
        assertEquals("entrega", mv.getObservacion());
    }

    @Test
    void laSalidaDeTodoElStockEsValida() throws Exception {
        assertEquals(0, dao.registrarMovimiento(idMaterial, "SALIDA", 100, idArea, idUsuario, null));
        assertEquals(0, stock());
    }

    @Test
    void salidaMayorAlStockLanzaStockInsuficienteYNoCambiaNada() {
        StockInsuficienteException e = assertThrows(StockInsuficienteException.class,
                () -> dao.registrarMovimiento(idMaterial, "SALIDA", 101, idArea, idUsuario, null));
        assertEquals(100, e.getStockDisponible());
        assertTrue(e.getMessage().contains("100"), "el mensaje incluye el stock disponible");
        assertEquals(100, stock(), "el stock no cambió");
        assertEquals(0, movimientos(), "no se creó ningún movimiento");
    }

    @Test
    void rollbackSiFallaElSegundoPasoNoDejaNadaGuardado() {
        // El segundo paso (INSERT del movimiento) falla por una clave foránea: el área 999999 no existe.
        assertThrows(DaoException.class,
                () -> dao.registrarMovimiento(idMaterial, "SALIDA", 10, 999999, idUsuario, null));
        assertEquals(100, stock(), "el stock quedó igual que antes (rollback)");
        assertEquals(0, movimientos(), "no quedó ningún movimiento (rollback)");
    }

    @Test
    void despuesDeUnRollbackLaSiguienteOperacionFuncionaNormalmente() throws Exception {
        assertThrows(DaoException.class,
                () -> dao.registrarMovimiento(idMaterial, "SALIDA", 10, 999999, idUsuario, null));
        assertEquals(90, dao.registrarMovimiento(idMaterial, "SALIDA", 10, idArea, idUsuario, null));
        assertEquals(1, movimientos());
    }

    @Test
    void materialInexistenteLanzaMaterialNoDisponible() {
        assertThrows(MaterialNoDisponibleException.class,
                () -> dao.registrarMovimiento(999999, "ENTRADA", 5, null, idUsuario, null));
    }

    @Test
    void materialDadoDeBajaNoAdmiteMovimientos() {
        ejecutar("UPDATE material SET estado = 0 WHERE id_material = ?", idMaterial);
        assertThrows(MaterialNoDisponibleException.class,
                () -> dao.registrarMovimiento(idMaterial, "ENTRADA", 5, null, idUsuario, null));
        assertEquals(100, stock());
        assertEquals(0, movimientos());
    }

    @Test
    void unTipoInvalidoSeRechazaSinTocarLaBase() {
        assertThrows(IllegalArgumentException.class,
                () -> dao.registrarMovimiento(idMaterial, "AJUSTE", 5, null, idUsuario, null));
        assertEquals(100, stock());
        assertEquals(0, movimientos());
    }

    @Test
    void elHistorialTraeLosMasRecientesPrimeroConNombres() throws Exception {
        dao.registrarMovimiento(idMaterial, "ENTRADA", 5, null, idUsuario, "primero");
        dao.registrarMovimiento(idMaterial, "SALIDA", 3, idArea, idUsuario, "segundo");
        List<MovimientoStock> historial = dao.listarPorMaterial(idMaterial);
        assertEquals(2, historial.size());
        assertEquals("segundo", historial.get(0).getObservacion(), "el más reciente va primero");
        assertEquals("Material de prueba", historial.get(0).getNombreMaterial());
        assertEquals("Encargado de Almacen", historial.get(0).getNombreUsuario());
        assertTrue(historial.get(0).getNombreArea() != null && historial.get(1).getNombreArea() == null);
    }

    @Test
    void variasOperacionesSeguidasMantienenElStockConsistenteConElHistorial() throws Exception {
        dao.registrarMovimiento(idMaterial, "ENTRADA", 50, null, idUsuario, null);
        dao.registrarMovimiento(idMaterial, "SALIDA", 20, idArea, idUsuario, null);
        dao.registrarMovimiento(idMaterial, "SALIDA", 5, idArea, idUsuario, null);
        long calculado = consultarNumero("SELECT COALESCE(SUM(CASE tipo WHEN 'ENTRADA' THEN cantidad ELSE -cantidad END), 0) "
                + "FROM movimiento_stock WHERE id_material = ?", idMaterial);
        assertEquals(100 + calculado, stock(), "stock inicial + entradas - salidas");
    }

    @Test
    void elMovimientoExitosoDejaUnRegistroDeAuditoriaConLaIp() throws Exception {
        dao.registrarMovimiento(idMaterial, "SALIDA", 10, idArea, idUsuario, "entrega", "10.1.2.3");
        assertEquals(1, contar("SELECT COUNT(*) FROM auditoria WHERE accion = 'MOVIMIENTO_STOCK' AND entidad = 'MATERIAL' AND id_entidad = ? AND ip = '10.1.2.3'", idMaterial));
        assertEquals(1, contar("SELECT COUNT(*) FROM auditoria WHERE id_usuario = ? AND username = 'almacen1'", idUsuario));
    }

    @Test
    void siFallaElMovimientoNoQuedaAuditoria() {
        assertThrows(StockInsuficienteException.class,
                () -> dao.registrarMovimiento(idMaterial, "SALIDA", 101, idArea, idUsuario, null, "10.1.2.3"));
        assertThrows(DaoException.class,
                () -> dao.registrarMovimiento(idMaterial, "SALIDA", 10, 999999, idUsuario, null, "10.1.2.3"));
        assertEquals(0, contar("SELECT COUNT(*) FROM auditoria"), "movimiento y auditoría se deshacen juntos");
        assertEquals(100, stock());
    }
}

package pe.gob.munquinua.sigmud.dao;

import java.util.List;
import pe.gob.munquinua.sigmud.exception.MaterialNoDisponibleException;
import pe.gob.munquinua.sigmud.exception.StockInsuficienteException;
import pe.gob.munquinua.sigmud.model.MovimientoStock;
import pe.gob.munquinua.sigmud.model.Pagina;

/** Movimientos de stock del almacén: entradas y salidas dentro de una transacción. */
public interface MovimientoDAO {

    /**
     * Registra una ENTRADA o SALIDA en UNA transacción: bloquea el material, valida el stock,
     * inserta el movimiento y actualiza el stock. Si algo falla, no queda ningún cambio.
     *
     * @param idArea solo se usa en SALIDA (en ENTRADA se guarda NULL)
     * @return el stock del material después del movimiento
     * @throws MaterialNoDisponibleException si el material no existe o está dado de baja
     * @throws StockInsuficienteException    si es SALIDA y la cantidad supera el stock
     * @throws pe.gob.munquinua.sigmud.exception.DaoException error de base de datos (ya con rollback)
     */
    default int registrarMovimiento(int idMaterial, String tipo, int cantidad, Integer idArea,
                            int idUsuario, String observacion)
            throws MaterialNoDisponibleException, StockInsuficienteException {
        return registrarMovimiento(idMaterial, tipo, cantidad, idArea, idUsuario, observacion, null);
    }

    /**
     * Igual que el anterior, y además guarda el registro de auditoría MOVIMIENTO_STOCK DENTRO de la misma transacción
     * (con la misma conexión, antes del commit): el movimiento, el cambio de stock y su auditoría se confirman o se
     * deshacen juntos.
     *
     * @param ip dirección IP de quien hace la operación (para la auditoría; puede ser null)
     */
    int registrarMovimiento(int idMaterial, String tipo, int cantidad, Integer idArea,
                            int idUsuario, String observacion, String ip)
            throws MaterialNoDisponibleException, StockInsuficienteException;

    /** Movimientos de un material, los más recientes primero, con nombres de material, área y usuario. */
    List<MovimientoStock> listarPorMaterial(int idMaterial);

    /**
     * Una página del historial de un material con orden por columna (fecha, tipo, cantidad, area o usuario; otro valor
     * = fecha descendente). LIMIT y OFFSET van como parámetros.
     */
    Pagina<MovimientoStock> listarPorMaterialPagina(int idMaterial, int pagina, int tamano, String orden, String dir);
}

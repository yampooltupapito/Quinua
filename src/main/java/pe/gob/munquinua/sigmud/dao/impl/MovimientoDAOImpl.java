package pe.gob.munquinua.sigmud.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import pe.gob.munquinua.sigmud.dao.MovimientoDAO;
import pe.gob.munquinua.sigmud.util.Auditoria;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.exception.MaterialNoDisponibleException;
import pe.gob.munquinua.sigmud.exception.StockInsuficienteException;
import pe.gob.munquinua.sigmud.model.MovimientoStock;
import pe.gob.munquinua.sigmud.model.Pagina;
import pe.gob.munquinua.sigmud.util.ConexionBD;
import pe.gob.munquinua.sigmud.util.OrdenSql;
import pe.gob.munquinua.sigmud.util.Paginador;

/** Implementación JDBC de MovimientoDAO: una sola transacción con bloqueo de fila, validación, INSERT, UPDATE y rollback. */
public class MovimientoDAOImpl implements MovimientoDAO {

    // FOR UPDATE bloquea la fila del material hasta el commit/rollback: nadie más puede
    // leer-modificar ese stock al mismo tiempo, así dos salidas simultáneas no descuentan de más.
    private static final String SQL_BLOQUEAR =
            "SELECT stock FROM material WHERE id_material = ? AND estado = 1 FOR UPDATE";
    private static final String SQL_INSERTAR =
            "INSERT INTO movimiento_stock (id_material, tipo, cantidad, id_area, id_usuario, observacion) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_SUMAR = "UPDATE material SET stock = stock + ? WHERE id_material = ?";
    private static final String SQL_RESTAR = "UPDATE material SET stock = stock - ? WHERE id_material = ?";

    private static final String SQL_HISTORIAL =
            "SELECT mv.id_movimiento, mv.id_material, mv.tipo, mv.cantidad, mv.fecha, mv.id_area, "
            + "mv.id_usuario, mv.observacion, m.nombre AS nombre_material, "
            + "a.nombre AS nombre_area, u.nombres AS nombre_usuario "
            + "FROM movimiento_stock mv "
            + "JOIN material m ON m.id_material = mv.id_material "
            + "LEFT JOIN area a ON a.id_area = mv.id_area "
            + "JOIN usuario u ON u.id_usuario = mv.id_usuario "
            + "WHERE mv.id_material = ? ORDER BY mv.fecha DESC, mv.id_movimiento DESC";

    @Override
    public int registrarMovimiento(int idMaterial, String tipo, int cantidad, Integer idArea,
                                   int idUsuario, String observacion, String ip)
            throws MaterialNoDisponibleException, StockInsuficienteException {
        boolean salida = MovimientoStock.SALIDA.equals(tipo);
        if (!salida && !MovimientoStock.ENTRADA.equals(tipo)) {
            throw new IllegalArgumentException("Tipo de movimiento no válido: " + tipo);
        }
        try (Connection cn = ConexionBD.obtener()) {
            try {
                cn.setAutoCommit(false);                                   // 1) inicia la transacción
                int stock = bloquearYLeerStock(cn, idMaterial);            // 2) SELECT ... FOR UPDATE
                if (salida && stock < cantidad) {                          // 3) regla de negocio
                    throw new StockInsuficienteException(stock);
                }
                insertarMovimiento(cn, idMaterial, tipo, cantidad, salida ? idArea : null, idUsuario, observacion);
                actualizarStock(cn, idMaterial, cantidad, salida);         // 4) INSERT + UPDATE
                int nuevoStock = salida ? stock - cantidad : stock + cantidad;
                auditar(cn, idMaterial, tipo, cantidad, idArea, idUsuario, observacion, ip, stock, nuevoStock);   // 4b) misma transacción
                cn.commit();                                               // 5) todo o nada
                return nuevoStock;
            } catch (SQLException | RuntimeException | MaterialNoDisponibleException
                     | StockInsuficienteException e) {
                cn.rollback();                                             // deshace todo lo hecho
                throw e;
            } finally {
                cn.setAutoCommit(true);                                    // la conexión queda como estaba
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo registrar el movimiento de stock", e);
        }
    }

    @Override
    public List<MovimientoStock> listarPorMaterial(int idMaterial) {
        List<MovimientoStock> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_HISTORIAL)) {
            ps.setInt(1, idMaterial);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DaoException("No se pudo listar los movimientos", e);
        }
    }

    private static final String COLUMNAS_PAGINA =
            "mv.id_movimiento, mv.id_material, mv.tipo, mv.cantidad, mv.fecha, mv.id_area, mv.id_usuario, "
            + "mv.observacion, m.nombre AS nombre_material, a.nombre AS nombre_area, u.nombres AS nombre_usuario";
    private static final String DESDE_PAGINA =
            "FROM movimiento_stock mv JOIN material m ON m.id_material = mv.id_material "
            + "LEFT JOIN area a ON a.id_area = mv.id_area JOIN usuario u ON u.id_usuario = mv.id_usuario "
            + "WHERE mv.id_material = ?";

    /** Lista blanca de orden: nombre que llega del navegador -> fragmento fijo de SQL. */
    public static final Map<String, String> ORDEN = Map.of("fecha", "mv.fecha", "tipo", "mv.tipo",
            "cantidad", "mv.cantidad", "area", "a.nombre", "usuario", "u.nombres");

    /** Orden efectivo: si la columna no está en la lista blanca se usa el orden por defecto (fecha, descendente). */
    public static OrdenSql.Orden resolverOrden(String orden, String dir) {
        return OrdenSql.resolver(ORDEN, orden, dir, "fecha", "desc", "mv.id_movimiento DESC");
    }

    @Override
    public Pagina<MovimientoStock> listarPorMaterialPagina(int idMaterial, int pagina, int tamano,
                                                           String orden, String dir) {
        return Paginador.ejecutar(COLUMNAS_PAGINA, DESDE_PAGINA, List.of(idMaterial), resolverOrden(orden, dir).sql(),
                pagina, tamano, this::mapear);
    }

    // ---------- pasos de la transacción ----------

    private static final String SQL_DATOS_AUDITORIA =
            "SELECT m.codigo, u.username FROM material m, usuario u WHERE m.id_material = ? AND u.id_usuario = ?";

    /**
     * Registra la auditoría MOVIMIENTO_STOCK con la MISMA conexión, antes del commit: si guardar la auditoría falla,
     * se deshace también el movimiento y el cambio de stock (los tres se confirman o se deshacen juntos).
     */
    private void auditar(Connection cn, int idMaterial, String tipo, int cantidad, Integer idArea, int idUsuario,
                         String observacion, String ip, int stockAntes, int stockDespues) throws SQLException {
        String codigo = "";
        String username = "";
        try (PreparedStatement ps = cn.prepareStatement(SQL_DATOS_AUDITORIA)) {
            ps.setInt(1, idMaterial);
            ps.setInt(2, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    codigo = rs.getString("codigo");
                    username = rs.getString("username");
                }
            }
        }
        String descripcion = tipo + " de " + cantidad + " unidades del material " + codigo
                + " (stock " + stockAntes + " -> " + stockDespues + ")";
        String detalle = "tipo: " + tipo + "; cantidad: " + cantidad + "; stock: " + stockAntes + " -> " + stockDespues
                + (idArea != null && MovimientoStock.SALIDA.equals(tipo) ? "; idArea: " + idArea : "")
                + (observacion == null || observacion.isBlank() ? "" : "; observacion: " + observacion);
        new AuditoriaDAOImpl().registrar(cn, Auditoria.construir(idUsuario, username, Auditoria.MOVIMIENTO_STOCK,
                Auditoria.MATERIAL, idMaterial, descripcion, detalle, ip));
    }

    /** Bloquea la fila del material y devuelve su stock; si no existe o está de baja, lanza excepción. */
    private int bloquearYLeerStock(Connection cn, int idMaterial)
            throws SQLException, MaterialNoDisponibleException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_BLOQUEAR)) {
            ps.setInt(1, idMaterial);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new MaterialNoDisponibleException();
                }
                return rs.getInt("stock");
            }
        }
    }

    private void insertarMovimiento(Connection cn, int idMaterial, String tipo, int cantidad,
                                    Integer idArea, int idUsuario, String observacion) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_INSERTAR)) {
            ps.setInt(1, idMaterial);
            ps.setString(2, tipo);
            ps.setInt(3, cantidad);
            if (idArea == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, idArea);
            }
            ps.setInt(5, idUsuario);
            ps.setString(6, observacion);
            ps.executeUpdate();
        }
    }

    private void actualizarStock(Connection cn, int idMaterial, int cantidad, boolean salida)
            throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(salida ? SQL_RESTAR : SQL_SUMAR)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, idMaterial);
            ps.executeUpdate();
        }
    }

    private MovimientoStock mapear(ResultSet rs) throws SQLException {
        MovimientoStock mv = new MovimientoStock();
        mv.setIdMovimiento(rs.getInt("id_movimiento"));
        mv.setIdMaterial(rs.getInt("id_material"));
        mv.setTipo(rs.getString("tipo"));
        mv.setCantidad(rs.getInt("cantidad"));
        Timestamp f = rs.getTimestamp("fecha");
        mv.setFecha(f == null ? null : f.toLocalDateTime());
        int area = rs.getInt("id_area");
        mv.setIdArea(rs.wasNull() ? null : area);
        mv.setIdUsuario(rs.getInt("id_usuario"));
        mv.setObservacion(rs.getString("observacion"));
        mv.setNombreMaterial(rs.getString("nombre_material"));
        mv.setNombreArea(rs.getString("nombre_area"));
        mv.setNombreUsuario(rs.getString("nombre_usuario"));
        return mv;
    }
}

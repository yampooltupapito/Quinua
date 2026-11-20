package pe.gob.munquinua.sigmud.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.gob.munquinua.sigmud.dao.ReporteAlmacenDAO;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.model.FilaResumenMaterial;
import pe.gob.munquinua.sigmud.model.FilaSalidaArea;
import pe.gob.munquinua.sigmud.model.FilaStockCritico;
import pe.gob.munquinua.sigmud.model.MovimientoStock;
import pe.gob.munquinua.sigmud.util.ConexionBD;

/**
 * Reportes del almacén. Todo el SQL está formado por texto FIJO; los valores del usuario
 * (fechas, tipo, área, material) entran siempre como parámetros "?".
 * El rango de fechas es [desde 00:00, hasta+1 día 00:00): incluye completo el día "hasta".
 */
public class ReporteAlmacenDAOImpl implements ReporteAlmacenDAO {

    // a) Stock crítico: filtro simple + columna calculada (faltante).
    private static final String SQL_STOCK_CRITICO =
            "SELECT m.codigo, m.nombre, m.unidad, m.stock, m.stock_minimo, "
            + "(m.stock_minimo - m.stock) AS faltante "
            + "FROM material m WHERE m.estado = 1 AND m.stock <= m.stock_minimo "
            + "ORDER BY faltante DESC, m.nombre";

    // b) Detalle de movimientos: JOIN a material, área (LEFT: las entradas no tienen área) y usuario.
    private static final String SQL_MOVIMIENTOS_BASE =
            "SELECT mv.id_movimiento, mv.id_material, mv.tipo, mv.cantidad, mv.fecha, mv.id_area, "
            + "mv.id_usuario, mv.observacion, CONCAT(m.codigo, ' - ', m.nombre) AS nombre_material, "
            + "a.nombre AS nombre_area, u.nombres AS nombre_usuario "
            + "FROM movimiento_stock mv "
            + "JOIN material m ON m.id_material = mv.id_material "
            + "LEFT JOIN area a ON a.id_area = mv.id_area "
            + "JOIN usuario u ON u.id_usuario = mv.id_usuario "
            + "WHERE mv.fecha >= ? AND mv.fecha < ?";
    // Fragmentos fijos para los filtros opcionales (se agregan solo si el filtro viene).
    private static final String FILTRO_TIPO = " AND mv.tipo = ?";
    private static final String FILTRO_AREA = " AND mv.id_area = ?";
    private static final String FILTRO_MATERIAL = " AND mv.id_material = ?";
    private static final String ORDEN_MOVIMIENTOS = " ORDER BY mv.fecha DESC, mv.id_movimiento DESC";

    // c) Resumen por material: SUM con CASE (condicional), COUNT y GROUP BY.
    private static final String SQL_RESUMEN =
            "SELECT m.codigo, m.nombre, m.unidad, "
            + "COALESCE(SUM(CASE WHEN mv.tipo = 'ENTRADA' THEN mv.cantidad ELSE 0 END), 0) AS total_entradas, "
            + "COALESCE(SUM(CASE WHEN mv.tipo = 'SALIDA' THEN mv.cantidad ELSE 0 END), 0) AS total_salidas, "
            + "COALESCE(SUM(CASE WHEN mv.tipo = 'ENTRADA' THEN mv.cantidad ELSE -mv.cantidad END), 0) AS saldo, "
            + "COUNT(mv.id_movimiento) AS cantidad_movimientos, "
            // Verificación: stock guardado vs. entradas - salidas de TODO el historial (sin filtro de fecha)
            + "m.stock AS stock_actual, "
            + "(SELECT COALESCE(SUM(CASE WHEN h.tipo = 'ENTRADA' THEN h.cantidad ELSE -h.cantidad END), 0) "
            + "FROM movimiento_stock h WHERE h.id_material = m.id_material) AS saldo_historico "
            + "FROM movimiento_stock mv JOIN material m ON m.id_material = mv.id_material "
            + "WHERE mv.fecha >= ? AND mv.fecha < ? "
            + "GROUP BY m.id_material, m.codigo, m.nombre, m.unidad, m.stock ORDER BY m.nombre";

    // d) Salidas por área: SUM y COUNT agrupados por área, de mayor a menor.
    private static final String SQL_SALIDAS_AREA =
            "SELECT a.nombre AS area, SUM(mv.cantidad) AS total_unidades, COUNT(*) AS cantidad_salidas "
            + "FROM movimiento_stock mv JOIN area a ON a.id_area = mv.id_area "
            + "WHERE mv.tipo = 'SALIDA' AND mv.fecha >= ? AND mv.fecha < ? "
            + "GROUP BY a.id_area, a.nombre ORDER BY total_unidades DESC, a.nombre";

    @Override
    public List<FilaStockCritico> stockCritico() {
        List<FilaStockCritico> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_STOCK_CRITICO);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FilaStockCritico f = new FilaStockCritico();
                f.setCodigo(rs.getString("codigo"));
                f.setNombre(rs.getString("nombre"));
                f.setUnidad(rs.getString("unidad"));
                f.setStock(rs.getInt("stock"));
                f.setStockMinimo(rs.getInt("stock_minimo"));
                f.setFaltante(rs.getInt("faltante"));
                lista.add(f);
            }
            return lista;
        } catch (SQLException e) {
            throw new DaoException("No se pudo generar el reporte de stock crítico", e);
        }
    }

    @Override
    public List<MovimientoStock> movimientosPorFecha(LocalDate desde, LocalDate hasta,
                                                     String tipo, Integer idArea, Integer idMaterial) {
        StringBuilder sql = new StringBuilder(SQL_MOVIMIENTOS_BASE);
        List<Object> parametros = new ArrayList<>();
        parametros.add(Timestamp.valueOf(desde.atStartOfDay()));
        parametros.add(Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
        if (tipo != null && !tipo.isEmpty()) {
            sql.append(FILTRO_TIPO);
            parametros.add(tipo);
        }
        if (idArea != null) {
            sql.append(FILTRO_AREA);
            parametros.add(idArea);
        }
        if (idMaterial != null) {
            sql.append(FILTRO_MATERIAL);
            parametros.add(idMaterial);
        }
        sql.append(ORDEN_MOVIMIENTOS);
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            asignar(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                return mapearMovimientos(rs);
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo generar el reporte de movimientos", e);
        }
    }

    @Override
    public List<FilaResumenMaterial> resumenPorMaterial(LocalDate desde, LocalDate hasta) {
        List<FilaResumenMaterial> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_RESUMEN)) {
            asignarPeriodo(ps, desde, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FilaResumenMaterial f = new FilaResumenMaterial();
                    f.setCodigo(rs.getString("codigo"));
                    f.setNombre(rs.getString("nombre"));
                    f.setUnidad(rs.getString("unidad"));
                    f.setTotalEntradas(rs.getLong("total_entradas"));
                    f.setTotalSalidas(rs.getLong("total_salidas"));
                    f.setSaldo(rs.getLong("saldo"));
                    f.setCantidadMovimientos(rs.getLong("cantidad_movimientos"));
                    f.setStockActual(rs.getLong("stock_actual"));
                    f.setSaldoHistorico(rs.getLong("saldo_historico"));
                    lista.add(f);
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DaoException("No se pudo generar el resumen por material", e);
        }
    }

    @Override
    public List<FilaSalidaArea> salidasPorArea(LocalDate desde, LocalDate hasta) {
        List<FilaSalidaArea> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_SALIDAS_AREA)) {
            asignarPeriodo(ps, desde, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FilaSalidaArea f = new FilaSalidaArea();
                    f.setArea(rs.getString("area"));
                    f.setTotalUnidades(rs.getLong("total_unidades"));
                    f.setCantidadSalidas(rs.getLong("cantidad_salidas"));
                    lista.add(f);
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DaoException("No se pudo generar el reporte de salidas por área", e);
        }
    }

    // ---------- auxiliares ----------

    private void asignarPeriodo(PreparedStatement ps, LocalDate desde, LocalDate hasta) throws SQLException {
        ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
        ps.setTimestamp(2, Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
    }

    /** Asigna los valores en orden a los "?" (los valores nunca se pegan al texto del SQL). */
    private void asignar(PreparedStatement ps, List<Object> parametros) throws SQLException {
        for (int i = 0; i < parametros.size(); i++) {
            ps.setObject(i + 1, parametros.get(i));
        }
    }

    private List<MovimientoStock> mapearMovimientos(ResultSet rs) throws SQLException {
        List<MovimientoStock> lista = new ArrayList<>();
        while (rs.next()) {
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
            lista.add(mv);
        }
        return lista;
    }
}

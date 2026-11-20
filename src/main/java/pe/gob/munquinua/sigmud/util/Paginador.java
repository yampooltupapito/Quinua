package pe.gob.munquinua.sigmud.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.model.Pagina;

/**
 * Ejecuta una consulta paginada: primero cuenta, corrige la página pedida (mínimo 1, máximo la última) y luego trae
 * solo esa página con LIMIT y OFFSET como PARÁMETROS. Todo el SQL que recibe es texto FIJO escrito por los DAO
 * (incluido el ORDER BY, que sale de la lista blanca de {@link OrdenSql}); los valores van siempre como parámetros.
 */
public final class Paginador {

    private Paginador() { }

    /** Convierte una fila en un objeto. */
    @FunctionalInterface
    public interface Mapeador<T> {
        T mapear(ResultSet rs) throws SQLException;
    }

    /**
     * @param columnas   lista fija de columnas del SELECT
     * @param desdeDonde texto fijo "FROM ... WHERE ..." (con ? para los filtros)
     * @param filtros    valores de los ? del FROM/WHERE
     * @param orden      cláusula " ORDER BY ..." ya resuelta con la lista blanca
     */
    public static <T> Pagina<T> ejecutar(String columnas, String desdeDonde, List<Object> filtros, String orden,
                                         int pagina, int tamano, Mapeador<T> mapeador) {
        int t = Pagina.normalizarTamano(tamano);
        try (Connection cn = ConexionBD.obtener()) {
            long total = contar(cn, desdeDonde, filtros);
            int p = Pagina.normalizarPagina(pagina, total, t);
            List<Object> parametros = new ArrayList<>(filtros);
            parametros.add(t);
            parametros.add(Pagina.desplazamiento(p, t));
            String sql = "SELECT " + columnas + " " + desdeDonde + orden + " LIMIT ? OFFSET ?";
            List<T> items = new ArrayList<>();
            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                asignar(ps, parametros);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(mapeador.mapear(rs));
                    }
                }
            }
            return new Pagina<>(items, p, t, total);
        } catch (SQLException e) {
            throw new DaoException("No se pudo consultar el listado", e);
        }
    }

    private static long contar(Connection cn, String desdeDonde, List<Object> filtros) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT COUNT(*) " + desdeDonde)) {
            asignar(ps, filtros);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static void asignar(PreparedStatement ps, List<Object> parametros) throws SQLException {
        for (int i = 0; i < parametros.size(); i++) {
            ps.setObject(i + 1, parametros.get(i));
        }
    }
}

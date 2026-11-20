package pe.gob.munquinua.sigmud.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import pe.gob.munquinua.sigmud.dao.MaterialDAO;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.exception.RegistroDuplicadoException;
import pe.gob.munquinua.sigmud.model.Material;
import pe.gob.munquinua.sigmud.model.Pagina;
import pe.gob.munquinua.sigmud.util.ConexionBD;
import pe.gob.munquinua.sigmud.util.OrdenSql;
import pe.gob.munquinua.sigmud.util.Paginador;

/** Implementación JDBC de MaterialDAO: el INSERT fija stock 0 y el UPDATE nunca toca el stock. */
public class MaterialDAOImpl implements MaterialDAO {

    /** Código de error de MySQL para "Duplicate entry" (violación de UNIQUE). */
    private static final int MYSQL_DUPLICADO = 1062;

    private static final String COLUMNAS = "id_material, codigo, nombre, unidad, stock, stock_minimo, estado";

    private static final String SQL_LISTAR =
            "SELECT " + COLUMNAS + " FROM material WHERE estado = 1 ORDER BY nombre";
    private static final String SQL_BUSCAR =
            "SELECT " + COLUMNAS + " FROM material WHERE estado = 1 "
            + "AND (codigo LIKE ? OR nombre LIKE ?) ORDER BY nombre";
    private static final String SQL_CRITICO =
            "SELECT " + COLUMNAS + " FROM material WHERE estado = 1 AND stock <= stock_minimo ORDER BY nombre";
    private static final String SQL_POR_ID = "SELECT " + COLUMNAS + " FROM material WHERE id_material = ?";
    private static final String SQL_POR_CODIGO = "SELECT " + COLUMNAS + " FROM material WHERE codigo = ?";

    /** Lista blanca de orden: nombre que llega del navegador -> fragmento fijo de SQL. */
    public static final Map<String, String> ORDEN = Map.of("codigo", "codigo", "nombre", "nombre", "unidad", "unidad",
            "stock", "stock", "minimo", "stock_minimo");

    /** Orden efectivo: si la columna no está en la lista blanca se usa el orden por defecto (nombre, ascendente). */
    public static OrdenSql.Orden resolverOrden(String orden, String dir) {
        return OrdenSql.resolver(ORDEN, orden, dir, "nombre", "asc", "id_material");
    }

    // El stock empieza en 0 (valor fijo en el SQL, no viene del objeto).
    private static final String SQL_INSERTAR =
            "INSERT INTO material (codigo, nombre, unidad, stock, stock_minimo) VALUES (?, ?, ?, 0, ?)";
    // Este UPDATE no incluye la columna stock a propósito.
    private static final String SQL_ACTUALIZAR =
            "UPDATE material SET codigo = ?, nombre = ?, unidad = ?, stock_minimo = ? WHERE id_material = ?";
    private static final String SQL_ESTADO = "UPDATE material SET estado = ? WHERE id_material = ?";

    @Override
    public List<Material> listar() {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            return mapearLista(rs);
        } catch (SQLException e) {
            throw new DaoException("No se pudo listar los materiales", e);
        }
    }

    @Override
    public List<Material> buscar(String texto) {
        String patron = "%" + escaparLike(texto == null ? "" : texto.trim()) + "%";
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_BUSCAR)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            try (ResultSet rs = ps.executeQuery()) {
                return mapearLista(rs);
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo buscar materiales", e);
        }
    }

    @Override
    public Pagina<Material> listarPagina(String texto, boolean soloCriticos, int pagina, int tamano,
                                         String orden, String dir) {
        StringBuilder desde = new StringBuilder("FROM material WHERE estado = 1");
        List<Object> filtros = new ArrayList<>();
        if (soloCriticos) {
            desde.append(" AND stock <= stock_minimo");
        }
        if (texto != null && !texto.trim().isEmpty()) {
            desde.append(" AND (codigo LIKE ? OR nombre LIKE ?)");
            String patron = "%" + escaparLike(texto.trim()) + "%";
            filtros.add(patron);
            filtros.add(patron);
        }
        return Paginador.ejecutar(COLUMNAS, desde.toString(), filtros, resolverOrden(orden, dir).sql(), pagina, tamano,
                this::mapear);
    }

    @Override
    public List<Material> listarStockCritico() {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_CRITICO);
             ResultSet rs = ps.executeQuery()) {
            return mapearLista(rs);
        } catch (SQLException e) {
            throw new DaoException("No se pudo listar el stock crítico", e);
        }
    }

    @Override
    public Optional<Material> buscarPorId(int id) {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_POR_ID)) {
            ps.setInt(1, id);
            return primero(ps);
        } catch (SQLException e) {
            throw new DaoException("No se pudo buscar el material", e);
        }
    }

    @Override
    public Optional<Material> buscarPorCodigo(String codigo) {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_POR_CODIGO)) {
            ps.setString(1, codigo);
            return primero(ps);
        } catch (SQLException e) {
            throw new DaoException("No se pudo buscar el material", e);
        }
    }

    @Override
    public void insertar(Material m) throws RegistroDuplicadoException {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getCodigo());
            ps.setString(2, m.getNombre());
            ps.setString(3, m.getUnidad());
            ps.setInt(4, m.getStockMinimo());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    m.setIdMaterial(keys.getInt(1));
                }
            }
            m.setStock(0);
        } catch (SQLException e) {
            lanzarSegunError(e, "No se pudo registrar el material");
        }
    }

    @Override
    public void actualizar(Material m) throws RegistroDuplicadoException {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_ACTUALIZAR)) {
            ps.setString(1, m.getCodigo());
            ps.setString(2, m.getNombre());
            ps.setString(3, m.getUnidad());
            ps.setInt(4, m.getStockMinimo());
            ps.setInt(5, m.getIdMaterial());
            ps.executeUpdate();
        } catch (SQLException e) {
            lanzarSegunError(e, "No se pudo actualizar el material");
        }
    }

    @Override
    public void darDeBaja(int id) {
        cambiarEstado(id, false);
    }

    @Override
    public void reactivar(int id) {
        cambiarEstado(id, true);
    }

    // ---------- métodos privados ----------

    private void cambiarEstado(int id, boolean activo) {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_ESTADO)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("No se pudo cambiar el estado del material", e);
        }
    }

    /** UNIQUE del código (error 1062) -> RegistroDuplicadoException; cualquier otro -> DaoException. */
    private void lanzarSegunError(SQLException e, String mensajeGeneral) throws RegistroDuplicadoException {
        if (e instanceof SQLIntegrityConstraintViolationException && e.getErrorCode() == MYSQL_DUPLICADO) {
            throw new RegistroDuplicadoException("Ya existe un material con ese código.", e);
        }
        throw new DaoException(mensajeGeneral, e);
    }

    private Optional<Material> primero(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
        }
    }

    private List<Material> mapearLista(ResultSet rs) throws SQLException {
        List<Material> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(mapear(rs));
        }
        return lista;
    }

    /** Convierte la fila actual del ResultSet en un Material. */
    private Material mapear(ResultSet rs) throws SQLException {
        Material m = new Material();
        m.setIdMaterial(rs.getInt("id_material"));
        m.setCodigo(rs.getString("codigo"));
        m.setNombre(rs.getString("nombre"));
        m.setUnidad(rs.getString("unidad"));
        m.setStock(rs.getInt("stock"));
        m.setStockMinimo(rs.getInt("stock_minimo"));
        m.setEstado(rs.getBoolean("estado"));
        return m;
    }

    /** Para que % y _ escritos por el usuario se busquen como texto normal. */
    private String escaparLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

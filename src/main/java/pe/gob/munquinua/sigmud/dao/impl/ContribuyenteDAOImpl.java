package pe.gob.munquinua.sigmud.dao.impl;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import pe.gob.munquinua.sigmud.dao.ContribuyenteDAO;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.exception.RegistroDuplicadoException;
import pe.gob.munquinua.sigmud.model.Contribuyente;
import pe.gob.munquinua.sigmud.model.Pagina;
import pe.gob.munquinua.sigmud.util.ConexionBD;
import pe.gob.munquinua.sigmud.util.OrdenSql;
import pe.gob.munquinua.sigmud.util.Paginador;

/** Implementación JDBC de ContribuyenteDAO (PreparedStatement y try-with-resources). */
public class ContribuyenteDAOImpl implements ContribuyenteDAO {

    /** Código de error de MySQL para "Duplicate entry" (violación de UNIQUE). */
    private static final int MYSQL_DUPLICADO = 1062;

    private static final String COLUMNAS =
            "id_contribuyente, tipo_persona, dni_ruc, nombres_razon_social, fecha_nacimiento, "
            + "telefono, email, direccion, estado, fecha_registro";

    private static final String SQL_LISTAR =
            "SELECT " + COLUMNAS + " FROM contribuyente WHERE estado = 1 ORDER BY nombres_razon_social";
    private static final String SQL_POR_ID =
            "SELECT " + COLUMNAS + " FROM contribuyente WHERE id_contribuyente = ?";
    private static final String SQL_POR_DNI_RUC =
            "SELECT " + COLUMNAS + " FROM contribuyente WHERE dni_ruc = ?";
    private static final String SQL_BUSCAR =
            "SELECT " + COLUMNAS + " FROM contribuyente WHERE estado = 1 "
            + "AND (nombres_razon_social LIKE ? OR dni_ruc LIKE ?) ORDER BY nombres_razon_social";
    private static final String SQL_LISTAR_TODOS =
            "SELECT " + COLUMNAS + " FROM contribuyente ORDER BY nombres_razon_social";
    private static final String SQL_BUSCAR_TODOS =
            "SELECT " + COLUMNAS + " FROM contribuyente "
            + "WHERE (nombres_razon_social LIKE ? OR dni_ruc LIKE ?) ORDER BY nombres_razon_social";
    /** Lista blanca de orden: nombre que llega del navegador -> fragmento fijo de SQL. */
    public static final Map<String, String> ORDEN = Map.of("dniRuc", "dni_ruc", "nombre", "nombres_razon_social",
            "tipo", "tipo_persona", "telefono", "telefono", "email", "email");

    /** Orden efectivo: si la columna no está en la lista blanca se usa el orden por defecto (nombre, ascendente). */
    public static OrdenSql.Orden resolverOrden(String orden, String dir) {
        return OrdenSql.resolver(ORDEN, orden, dir, "nombre", "asc", "id_contribuyente");
    }

    private static final String SQL_INSERTAR =
            "INSERT INTO contribuyente (tipo_persona, dni_ruc, nombres_razon_social, fecha_nacimiento, "
            + "telefono, email, direccion) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_ACTUALIZAR =
            "UPDATE contribuyente SET tipo_persona = ?, dni_ruc = ?, nombres_razon_social = ?, "
            + "fecha_nacimiento = ?, telefono = ?, email = ?, direccion = ? WHERE id_contribuyente = ?";
    private static final String SQL_ESTADO =
            "UPDATE contribuyente SET estado = ? WHERE id_contribuyente = ?";
    private static final String SQL_EXISTE =
            "SELECT 1 FROM contribuyente WHERE dni_ruc = ? AND id_contribuyente <> ?";

    @Override
    public List<Contribuyente> listar() {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            return mapearLista(rs);
        } catch (SQLException e) {
            throw new DaoException("No se pudo listar los contribuyentes", e);
        }
    }

    @Override
    public Pagina<Contribuyente> listarPagina(String texto, boolean incluirInactivos, int pagina, int tamano,
                                              String orden, String dir) {
        StringBuilder desde = new StringBuilder("FROM contribuyente WHERE 1 = 1");
        List<Object> filtros = new ArrayList<>();
        if (!incluirInactivos) {
            desde.append(" AND estado = 1");
        }
        if (texto != null && !texto.trim().isEmpty()) {
            desde.append(" AND (nombres_razon_social LIKE ? OR dni_ruc LIKE ?)");
            String patron = "%" + escaparLike(texto.trim()) + "%";
            filtros.add(patron);
            filtros.add(patron);
        }
        return Paginador.ejecutar(COLUMNAS, desde.toString(), filtros, resolverOrden(orden, dir).sql(), pagina, tamano,
                this::mapear);
    }

    @Override
    public List<Contribuyente> listarConInactivos() {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_LISTAR_TODOS);
             ResultSet rs = ps.executeQuery()) {
            return mapearLista(rs);
        } catch (SQLException e) {
            throw new DaoException("No se pudo listar los contribuyentes", e);
        }
    }

    @Override
    public List<Contribuyente> buscarConInactivos(String texto) {
        String patron = "%" + escaparLike(texto == null ? "" : texto.trim()) + "%";
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_BUSCAR_TODOS)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            try (ResultSet rs = ps.executeQuery()) {
                return mapearLista(rs);
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo buscar contribuyentes", e);
        }
    }

    @Override
    public Optional<Contribuyente> buscarPorId(int id) {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_POR_ID)) {
            ps.setInt(1, id);
            return primero(ps);
        } catch (SQLException e) {
            throw new DaoException("No se pudo buscar el contribuyente", e);
        }
    }

    @Override
    public Optional<Contribuyente> buscarPorDniRuc(String dniRuc) {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_POR_DNI_RUC)) {
            ps.setString(1, dniRuc);
            return primero(ps);
        } catch (SQLException e) {
            throw new DaoException("No se pudo buscar el contribuyente", e);
        }
    }

    @Override
    public List<Contribuyente> buscar(String texto) {
        String patron = "%" + escaparLike(texto == null ? "" : texto.trim()) + "%";
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_BUSCAR)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            try (ResultSet rs = ps.executeQuery()) {
                return mapearLista(rs);
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo buscar contribuyentes", e);
        }
    }

    @Override
    public void insertar(Contribuyente c) throws RegistroDuplicadoException {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            asignarDatos(ps, c);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    c.setIdContribuyente(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            lanzarSegunError(e, "No se pudo registrar el contribuyente");
        }
    }

    @Override
    public void actualizar(Contribuyente c) throws RegistroDuplicadoException {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_ACTUALIZAR)) {
            asignarDatos(ps, c);
            ps.setInt(8, c.getIdContribuyente());
            ps.executeUpdate();
        } catch (SQLException e) {
            lanzarSegunError(e, "No se pudo actualizar el contribuyente");
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

    @Override
    public boolean existeDniRuc(String dniRuc, int idExcluido) {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_EXISTE)) {
            ps.setString(1, dniRuc);
            ps.setInt(2, idExcluido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo verificar el DNI/RUC", e);
        }
    }

    // ---------- métodos privados ----------

    private void cambiarEstado(int id, boolean activo) {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_ESTADO)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("No se pudo cambiar el estado del contribuyente", e);
        }
    }

    /** Parámetros 1 a 7, comunes a INSERT y UPDATE. Los textos vacíos se guardan como NULL. */
    private void asignarDatos(PreparedStatement ps, Contribuyente c) throws SQLException {
        ps.setString(1, c.getTipoPersona());
        ps.setString(2, c.getDniRuc());
        ps.setString(3, c.getNombresRazonSocial());
        if (c.getFechaNacimiento() == null) {
            ps.setNull(4, Types.DATE);
        } else {
            ps.setDate(4, Date.valueOf(c.getFechaNacimiento()));
        }
        ps.setString(5, nuloSiVacio(c.getTelefono()));
        ps.setString(6, nuloSiVacio(c.getEmail()));
        ps.setString(7, nuloSiVacio(c.getDireccion()));
    }

    /** UNIQUE de DNI/RUC (error 1062) -> RegistroDuplicadoException; cualquier otro -> DaoException. */
    private void lanzarSegunError(SQLException e, String mensajeGeneral) throws RegistroDuplicadoException {
        if (e instanceof SQLIntegrityConstraintViolationException && e.getErrorCode() == MYSQL_DUPLICADO) {
            throw new RegistroDuplicadoException("Ya existe un contribuyente con ese DNI/RUC.", e);
        }
        throw new DaoException(mensajeGeneral, e);
    }

    private Optional<Contribuyente> primero(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
        }
    }

    private List<Contribuyente> mapearLista(ResultSet rs) throws SQLException {
        List<Contribuyente> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(mapear(rs));
        }
        return lista;
    }

    /** Convierte la fila actual del ResultSet en un Contribuyente. */
    private Contribuyente mapear(ResultSet rs) throws SQLException {
        Contribuyente c = new Contribuyente();
        c.setIdContribuyente(rs.getInt("id_contribuyente"));
        c.setTipoPersona(rs.getString("tipo_persona"));
        c.setDniRuc(rs.getString("dni_ruc"));
        c.setNombresRazonSocial(rs.getString("nombres_razon_social"));
        Date nac = rs.getDate("fecha_nacimiento");
        c.setFechaNacimiento(nac == null ? null : nac.toLocalDate());
        c.setTelefono(rs.getString("telefono"));
        c.setEmail(rs.getString("email"));
        c.setDireccion(rs.getString("direccion"));
        c.setEstado(rs.getBoolean("estado"));
        Timestamp reg = rs.getTimestamp("fecha_registro");
        c.setFechaRegistro(reg == null ? null : reg.toLocalDateTime());
        return c;
    }

    private String nuloSiVacio(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    /** Para que % y _ escritos por el usuario se busquen como texto normal. */
    private String escaparLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

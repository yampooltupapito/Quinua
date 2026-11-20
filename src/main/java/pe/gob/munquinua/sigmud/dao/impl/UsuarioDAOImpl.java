package pe.gob.munquinua.sigmud.dao.impl;

import java.sql.Connection;
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
import pe.gob.munquinua.sigmud.dao.UsuarioDAO;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.exception.RegistroDuplicadoException;
import pe.gob.munquinua.sigmud.exception.UltimoAdminException;
import pe.gob.munquinua.sigmud.model.Pagina;
import pe.gob.munquinua.sigmud.model.Usuario;
import pe.gob.munquinua.sigmud.util.ConexionBD;
import pe.gob.munquinua.sigmud.util.OrdenSql;
import pe.gob.munquinua.sigmud.util.Paginador;

/**
 * Implementación JDBC de UsuarioDAO (PreparedStatement y try-with-resources). Todo el SQL es texto fijo; los
 * filtros opcionales de listar() agregan fragmentos constantes y los valores van siempre como parámetros.
 * La regla "siempre debe quedar un ADMIN activo" se aplica dentro de una transacción con bloqueo de filas.
 */
public class UsuarioDAOImpl implements UsuarioDAO {

    /** Cantidad de intentos fallidos que bloquea la cuenta. */
    public static final int MAX_INTENTOS = 5;

    /** Código de error de MySQL para "Duplicate entry" (violación de UNIQUE). */
    private static final int MYSQL_DUPLICADO = 1062;

    private static final String SELECT_BASE =
            "SELECT u.id_usuario, u.username, u.password_hash, u.nombres, u.rol, u.id_area, u.estado, "
            + "u.intentos_fallidos, u.ultimo_acceso, u.debe_cambiar_password, a.nombre AS nombre_area "
            + "FROM usuario u LEFT JOIN area a ON a.id_area = u.id_area ";

    // Para el listado paginado: columnas y origen por separado (Paginador arma el SELECT y el COUNT).
    private static final String COLUMNAS_PAGINA =
            "u.id_usuario, u.username, u.password_hash, u.nombres, u.rol, u.id_area, u.estado, "
            + "u.intentos_fallidos, u.ultimo_acceso, u.debe_cambiar_password, a.nombre AS nombre_area";
    private static final String DESDE_PAGINA =
            "FROM usuario u LEFT JOIN area a ON a.id_area = u.id_area WHERE 1 = 1";

    /** Lista blanca de orden: nombre que llega del navegador -> fragmento fijo de SQL. */
    public static final Map<String, String> ORDEN = Map.of("username", "u.username", "nombre", "u.nombres",
            "rol", "u.rol", "area", "a.nombre", "estado", "u.estado", "acceso", "u.ultimo_acceso");

    /** Orden efectivo: si la columna no está en la lista blanca se usa el orden por defecto (nombre, ascendente). */
    public static OrdenSql.Orden resolverOrden(String orden, String dir) {
        return OrdenSql.resolver(ORDEN, orden, dir, "nombre", "asc", "u.id_usuario");
    }

    private static final String SQL_POR_USERNAME = SELECT_BASE + "WHERE u.username = ?";
    private static final String SQL_POR_ID = SELECT_BASE + "WHERE u.id_usuario = ?";
    private static final String SQL_LISTAR = SELECT_BASE + "WHERE 1 = 1";
    private static final String FILTRO_TEXTO = " AND (u.username LIKE ? OR u.nombres LIKE ?)";
    private static final String FILTRO_ROL = " AND u.rol = ?";
    private static final String FILTRO_ESTADO = " AND u.estado = ?";
    private static final String ORDEN_LISTAR = " ORDER BY u.nombres, u.username";

    private static final String SQL_EXISTE_USERNAME =
            "SELECT 1 FROM usuario WHERE username = ? AND id_usuario <> ?";
    private static final String SQL_CONTAR_ADMINS =
            "SELECT COUNT(*) FROM usuario WHERE rol = 'ADMIN' AND estado = 1";
    // FOR UPDATE bloquea esas filas hasta el commit: dos cambios simultáneos no pueden dejar el sistema sin ADMIN.
    private static final String SQL_OTROS_ADMINS =
            "SELECT COUNT(*) FROM usuario WHERE rol = 'ADMIN' AND estado = 1 AND id_usuario <> ? FOR UPDATE";
    private static final String SQL_ROL_ESTADO = "SELECT rol, estado FROM usuario WHERE id_usuario = ? FOR UPDATE";

    private static final String SQL_INSERTAR =
            "INSERT INTO usuario (username, password_hash, nombres, rol, id_area, estado, debe_cambiar_password) "
            + "VALUES (?, ?, ?, ?, ?, 1, 1)";
    private static final String SQL_ACTUALIZAR =
            "UPDATE usuario SET nombres = ?, rol = ?, id_area = ? WHERE id_usuario = ?";
    // Al activar se reinician los intentos (una cuenta bloqueada por intentos no debe quedar a un fallo de bloquearse).
    private static final String SQL_ESTADO =
            "UPDATE usuario SET estado = ?, intentos_fallidos = IF(? = 1, 0, intentos_fallidos) WHERE id_usuario = ?";
    private static final String SQL_PASSWORD =
            "UPDATE usuario SET password_hash = ?, debe_cambiar_password = ? WHERE id_usuario = ?";

    // Solo desbloquea cuentas con 5 o más intentos (las bloqueadas por el sistema); no reactiva las desactivadas a mano.
    private static final String SQL_DESBLOQUEAR =
            "UPDATE usuario SET intentos_fallidos = 0, estado = 1 WHERE id_usuario = ? AND intentos_fallidos >= ?";

    private static final String SQL_FUNCIONARIOS =
            SELECT_BASE + "WHERE u.id_area = ? AND u.rol = 'ATENCION' AND u.estado = 1 ORDER BY u.nombres";

    private static final String SQL_ACCESO_OK =
            "UPDATE usuario SET intentos_fallidos = 0, ultimo_acceso = NOW() WHERE id_usuario = ?";

    // estado se evalúa primero (con el valor anterior de intentos) y luego se suma el intento.
    private static final String SQL_FALLIDO =
            "UPDATE usuario SET estado = IF(intentos_fallidos + 1 >= ?, 0, estado), "
            + "intentos_fallidos = intentos_fallidos + 1 WHERE id_usuario = ?";

    // ---------------------------------------------------------------- consultas

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        return uno(SQL_POR_USERNAME, username, true);
    }

    @Override
    public Optional<Usuario> buscarPorId(int idUsuario) {
        return uno(SQL_POR_ID, idUsuario, false);
    }

    @Override
    public List<Usuario> listar(String texto, String rol, Boolean activo) {
        StringBuilder sql = new StringBuilder(SQL_LISTAR);
        List<Object> parametros = new ArrayList<>();
        if (texto != null && !texto.trim().isEmpty()) {
            sql.append(FILTRO_TEXTO);
            String patron = "%" + escaparLike(texto.trim()) + "%";
            parametros.add(patron);
            parametros.add(patron);
        }
        if (rol != null && !rol.isEmpty()) {
            sql.append(FILTRO_ROL);
            parametros.add(rol);
        }
        if (activo != null) {
            sql.append(FILTRO_ESTADO);
            parametros.add(activo ? 1 : 0);
        }
        sql.append(ORDEN_LISTAR);
        try (Connection cn = ConexionBD.obtener(); PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            return mapearLista(ps);
        } catch (SQLException e) {
            throw new DaoException("No se pudo listar los usuarios", e);
        }
    }

    @Override
    public Pagina<Usuario> listarPagina(String texto, String rol, Boolean activo, int pagina, int tamano,
                                        String orden, String dir) {
        StringBuilder desde = new StringBuilder(DESDE_PAGINA);
        List<Object> filtros = new ArrayList<>();
        if (texto != null && !texto.trim().isEmpty()) {
            desde.append(FILTRO_TEXTO);
            String patron = "%" + escaparLike(texto.trim()) + "%";
            filtros.add(patron);
            filtros.add(patron);
        }
        if (rol != null && !rol.isEmpty()) {
            desde.append(FILTRO_ROL);
            filtros.add(rol);
        }
        if (activo != null) {
            desde.append(FILTRO_ESTADO);
            filtros.add(activo ? 1 : 0);
        }
        return Paginador.ejecutar(COLUMNAS_PAGINA, desde.toString(), filtros, resolverOrden(orden, dir).sql(), pagina,
                tamano, this::mapear);
    }

    @Override
    public boolean existeUsername(String username, int idExcluido) {
        try (Connection cn = ConexionBD.obtener(); PreparedStatement ps = cn.prepareStatement(SQL_EXISTE_USERNAME)) {
            ps.setString(1, username);
            ps.setInt(2, idExcluido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo verificar el nombre de usuario", e);
        }
    }

    @Override
    public int contarAdminsActivos() {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_CONTAR_ADMINS);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new DaoException("No se pudo contar los administradores", e);
        }
    }

    @Override
    public List<Usuario> listarFuncionariosPorArea(int idArea) {
        try (Connection cn = ConexionBD.obtener(); PreparedStatement ps = cn.prepareStatement(SQL_FUNCIONARIOS)) {
            ps.setInt(1, idArea);
            return mapearLista(ps);
        } catch (SQLException e) {
            throw new DaoException("No se pudo listar los funcionarios", e);
        }
    }

    // ---------------------------------------------------------------- escritura

    @Override
    public void insertar(Usuario u, String passwordHash) throws RegistroDuplicadoException {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, passwordHash);
            ps.setString(3, u.getNombres());
            ps.setString(4, u.getRol());
            if (u.getIdArea() == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, u.getIdArea());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    u.setIdUsuario(keys.getInt(1));
                }
            }
            u.setEstado(true);
            u.setDebeCambiarPassword(true);
        } catch (SQLIntegrityConstraintViolationException e) {
            if (e.getErrorCode() == MYSQL_DUPLICADO) {
                throw new RegistroDuplicadoException("Ya existe un usuario con ese nombre de usuario.", e);
            }
            throw new DaoException("No se pudo registrar el usuario", e);
        } catch (SQLException e) {
            throw new DaoException("No se pudo registrar el usuario", e);
        }
    }

    @Override
    public void actualizar(Usuario u) throws UltimoAdminException {
        try (Connection cn = ConexionBD.obtener()) {
            try {
                cn.setAutoCommit(false);
                if (!"ADMIN".equals(u.getRol())) {
                    verificarQueQuedaOtroAdmin(cn, u.getIdUsuario());   // se le quita el rol ADMIN
                }
                try (PreparedStatement ps = cn.prepareStatement(SQL_ACTUALIZAR)) {
                    ps.setString(1, u.getNombres());
                    ps.setString(2, u.getRol());
                    if (u.getIdArea() == null) {
                        ps.setNull(3, Types.INTEGER);
                    } else {
                        ps.setInt(3, u.getIdArea());
                    }
                    ps.setInt(4, u.getIdUsuario());
                    ps.executeUpdate();
                }
                cn.commit();
            } catch (SQLException | RuntimeException | UltimoAdminException e) {
                cn.rollback();
                throw e;
            } finally {
                cn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo actualizar el usuario", e);
        }
    }

    @Override
    public void cambiarEstado(int idUsuario, boolean activo) throws UltimoAdminException {
        try (Connection cn = ConexionBD.obtener()) {
            try {
                cn.setAutoCommit(false);
                if (!activo) {
                    verificarQueQuedaOtroAdmin(cn, idUsuario);          // se desactiva
                }
                try (PreparedStatement ps = cn.prepareStatement(SQL_ESTADO)) {
                    ps.setInt(1, activo ? 1 : 0);
                    ps.setInt(2, activo ? 1 : 0);
                    ps.setInt(3, idUsuario);
                    ps.executeUpdate();
                }
                cn.commit();
            } catch (SQLException | RuntimeException | UltimoAdminException e) {
                cn.rollback();
                throw e;
            } finally {
                cn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo cambiar el estado del usuario", e);
        }
    }

    /**
     * Si el usuario es hoy un ADMIN activo, comprueba que exista OTRO ADMIN activo; si no, lanza
     * UltimoAdminException. Bloquea las filas (FOR UPDATE) hasta que termine la transacción.
     */
    private void verificarQueQuedaOtroAdmin(Connection cn, int idUsuario) throws SQLException, UltimoAdminException {
        boolean esAdminActivo;
        try (PreparedStatement ps = cn.prepareStatement(SQL_ROL_ESTADO)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                esAdminActivo = rs.next() && "ADMIN".equals(rs.getString("rol")) && rs.getBoolean("estado");
            }
        }
        if (!esAdminActivo) {
            return;   // no es un ADMIN activo: no cambia el número de administradores
        }
        try (PreparedStatement ps = cn.prepareStatement(SQL_OTROS_ADMINS)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    throw new UltimoAdminException();
                }
            }
        }
    }

    @Override
    public void actualizarPassword(int idUsuario, String passwordHash, boolean debeCambiar) {
        try (Connection cn = ConexionBD.obtener(); PreparedStatement ps = cn.prepareStatement(SQL_PASSWORD)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, debeCambiar ? 1 : 0);
            ps.setInt(3, idUsuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("No se pudo guardar la contraseña", e);
        }
    }

    @Override
    public boolean desbloquear(int idUsuario) {
        try (Connection cn = ConexionBD.obtener(); PreparedStatement ps = cn.prepareStatement(SQL_DESBLOQUEAR)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, MAX_INTENTOS);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("No se pudo desbloquear la cuenta", e);
        }
    }

    @Override
    public void registrarAccesoExitoso(int idUsuario) {
        try (Connection cn = ConexionBD.obtener(); PreparedStatement ps = cn.prepareStatement(SQL_ACCESO_OK)) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("No se pudo registrar el acceso", e);
        }
    }

    @Override
    public void registrarIntentoFallido(int idUsuario) {
        try (Connection cn = ConexionBD.obtener(); PreparedStatement ps = cn.prepareStatement(SQL_FALLIDO)) {
            ps.setInt(1, MAX_INTENTOS);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("No se pudo registrar el intento fallido", e);
        }
    }

    // ---------------------------------------------------------------- auxiliares

    /** Busca un usuario por un parámetro. Solo la búsqueda por username conserva el hash (para el login). */
    private Optional<Usuario> uno(String sql, Object parametro, boolean conHash) {
        try (Connection cn = ConexionBD.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Usuario u = mapear(rs);
                if (!conHash) {
                    u.setPasswordHash(null);   // el hash no sale del login
                }
                return Optional.of(u);
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo buscar el usuario", e);
        }
    }

    /** Ejecuta la consulta y devuelve los usuarios SIN el hash de la contraseña. */
    private List<Usuario> mapearLista(PreparedStatement ps) throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Usuario u = mapear(rs);
                u.setPasswordHash(null);
                lista.add(u);
            }
        }
        return lista;
    }

    /** Convierte la fila actual del ResultSet en un Usuario. */
    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setNombres(rs.getString("nombres"));
        u.setRol(rs.getString("rol"));
        int area = rs.getInt("id_area");
        u.setIdArea(rs.wasNull() ? null : area);
        u.setEstado(rs.getBoolean("estado"));
        u.setIntentosFallidos(rs.getInt("intentos_fallidos"));
        Timestamp t = rs.getTimestamp("ultimo_acceso");
        u.setUltimoAcceso(t == null ? null : t.toLocalDateTime());
        u.setDebeCambiarPassword(rs.getBoolean("debe_cambiar_password"));
        u.setNombreArea(rs.getString("nombre_area"));
        return u;
    }

    /** Para que % y _ escritos por el usuario se busquen como texto normal. */
    private String escaparLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

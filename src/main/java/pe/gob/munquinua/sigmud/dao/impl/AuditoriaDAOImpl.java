package pe.gob.munquinua.sigmud.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import pe.gob.munquinua.sigmud.dao.AuditoriaDAO;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.model.FiltrosAuditoria;
import pe.gob.munquinua.sigmud.model.RegistroAuditoria;
import pe.gob.munquinua.sigmud.util.ConexionBD;

/**
 * Implementación JDBC de AuditoriaDAO (PreparedStatement y try-with-resources). Todo el SQL es texto fijo: los
 * filtros agregan fragmentos constantes y los valores (incluidos LIMIT y OFFSET) van siempre como parámetros.
 */
public class AuditoriaDAOImpl implements AuditoriaDAO {

    private static final String SQL_INSERTAR =
            "INSERT INTO auditoria (id_usuario, username, accion, entidad, id_entidad, descripcion, detalle, ip) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String COLUMNAS =
            "id_auditoria, fecha_hora, id_usuario, username, accion, entidad, id_entidad, descripcion, detalle, ip";
    private static final String SELECT = "SELECT " + COLUMNAS + " FROM auditoria WHERE 1 = 1";
    private static final String CONTAR = "SELECT COUNT(*) FROM auditoria WHERE 1 = 1";

    // Fragmentos fijos de los filtros opcionales
    private static final String F_DESDE = " AND fecha_hora >= ?";
    private static final String F_HASTA = " AND fecha_hora < ?";
    private static final String F_USUARIO = " AND username LIKE ?";
    private static final String F_ACCION = " AND accion = ?";
    private static final String F_ENTIDAD = " AND entidad = ?";
    private static final String F_ID_ENTIDAD = " AND id_entidad = ?";
    private static final String F_TEXTO = " AND descripcion LIKE ?";
    private static final String ORDEN = " ORDER BY fecha_hora DESC, id_auditoria DESC";
    private static final String PAGINA = " LIMIT ? OFFSET ?";

    @Override
    public void registrar(RegistroAuditoria r) {
        try (Connection cn = ConexionBD.obtener()) {
            registrar(cn, r);
        } catch (SQLException e) {
            throw new DaoException("No se pudo guardar el registro de auditoría", e);
        }
    }

    @Override
    public void registrar(Connection cn, RegistroAuditoria r) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_INSERTAR)) {
            if (r.getIdUsuario() == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, r.getIdUsuario());
            }
            ps.setString(2, r.getUsername());
            ps.setString(3, r.getAccion());
            ps.setString(4, r.getEntidad());
            if (r.getIdEntidad() == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, r.getIdEntidad());
            }
            ps.setString(6, r.getDescripcion());
            ps.setString(7, r.getDetalle());
            ps.setString(8, r.getIp());
            ps.executeUpdate();
        }
    }

    @Override
    public List<RegistroAuditoria> buscar(FiltrosAuditoria f, int pagina, int tamano) {
        List<Object> parametros = new ArrayList<>();
        String sql = SELECT + filtros(f, parametros) + ORDEN + PAGINA;
        parametros.add(tamano);
        parametros.add((long) Math.max(0, pagina - 1) * tamano);
        return consultar(sql, parametros);
    }

    @Override
    public long contar(FiltrosAuditoria f) {
        List<Object> parametros = new ArrayList<>();
        String sql = CONTAR + filtros(f, parametros);
        try (Connection cn = ConexionBD.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            asignar(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo contar los registros de auditoría", e);
        }
    }

    @Override
    public List<RegistroAuditoria> recientes(int n) {
        List<Object> parametros = new ArrayList<>();
        parametros.add(n);
        return consultar(SELECT + ORDEN + " LIMIT ?", parametros);
    }

    @Override
    public List<RegistroAuditoria> ultimosPorEntidad(String entidad, int idEntidad, int n) {
        FiltrosAuditoria f = new FiltrosAuditoria();
        f.setEntidad(entidad);
        f.setIdEntidad(idEntidad);
        return buscar(f, 1, n);
    }

    // ---------------------------------------------------------------- auxiliares

    /** Arma los fragmentos fijos de los filtros que vengan y agrega sus valores a la lista de parámetros. */
    private String filtros(FiltrosAuditoria f, List<Object> parametros) {
        StringBuilder sql = new StringBuilder();
        if (f == null) {
            return "";
        }
        if (f.getDesdeMomento() != null) {
            sql.append(F_DESDE);
            parametros.add(Timestamp.valueOf(f.getDesdeMomento()));
        } else if (f.getDesde() != null) {
            sql.append(F_DESDE);
            parametros.add(Timestamp.valueOf(f.getDesde().atStartOfDay()));
        }
        if (f.getHasta() != null) {
            sql.append(F_HASTA);
            parametros.add(Timestamp.valueOf(f.getHasta().plusDays(1).atStartOfDay()));   // incluye todo el día "hasta"
        }
        if (hay(f.getUsername())) {
            sql.append(F_USUARIO);
            parametros.add("%" + escaparLike(f.getUsername().trim()) + "%");
        }
        if (hay(f.getAccion())) {
            sql.append(F_ACCION);
            parametros.add(f.getAccion().trim());
        }
        if (hay(f.getEntidad())) {
            sql.append(F_ENTIDAD);
            parametros.add(f.getEntidad().trim());
        }
        if (f.getIdEntidad() != null) {
            sql.append(F_ID_ENTIDAD);
            parametros.add(f.getIdEntidad());
        }
        if (hay(f.getTexto())) {
            sql.append(F_TEXTO);
            parametros.add("%" + escaparLike(f.getTexto().trim()) + "%");
        }
        return sql.toString();
    }

    private List<RegistroAuditoria> consultar(String sql, List<Object> parametros) {
        try (Connection cn = ConexionBD.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            asignar(ps, parametros);
            List<RegistroAuditoria> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DaoException("No se pudo consultar la auditoría", e);
        }
    }

    private void asignar(PreparedStatement ps, List<Object> parametros) throws SQLException {
        for (int i = 0; i < parametros.size(); i++) {
            ps.setObject(i + 1, parametros.get(i));
        }
    }

    private RegistroAuditoria mapear(ResultSet rs) throws SQLException {
        RegistroAuditoria r = new RegistroAuditoria();
        r.setIdAuditoria(rs.getLong("id_auditoria"));
        Timestamp t = rs.getTimestamp("fecha_hora");
        r.setFechaHora(t == null ? null : t.toLocalDateTime());
        int usuario = rs.getInt("id_usuario");
        r.setIdUsuario(rs.wasNull() ? null : usuario);
        r.setUsername(rs.getString("username"));
        r.setAccion(rs.getString("accion"));
        r.setEntidad(rs.getString("entidad"));
        int entidad = rs.getInt("id_entidad");
        r.setIdEntidad(rs.wasNull() ? null : entidad);
        r.setDescripcion(rs.getString("descripcion"));
        r.setDetalle(rs.getString("detalle"));
        r.setIp(rs.getString("ip"));
        return r;
    }

    private boolean hay(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /** Para que % y _ escritos por el usuario se busquen como texto normal. */
    private String escaparLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

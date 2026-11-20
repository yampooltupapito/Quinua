package pe.gob.munquinua.sigmud.dao.impl;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import pe.gob.munquinua.sigmud.dao.CitaDAO;
import pe.gob.munquinua.sigmud.exception.CitaCruzadaException;
import pe.gob.munquinua.sigmud.exception.CitaEstadoException;
import pe.gob.munquinua.sigmud.exception.CitaNoEncontradaException;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.model.Cita;
import pe.gob.munquinua.sigmud.model.Pagina;
import pe.gob.munquinua.sigmud.model.ResumenCitas;
import pe.gob.munquinua.sigmud.util.ConexionBD;
import pe.gob.munquinua.sigmud.util.OrdenSql;
import pe.gob.munquinua.sigmud.util.Paginador;

/** Implementación JDBC de CitaDAO: cruce de horarios con doble defensa (consulta previa + UNIQUE de la base). */
public class CitaDAOImpl implements CitaDAO {

    /** Código de error de MySQL para "Duplicate entry" (aquí: uq_cita_funcionario_horario). */
    private static final int MYSQL_DUPLICADO = 1062;

    private static final String COLUMNAS_BASE =
            "c.id_cita, c.id_contribuyente, c.id_area, c.id_funcionario, c.ventanilla, c.fecha, "
            + "c.hora, c.motivo, c.estado, ct.nombres_razon_social AS nombre_contribuyente, ct.dni_ruc, "
            + "a.nombre AS nombre_area, u.nombres AS nombre_funcionario";
    private static final String DESDE_BASE =
            "FROM cita c "
            + "JOIN contribuyente ct ON ct.id_contribuyente = c.id_contribuyente "
            + "JOIN area a ON a.id_area = c.id_area "
            + "JOIN usuario u ON u.id_usuario = c.id_funcionario ";
    private static final String SELECT_BASE = "SELECT " + COLUMNAS_BASE + " " + DESDE_BASE;

    /** Lista blanca de orden: nombre que llega del navegador -> fragmento fijo de SQL. */
    public static final Map<String, String> ORDEN = Map.of("fecha", "c.fecha", "hora", "c.hora", "area", "a.nombre",
            "funcionario", "u.nombres", "motivo", "c.motivo", "estado", "c.estado");

    /** Orden efectivo: si la columna no está en la lista blanca se usa el orden por defecto (fecha, descendente). */
    public static OrdenSql.Orden resolverOrden(String orden, String dir) {
        return OrdenSql.resolver(ORDEN, orden, dir, "fecha", "desc", "c.hora DESC, c.id_cita DESC");
    }
    private static final String SQL_POR_ID = SELECT_BASE + "WHERE c.id_cita = ?";
    private static final String SQL_POR_FECHA = SELECT_BASE + "WHERE c.fecha = ?";
    private static final String FILTRO_AREA = " AND c.id_area = ?";
    private static final String ORDEN_FECHA = " ORDER BY c.hora, c.id_cita";
    private static final String SQL_POR_CONTRIBUYENTE =
            SELECT_BASE + "WHERE c.id_contribuyente = ? ORDER BY c.fecha DESC, c.hora DESC";

    private static final String FILTRO_ESTADO = " AND c.estado = ?";
    private static final String ORDEN_RECIENTES = " ORDER BY c.fecha DESC, c.hora DESC, c.id_cita DESC";
    // Un solo recorrido: COUNT(*) del total y SUM(CASE ...) por estado (SUM da NULL si no hay filas: COALESCE a 0).
    private static final String SQL_RESUMEN =
            "SELECT COUNT(*) AS total, "
            + "COALESCE(SUM(CASE WHEN estado = 'PROGRAMADA' THEN 1 ELSE 0 END), 0) AS programadas, "
            + "COALESCE(SUM(CASE WHEN estado = 'ATENDIDA' THEN 1 ELSE 0 END), 0) AS atendidas, "
            + "COALESCE(SUM(CASE WHEN estado = 'CANCELADA' THEN 1 ELSE 0 END), 0) AS canceladas "
            + "FROM cita WHERE id_contribuyente = ?";
    // Próxima PROGRAMADA desde hoy (si es hoy, solo si la hora aún no pasó): la más cercana.
    private static final String SQL_PROXIMA = SELECT_BASE
            + "WHERE c.id_contribuyente = ? AND c.estado = 'PROGRAMADA' "
            + "AND (c.fecha > ? OR (c.fecha = ? AND c.hora >= ?)) "
            + "ORDER BY c.fecha, c.hora, c.id_cita LIMIT 1";

    private static final String SQL_INSERTAR =
            "INSERT INTO cita (id_contribuyente, id_area, id_funcionario, ventanilla, fecha, hora, motivo, estado) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, 'PROGRAMADA')";
    // "AND estado = 'PROGRAMADA'" evita pisar una cita que otro usuario cambió justo antes.
    private static final String SQL_REPROGRAMAR =
            "UPDATE cita SET fecha = ?, hora = ?, ventanilla = ? WHERE id_cita = ? AND estado = 'PROGRAMADA'";
    private static final String SQL_ESTADO =
            "UPDATE cita SET estado = ? WHERE id_cita = ? AND estado = 'PROGRAMADA'";

    private static final String SQL_CRUCE =
            "SELECT COUNT(*) FROM cita WHERE id_funcionario = ? AND fecha = ? AND hora = ? "
            + "AND estado <> 'CANCELADA' AND id_cita <> ?";
    private static final String SQL_HORAS_OCUPADAS =
            "SELECT hora FROM cita WHERE id_funcionario = ? AND fecha = ? AND estado <> 'CANCELADA' ORDER BY hora";

    // ---------- escritura ----------

    @Override
    public void insertar(Cita c) throws CitaCruzadaException {
        try (Connection cn = ConexionBD.obtener()) {
            if (hayCruce(cn, c.getIdFuncionario(), c.getFecha(), c.getHora(), 0)) {   // defensa 1
                throw new CitaCruzadaException(c.getFecha(), c.getHora());
            }
            try (PreparedStatement ps = cn.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, c.getIdContribuyente());
                ps.setInt(2, c.getIdArea());
                ps.setInt(3, c.getIdFuncionario());
                ps.setInt(4, c.getVentanilla());
                ps.setDate(5, Date.valueOf(c.getFecha()));
                ps.setTime(6, Time.valueOf(c.getHora()));
                ps.setString(7, c.getMotivo());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        c.setIdCita(keys.getInt(1));
                    }
                }
                c.setEstado(Cita.PROGRAMADA);
            }
        } catch (SQLException e) {
            traducirError(e, c.getFecha(), c.getHora(), "No se pudo registrar la cita");  // defensa 2
        }
    }

    @Override
    public void reprogramar(int idCita, LocalDate fecha, LocalTime hora, int ventanilla)
            throws CitaNoEncontradaException, CitaEstadoException, CitaCruzadaException {
        Cita actual = buscarPorId(idCita).orElseThrow(CitaNoEncontradaException::new);
        if (!Cita.PROGRAMADA.equals(actual.getEstado())) {
            throw new CitaEstadoException("No se puede reprogramar una cita " + actual.getEstado() + ".");
        }
        try (Connection cn = ConexionBD.obtener()) {
            if (hayCruce(cn, actual.getIdFuncionario(), fecha, hora, idCita)) {       // defensa 1 (excluye la propia cita)
                throw new CitaCruzadaException(fecha, hora);
            }
            try (PreparedStatement ps = cn.prepareStatement(SQL_REPROGRAMAR)) {
                ps.setDate(1, Date.valueOf(fecha));
                ps.setTime(2, Time.valueOf(hora));
                ps.setInt(3, ventanilla);
                ps.setInt(4, idCita);
                if (ps.executeUpdate() == 0) {
                    throw new CitaEstadoException("La cita cambió de estado y ya no se puede reprogramar.");
                }
            }
        } catch (SQLException e) {
            traducirError(e, fecha, hora, "No se pudo reprogramar la cita");          // defensa 2
        }
    }

    @Override
    public void cambiarEstado(int idCita, String nuevoEstado)
            throws CitaNoEncontradaException, CitaEstadoException {
        if (!Cita.ATENDIDA.equals(nuevoEstado) && !Cita.CANCELADA.equals(nuevoEstado)) {
            throw new CitaEstadoException("El nuevo estado debe ser ATENDIDA o CANCELADA.");
        }
        Cita actual = buscarPorId(idCita).orElseThrow(CitaNoEncontradaException::new);
        if (!Cita.PROGRAMADA.equals(actual.getEstado())) {
            throw new CitaEstadoException("Una cita " + actual.getEstado() + " ya no se puede modificar.");
        }
        if (Cita.ATENDIDA.equals(nuevoEstado) && actual.getFecha().isAfter(LocalDate.now())) {
            throw new CitaEstadoException(
                    "Una cita solo puede marcarse como atendida el día de la cita o después.");
        }
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_ESTADO)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idCita);
            if (ps.executeUpdate() == 0) {
                throw new CitaEstadoException("La cita cambió de estado y ya no se puede modificar.");
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo cambiar el estado de la cita", e);
        }
    }

    // ---------- lectura ----------

    @Override
    public Optional<Cita> buscarPorId(int idCita) {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_POR_ID)) {
            ps.setInt(1, idCita);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo buscar la cita", e);
        }
    }

    @Override
    public List<Cita> listarPorFecha(LocalDate fecha, Integer idArea) {
        String sql = SQL_POR_FECHA + (idArea == null ? "" : FILTRO_AREA) + ORDEN_FECHA;
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fecha));
            if (idArea != null) {
                ps.setInt(2, idArea);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return mapearLista(rs);
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo listar las citas", e);
        }
    }

    @Override
    public List<Cita> listarPorContribuyente(int idContribuyente) {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_POR_CONTRIBUYENTE)) {
            ps.setInt(1, idContribuyente);
            try (ResultSet rs = ps.executeQuery()) {
                return mapearLista(rs);
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo listar las citas del contribuyente", e);
        }
    }

    @Override
    public List<Cita> listarPorContribuyente(int idContribuyente, String estado) {
        boolean filtrar = estado != null && !estado.isBlank();
        String sql = SELECT_BASE + "WHERE c.id_contribuyente = ?" + (filtrar ? FILTRO_ESTADO : "") + ORDEN_RECIENTES;
        try (Connection cn = ConexionBD.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idContribuyente);
            if (filtrar) {
                ps.setString(2, estado.trim());
            }
            try (ResultSet rs = ps.executeQuery()) {
                return mapearLista(rs);
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo listar las citas del contribuyente", e);
        }
    }

    @Override
    public Pagina<Cita> listarPorContribuyentePagina(int idContribuyente, String estado, int pagina, int tamano,
                                                     String orden, String dir) {
        boolean filtrar = estado != null && !estado.isBlank();
        List<Object> filtros = new ArrayList<>();
        filtros.add(idContribuyente);
        String desde = DESDE_BASE + "WHERE c.id_contribuyente = ?";
        if (filtrar) {
            desde += FILTRO_ESTADO;
            filtros.add(estado.trim());
        }
        return Paginador.ejecutar(COLUMNAS_BASE, desde, filtros, resolverOrden(orden, dir).sql(), pagina, tamano,
                this::mapear);
    }

    @Override
    public ResumenCitas resumenPorContribuyente(int idContribuyente) {
        try (Connection cn = ConexionBD.obtener(); PreparedStatement ps = cn.prepareStatement(SQL_RESUMEN)) {
            ps.setInt(1, idContribuyente);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new ResumenCitas(rs.getLong("total"), rs.getLong("programadas"), rs.getLong("atendidas"),
                        rs.getLong("canceladas"));
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo resumir las citas del contribuyente", e);
        }
    }

    @Override
    public Cita proximaCita(int idContribuyente) {
        try (Connection cn = ConexionBD.obtener(); PreparedStatement ps = cn.prepareStatement(SQL_PROXIMA)) {
            LocalDate hoy = LocalDate.now();
            ps.setInt(1, idContribuyente);
            ps.setDate(2, Date.valueOf(hoy));
            ps.setDate(3, Date.valueOf(hoy));
            ps.setTime(4, Time.valueOf(LocalTime.now()));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo buscar la próxima cita del contribuyente", e);
        }
    }

    @Override
    public boolean existeCruce(int idFuncionario, LocalDate fecha, LocalTime hora, int idCitaExcluida) {
        try (Connection cn = ConexionBD.obtener()) {
            return hayCruce(cn, idFuncionario, fecha, hora, idCitaExcluida);
        } catch (SQLException e) {
            throw new DaoException("No se pudo verificar el horario", e);
        }
    }

    @Override
    public List<LocalTime> horasOcupadas(int idFuncionario, LocalDate fecha) {
        List<LocalTime> horas = new ArrayList<>();
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_HORAS_OCUPADAS)) {
            ps.setInt(1, idFuncionario);
            ps.setDate(2, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    horas.add(rs.getTime("hora").toLocalTime());
                }
            }
            return horas;
        } catch (SQLException e) {
            throw new DaoException("No se pudo consultar las horas ocupadas", e);
        }
    }

    // ---------- auxiliares ----------

    /** Consulta de cruce usando la conexión ya abierta. Protegido para poder probar la defensa 2 por separado. */
    protected boolean hayCruce(Connection cn, int idFuncionario, LocalDate fecha, LocalTime hora,
                               int idCitaExcluida) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_CRUCE)) {
            ps.setInt(1, idFuncionario);
            ps.setDate(2, Date.valueOf(fecha));
            ps.setTime(3, Time.valueOf(hora));
            ps.setInt(4, idCitaExcluida);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** UNIQUE violado (error 1062) = otro usuario tomó ese horario a la vez -> CitaCruzadaException. */
    private void traducirError(SQLException e, LocalDate fecha, LocalTime hora, String mensajeGeneral)
            throws CitaCruzadaException {
        if (e instanceof SQLIntegrityConstraintViolationException && e.getErrorCode() == MYSQL_DUPLICADO) {
            throw new CitaCruzadaException(fecha, hora, e);
        }
        throw new DaoException(mensajeGeneral, e);
    }

    private List<Cita> mapearLista(ResultSet rs) throws SQLException {
        List<Cita> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(mapear(rs));
        }
        return lista;
    }

    private Cita mapear(ResultSet rs) throws SQLException {
        Cita c = new Cita();
        c.setIdCita(rs.getInt("id_cita"));
        c.setIdContribuyente(rs.getInt("id_contribuyente"));
        c.setIdArea(rs.getInt("id_area"));
        c.setIdFuncionario(rs.getInt("id_funcionario"));
        c.setVentanilla(rs.getInt("ventanilla"));
        c.setFecha(rs.getDate("fecha").toLocalDate());
        c.setHora(rs.getTime("hora").toLocalTime());
        c.setMotivo(rs.getString("motivo"));
        c.setEstado(rs.getString("estado"));
        c.setNombreContribuyente(rs.getString("nombre_contribuyente"));
        c.setDniRuc(rs.getString("dni_ruc"));
        c.setNombreArea(rs.getString("nombre_area"));
        c.setNombreFuncionario(rs.getString("nombre_funcionario"));
        return c;
    }
}

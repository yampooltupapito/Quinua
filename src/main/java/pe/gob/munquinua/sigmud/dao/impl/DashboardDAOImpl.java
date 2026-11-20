package pe.gob.munquinua.sigmud.dao.impl;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pe.gob.munquinua.sigmud.dao.DashboardDAO;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.model.Cita;
import pe.gob.munquinua.sigmud.model.EtiquetaValor;
import pe.gob.munquinua.sigmud.model.FilaStockCritico;
import pe.gob.munquinua.sigmud.model.MovimientoStock;
import pe.gob.munquinua.sigmud.model.PuntoDiario;
import pe.gob.munquinua.sigmud.model.Usuario;
import pe.gob.munquinua.sigmud.util.ConexionBD;

/**
 * Implementación JDBC de DashboardDAO: consultas de agregación (COUNT, SUM, GROUP BY) con PreparedStatement.
 * Todo el SQL es texto fijo; las fechas y los ids entran siempre como parámetros "?".
 * Los rangos de fechas se expresan como [inicio, fin) para incluir completo el último día.
 */
public class DashboardDAOImpl implements DashboardDAO {

    // ---------- ADMIN ----------

    // Una sola consulta con 6 subconsultas COUNT: un solo viaje a la base de datos.
    private static final String SQL_TOTALES_ADMIN =
            "SELECT (SELECT COUNT(*) FROM usuario WHERE estado = 1) AS usuarios_activos, "
            + "(SELECT COUNT(*) FROM contribuyente WHERE estado = 1) AS contribuyentes_activos, "
            + "(SELECT COUNT(*) FROM material WHERE estado = 1) AS materiales_activos, "
            + "(SELECT COUNT(*) FROM material WHERE estado = 1 AND stock <= stock_minimo) AS materiales_criticos, "
            + "(SELECT COUNT(*) FROM cita WHERE fecha = ?) AS citas_hoy, "
            + "(SELECT COUNT(*) FROM movimiento_stock WHERE fecha >= ? AND fecha < ?) AS movimientos_hoy";

    // GROUP BY estado: una fila por estado con su COUNT.
    private static final String SQL_CITAS_POR_ESTADO =
            "SELECT estado, COUNT(*) AS total FROM cita WHERE fecha BETWEEN ? AND ? GROUP BY estado";

    // SUM con CASE: separa entradas y salidas dentro de un mismo GROUP BY por día.
    private static final String SQL_MOVIMIENTOS_POR_DIA =
            "SELECT DATE(fecha) AS dia, "
            + "COALESCE(SUM(CASE WHEN tipo = 'ENTRADA' THEN cantidad ELSE 0 END), 0) AS entradas, "
            + "COALESCE(SUM(CASE WHEN tipo = 'SALIDA' THEN cantidad ELSE 0 END), 0) AS salidas "
            + "FROM movimiento_stock WHERE fecha >= ? AND fecha < ? GROUP BY DATE(fecha)";

    private static final String SQL_CONTRIBUYENTES_POR_MES =
            "SELECT DATE_FORMAT(fecha_registro, '%Y-%m') AS mes, COUNT(*) AS total "
            + "FROM contribuyente WHERE fecha_registro >= ? GROUP BY DATE_FORMAT(fecha_registro, '%Y-%m')";

    private static final String COLUMNAS_USUARIO =
            "SELECT id_usuario, username, nombres, rol, id_area, estado, intentos_fallidos, ultimo_acceso FROM usuario ";
    private static final String SQL_BLOQUEADAS = COLUMNAS_USUARIO + "WHERE intentos_fallidos >= ? ORDER BY nombres";
    private static final String SQL_ULTIMOS_ACCESOS =
            COLUMNAS_USUARIO + "WHERE ultimo_acceso IS NOT NULL ORDER BY ultimo_acceso DESC LIMIT 5";

    // ---------- ATENCION ----------

    // COUNT(CASE WHEN ... THEN 1 END) cuenta solo las filas que cumplen la condición.
    private static final String SQL_MIS_CITAS_HOY =
            "SELECT COUNT(*) AS total, "
            + "COUNT(CASE WHEN estado = 'PROGRAMADA' THEN 1 END) AS programadas, "
            + "COUNT(CASE WHEN estado = 'ATENDIDA' THEN 1 END) AS atendidas, "
            + "COUNT(CASE WHEN estado = 'CANCELADA' THEN 1 END) AS canceladas "
            + "FROM cita WHERE id_funcionario = ? AND fecha = ?";

    private static final String SQL_MIS_PROXIMAS =
            "SELECT c.id_cita, c.id_contribuyente, c.id_area, c.id_funcionario, c.ventanilla, c.fecha, c.hora, "
            + "c.motivo, c.estado, ct.nombres_razon_social AS nombre_contribuyente, ct.dni_ruc, "
            + "a.nombre AS nombre_area, u.nombres AS nombre_funcionario "
            + "FROM cita c JOIN contribuyente ct ON ct.id_contribuyente = c.id_contribuyente "
            + "JOIN area a ON a.id_area = c.id_area JOIN usuario u ON u.id_usuario = c.id_funcionario "
            + "WHERE c.id_funcionario = ? AND c.fecha = ? AND c.estado = 'PROGRAMADA' AND c.hora >= ? "
            + "ORDER BY c.hora LIMIT 5";

    private static final String SQL_MIS_ATENDIDAS_POR_DIA =
            "SELECT fecha, COUNT(*) AS total FROM cita "
            + "WHERE id_funcionario = ? AND estado = 'ATENDIDA' AND fecha BETWEEN ? AND ? GROUP BY fecha";

    // ---------- ALMACEN ----------

    private static final String SQL_TOTALES_ALMACEN =
            "SELECT (SELECT COUNT(*) FROM material WHERE estado = 1) AS materiales_activos, "
            + "(SELECT COUNT(*) FROM material WHERE estado = 1 AND stock <= stock_minimo) AS materiales_criticos, "
            + "(SELECT COUNT(*) FROM movimiento_stock WHERE fecha >= ? AND fecha < ?) AS movimientos_hoy, "
            + "(SELECT COALESCE(SUM(cantidad), 0) FROM movimiento_stock WHERE tipo = 'ENTRADA' AND fecha >= ? AND fecha < ?) AS entradas_mes, "
            + "(SELECT COALESCE(SUM(cantidad), 0) FROM movimiento_stock WHERE tipo = 'SALIDA' AND fecha >= ? AND fecha < ?) AS salidas_mes";

    private static final String SQL_REPONER =
            "SELECT codigo, nombre, unidad, stock, stock_minimo, (stock_minimo - stock) AS faltante "
            + "FROM material WHERE estado = 1 AND stock <= stock_minimo ORDER BY faltante DESC, nombre LIMIT 5";

    private static final String SQL_SALIDAS_POR_AREA_MES =
            "SELECT a.nombre AS area, SUM(mv.cantidad) AS total "
            + "FROM movimiento_stock mv JOIN area a ON a.id_area = mv.id_area "
            + "WHERE mv.tipo = 'SALIDA' AND mv.fecha >= ? AND mv.fecha < ? "
            + "GROUP BY a.id_area, a.nombre ORDER BY total DESC, a.nombre";

    private static final String SQL_ULTIMOS_MOVIMIENTOS =
            "SELECT mv.id_movimiento, mv.id_material, mv.tipo, mv.cantidad, mv.fecha, mv.id_area, mv.id_usuario, "
            + "mv.observacion, CONCAT(m.codigo, ' - ', m.nombre) AS nombre_material, "
            + "a.nombre AS nombre_area, u.nombres AS nombre_usuario "
            + "FROM movimiento_stock mv JOIN material m ON m.id_material = mv.id_material "
            + "LEFT JOIN area a ON a.id_area = mv.id_area JOIN usuario u ON u.id_usuario = mv.id_usuario "
            + "ORDER BY mv.fecha DESC, mv.id_movimiento DESC LIMIT ?";

    /** Fecha de "hoy" según el reloj de la aplicación. Protegido para poder fijarla en pruebas. */
    protected LocalDate hoy() {
        return LocalDate.now();
    }

    // =============================== ADMIN ===============================

    @Override
    public Map<String, Long> totalesAdmin() {
        LocalDate hoy = hoy();
        Map<String, Long> t = new LinkedHashMap<>();
        consultar(SQL_TOTALES_ADMIN, rs -> {
            rs.next();
            t.put("usuariosActivos", rs.getLong("usuarios_activos"));
            t.put("contribuyentesActivos", rs.getLong("contribuyentes_activos"));
            t.put("materialesActivos", rs.getLong("materiales_activos"));
            t.put("materialesCriticos", rs.getLong("materiales_criticos"));
            t.put("citasHoy", rs.getLong("citas_hoy"));
            t.put("movimientosHoy", rs.getLong("movimientos_hoy"));
        }, Date.valueOf(hoy), inicio(hoy), inicio(hoy.plusDays(1)));
        return t;
    }

    @Override
    public List<EtiquetaValor> citasPorEstado(int dias) {
        LocalDate hoy = hoy();
        Map<String, Long> conteo = new HashMap<>();
        consultar(SQL_CITAS_POR_ESTADO, rs -> {
            while (rs.next()) {
                conteo.put(rs.getString("estado"), rs.getLong("total"));
            }
        }, Date.valueOf(hoy.minusDays(dias - 1L)), Date.valueOf(hoy));
        List<EtiquetaValor> lista = new ArrayList<>();
        for (String estado : List.of(Cita.PROGRAMADA, Cita.ATENDIDA, Cita.CANCELADA)) {
            lista.add(new EtiquetaValor(estado, conteo.getOrDefault(estado, 0L)));
        }
        return lista;
    }

    @Override
    public List<PuntoDiario> movimientosPorDia(int dias) {
        LocalDate hoy = hoy();
        LocalDate desde = hoy.minusDays(dias - 1L);
        Map<LocalDate, long[]> porDia = new HashMap<>();
        consultar(SQL_MOVIMIENTOS_POR_DIA, rs -> {
            while (rs.next()) {
                porDia.put(rs.getDate("dia").toLocalDate(), new long[] {rs.getLong("entradas"), rs.getLong("salidas")});
            }
        }, inicio(desde), inicio(hoy.plusDays(1)));
        List<PuntoDiario> lista = new ArrayList<>();
        for (LocalDate d = desde; !d.isAfter(hoy); d = d.plusDays(1)) {   // los días sin datos quedan en 0
            long[] v = porDia.getOrDefault(d, new long[] {0, 0});
            lista.add(new PuntoDiario(d, v[0], v[1]));
        }
        return lista;
    }

    @Override
    public List<EtiquetaValor> contribuyentesPorMes(int meses) {
        YearMonth actual = YearMonth.from(hoy());
        YearMonth primero = actual.minusMonths(meses - 1L);
        Map<String, Long> conteo = new HashMap<>();
        consultar(SQL_CONTRIBUYENTES_POR_MES, rs -> {
            while (rs.next()) {
                conteo.put(rs.getString("mes"), rs.getLong("total"));
            }
        }, inicio(primero.atDay(1)));
        List<EtiquetaValor> lista = new ArrayList<>();
        for (YearMonth m = primero; !m.isAfter(actual); m = m.plusMonths(1)) {
            lista.add(new EtiquetaValor(m.toString(), conteo.getOrDefault(m.toString(), 0L)));
        }
        return lista;
    }

    @Override
    public List<Usuario> cuentasBloqueadas() {
        return usuarios(SQL_BLOQUEADAS, UsuarioDAOImpl.MAX_INTENTOS);
    }

    @Override
    public List<Usuario> ultimosAccesos() {
        return usuarios(SQL_ULTIMOS_ACCESOS);
    }

    // ============================== ATENCION ==============================

    @Override
    public Map<String, Long> misCitasHoy(int idFuncionario) {
        Map<String, Long> t = new LinkedHashMap<>();
        consultar(SQL_MIS_CITAS_HOY, rs -> {
            rs.next();
            t.put("total", rs.getLong("total"));
            t.put("programadas", rs.getLong("programadas"));
            t.put("atendidas", rs.getLong("atendidas"));
            t.put("canceladas", rs.getLong("canceladas"));
        }, idFuncionario, Date.valueOf(hoy()));
        return t;
    }

    @Override
    public List<Cita> misProximasCitasHoy(int idFuncionario, LocalTime desde) {
        List<Cita> lista = new ArrayList<>();
        consultar(SQL_MIS_PROXIMAS, rs -> {
            while (rs.next()) {
                lista.add(mapearCita(rs));
            }
        }, idFuncionario, Date.valueOf(hoy()), Time.valueOf(desde.withNano(0)));
        return lista;
    }

    @Override
    public List<EtiquetaValor> misCitasAtendidasPorDia(int idFuncionario, int dias) {
        LocalDate hoy = hoy();
        LocalDate desde = hoy.minusDays(dias - 1L);
        Map<LocalDate, Long> conteo = new HashMap<>();
        consultar(SQL_MIS_ATENDIDAS_POR_DIA, rs -> {
            while (rs.next()) {
                conteo.put(rs.getDate("fecha").toLocalDate(), rs.getLong("total"));
            }
        }, idFuncionario, Date.valueOf(desde), Date.valueOf(hoy));
        List<EtiquetaValor> lista = new ArrayList<>();
        for (LocalDate d = desde; !d.isAfter(hoy); d = d.plusDays(1)) {
            lista.add(new EtiquetaValor(d.toString(), conteo.getOrDefault(d, 0L)));
        }
        return lista;
    }

    // ============================== ALMACEN ==============================

    @Override
    public Map<String, Long> totalesAlmacen() {
        LocalDate hoy = hoy();
        LocalDate mes = hoy.withDayOfMonth(1);
        Map<String, Long> t = new LinkedHashMap<>();
        consultar(SQL_TOTALES_ALMACEN, rs -> {
            rs.next();
            t.put("materialesActivos", rs.getLong("materiales_activos"));
            t.put("materialesCriticos", rs.getLong("materiales_criticos"));
            t.put("movimientosHoy", rs.getLong("movimientos_hoy"));
            t.put("entradasMes", rs.getLong("entradas_mes"));
            t.put("salidasMes", rs.getLong("salidas_mes"));
        }, inicio(hoy), inicio(hoy.plusDays(1)), inicio(mes), inicio(mes.plusMonths(1)),
                inicio(mes), inicio(mes.plusMonths(1)));
        return t;
    }

    @Override
    public List<FilaStockCritico> materialesParaReponer() {
        List<FilaStockCritico> lista = new ArrayList<>();
        consultar(SQL_REPONER, rs -> {
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
        });
        return lista;
    }

    @Override
    public List<EtiquetaValor> salidasPorAreaMes() {
        LocalDate mes = hoy().withDayOfMonth(1);
        List<EtiquetaValor> lista = new ArrayList<>();
        consultar(SQL_SALIDAS_POR_AREA_MES, rs -> {
            while (rs.next()) {
                lista.add(new EtiquetaValor(rs.getString("area"), rs.getLong("total")));
            }
        }, inicio(mes), inicio(mes.plusMonths(1)));
        return lista;
    }

    @Override
    public List<MovimientoStock> ultimosMovimientos(int limite) {
        List<MovimientoStock> lista = new ArrayList<>();
        consultar(SQL_ULTIMOS_MOVIMIENTOS, rs -> {
            while (rs.next()) {
                lista.add(mapearMovimiento(rs));
            }
        }, limite);
        return lista;
    }

    // ============================== auxiliares ==============================

    /** Lo que se hace con el ResultSet de una consulta. */
    private interface LectorResultado {
        void leer(ResultSet rs) throws SQLException;
    }

    /** Ejecuta una consulta parametrizada y entrega el ResultSet al lector (todo se cierra con try-with-resources). */
    private void consultar(String sql, LectorResultado lector, Object... parametros) {
        try (Connection cn = ConexionBD.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                lector.leer(rs);
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo consultar los datos del panel", e);
        }
    }

    private Timestamp inicio(LocalDate fecha) {
        return Timestamp.valueOf(fecha.atStartOfDay());
    }

    private List<Usuario> usuarios(String sql, Object... parametros) {
        List<Usuario> lista = new ArrayList<>();
        consultar(sql, rs -> {
            while (rs.next()) {
                Usuario u = new Usuario();
                u.setIdUsuario(rs.getInt("id_usuario"));
                u.setUsername(rs.getString("username"));
                u.setNombres(rs.getString("nombres"));
                u.setRol(rs.getString("rol"));
                u.setEstado(rs.getBoolean("estado"));
                u.setIntentosFallidos(rs.getInt("intentos_fallidos"));
                Timestamp t = rs.getTimestamp("ultimo_acceso");
                u.setUltimoAcceso(t == null ? null : t.toLocalDateTime());
                lista.add(u);            // el hash de la contraseña no se consulta
            }
        }, parametros);
        return lista;
    }

    private Cita mapearCita(ResultSet rs) throws SQLException {
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

    private MovimientoStock mapearMovimiento(ResultSet rs) throws SQLException {
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

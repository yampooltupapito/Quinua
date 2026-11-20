package pe.gob.munquinua.sigmud.dao.impl;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.gob.munquinua.sigmud.dao.ReporteCitasDAO;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.model.FilaCitasResumen;
import pe.gob.munquinua.sigmud.util.ConexionBD;

/** SQL de texto fijo; las fechas entran siempre como parámetros "?". */
public class ReporteCitasDAOImpl implements ReporteCitasDAO {

    // COUNT(CASE WHEN ... THEN 1 END) cuenta solo las filas que cumplen la condición
    // (CASE devuelve NULL en las demás y COUNT ignora los NULL).
    private static final String CONTEOS =
            "COUNT(*) AS total, "
            + "COUNT(CASE WHEN c.estado = 'PROGRAMADA' THEN 1 END) AS programadas, "
            + "COUNT(CASE WHEN c.estado = 'ATENDIDA' THEN 1 END) AS atendidas, "
            + "COUNT(CASE WHEN c.estado = 'CANCELADA' THEN 1 END) AS canceladas ";

    private static final String SQL_POR_AREA =
            "SELECT a.nombre AS nombre, '' AS area, " + CONTEOS
            + "FROM cita c JOIN area a ON a.id_area = c.id_area "
            + "WHERE c.fecha BETWEEN ? AND ? "
            + "GROUP BY a.id_area, a.nombre ORDER BY a.nombre";

    // El área del funcionario sale de su usuario (LEFT JOIN: el área del usuario puede ser nula).
    private static final String SQL_POR_FUNCIONARIO =
            "SELECT u.nombres AS nombre, COALESCE(a.nombre, '') AS area, " + CONTEOS
            + "FROM cita c JOIN usuario u ON u.id_usuario = c.id_funcionario "
            + "LEFT JOIN area a ON a.id_area = u.id_area "
            + "WHERE c.fecha BETWEEN ? AND ? "
            + "GROUP BY u.id_usuario, u.nombres, a.nombre ORDER BY u.nombres";

    @Override
    public List<FilaCitasResumen> citasPorArea(LocalDate desde, LocalDate hasta) {
        return consultar(SQL_POR_AREA, desde, hasta, "No se pudo generar el reporte de citas por área");
    }

    @Override
    public List<FilaCitasResumen> citasPorFuncionario(LocalDate desde, LocalDate hasta) {
        return consultar(SQL_POR_FUNCIONARIO, desde, hasta, "No se pudo generar el reporte de citas por funcionario");
    }

    private List<FilaCitasResumen> consultar(String sql, LocalDate desde, LocalDate hasta, String error) {
        List<FilaCitasResumen> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FilaCitasResumen f = new FilaCitasResumen();
                    f.setNombre(rs.getString("nombre"));
                    f.setArea(rs.getString("area"));
                    f.setTotal(rs.getLong("total"));
                    f.setProgramadas(rs.getLong("programadas"));
                    f.setAtendidas(rs.getLong("atendidas"));
                    f.setCanceladas(rs.getLong("canceladas"));
                    lista.add(f);
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DaoException(error, e);
        }
    }
}

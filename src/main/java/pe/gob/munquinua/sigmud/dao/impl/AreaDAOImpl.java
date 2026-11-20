package pe.gob.munquinua.sigmud.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import pe.gob.munquinua.sigmud.dao.AreaDAO;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.model.Area;
import pe.gob.munquinua.sigmud.util.ConexionBD;

/** Implementación JDBC de AreaDAO (PreparedStatement y try-with-resources). */
public class AreaDAOImpl implements AreaDAO {

    private static final String SQL_ACTIVAS =
            "SELECT id_area, nombre, estado FROM area WHERE estado = 1 ORDER BY nombre";

    @Override
    public List<Area> listarActivas() {
        List<Area> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_ACTIVAS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Area a = new Area();
                a.setIdArea(rs.getInt("id_area"));
                a.setNombre(rs.getString("nombre"));
                a.setEstado(rs.getBoolean("estado"));
                lista.add(a);
            }
            return lista;
        } catch (SQLException e) {
            throw new DaoException("No se pudo listar las áreas", e);
        }
    }
}

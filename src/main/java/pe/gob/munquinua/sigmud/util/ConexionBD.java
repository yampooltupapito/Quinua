package pe.gob.munquinua.sigmud.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Entrega conexiones JDBC leyendo db.properties del classpath. */
public final class ConexionBD {

    private static final Properties PROPS = cargarPropiedades();

    private ConexionBD() { }

    /** Lee db.properties (try-with-resources cierra el archivo) y carga el driver. */
    private static Properties cargarPropiedades() {
        Properties p = new Properties();
        try (InputStream in = ConexionBD.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("No se encontró db.properties en el classpath");
            }
            p.load(in);
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
        return p;
    }

    /**
     * Abre una conexión nueva. Quien la reciba debe cerrarla con try-with-resources:
     * try (Connection cn = ConexionBD.obtener()) { ... }
     */
    public static Connection obtener() throws SQLException {
        return DriverManager.getConnection(
                PROPS.getProperty("url"),
                PROPS.getProperty("usuario"),
                PROPS.getProperty("password"));
    }
}

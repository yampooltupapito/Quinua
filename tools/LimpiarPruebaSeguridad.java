import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Properties;

/**
 * Limpieza de pruebas-seguridad.ps1: borra FISICAMENTE los datos de prueba que el script creo:
 *  - el contribuyente de prueba (mismo DNI y nombre con la etiqueta script), y
 *  - el usuario de prueba (nombre que empieza con "segpru"; la aplicacion nunca borra usuarios).
 * Solo esos registros; nada mas.
 * Uso: java -cp mysql-connector-j.jar tools/LimpiarPruebaSeguridad.java <db.properties> <dni> [usuario]
 */
public class LimpiarPruebaSeguridad {

    public static void main(String[] args) throws Exception {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(args[0])) {
            p.load(in);
        }
        try (Connection cn = DriverManager.getConnection(p.getProperty("url"), p.getProperty("usuario"), p.getProperty("password"))) {
            try (PreparedStatement ps = cn.prepareStatement(
                    "DELETE FROM contribuyente WHERE dni_ruc = ? AND nombres_razon_social LIKE '%<script>%'")) {
                ps.setString(1, args[1]);
                System.out.print("contribuyentes borrados: " + ps.executeUpdate());
            }
            if (args.length > 2) {
                try (PreparedStatement ps = cn.prepareStatement(
                        "DELETE FROM usuario WHERE username = ? AND username LIKE 'segpru%'")) {
                    ps.setString(1, args[2]);
                    System.out.print(", usuarios borrados: " + ps.executeUpdate());
                }
            }
            System.out.println();
        }
    }
}

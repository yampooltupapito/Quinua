package pe.gob.munquinua.sigmud.dao;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeAll;
import pe.gob.munquinua.sigmud.util.ConexionBD;

/**
 * Base de las pruebas de DAO. Usa src/test/resources/db.properties (base mun_quinua_test).
 *
 * SEGURIDAD: antes de correr cualquier prueba comprueba que la conexión sea a "mun_quinua_test".
 * Si por error apuntara a otra base (por ejemplo la real), TODAS las pruebas fallan sin tocar nada.
 * Si la base de pruebas aún no existe, las pruebas se OMITEN (no fallan) con un mensaje que explica
 * cómo crearla (sql/03_bd_pruebas.sql).
 */
abstract class BaseDaoTest {

    static final String BASE_DE_PRUEBAS = "mun_quinua_test";

    @BeforeAll
    static void verificarBaseDePruebas() {
        String catalogo;
        try (Connection cn = ConexionBD.obtener()) {
            catalogo = cn.getCatalog();
        } catch (SQLException e) {
            assumeTrue(false, "Base de pruebas no disponible (" + e.getMessage()
                    + "). Ejecute sql/03_bd_pruebas.sql como root y vuelva a correr las pruebas.");
            return;
        }
        if (!BASE_DE_PRUEBAS.equals(catalogo)) {
            fail("PELIGRO: las pruebas de DAO apuntan a la base '" + catalogo
                    + "' y no a '" + BASE_DE_PRUEBAS + "'. Se cancelan para no tocar datos reales.");
        }
    }

    // ---------- utilidades SQL para preparar y limpiar datos (solo en la base de pruebas) ----------

    static int ejecutar(String sql, Object... parametros) {
        try (Connection cn = ConexionBD.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            asignar(ps, parametros);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Falló el SQL de prueba: " + sql, e);
        }
    }

    static int contar(String sql, Object... parametros) {
        return (int) consultarNumero(sql, parametros);
    }

    static long consultarNumero(String sql, Object... parametros) {
        try (Connection cn = ConexionBD.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            asignar(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falló la consulta de prueba: " + sql, e);
        }
    }

    private static void asignar(PreparedStatement ps, Object[] parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            ps.setObject(i + 1, parametros[i]);
        }
    }
}

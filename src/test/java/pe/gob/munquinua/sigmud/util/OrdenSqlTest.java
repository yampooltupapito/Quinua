package pe.gob.munquinua.sigmud.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.gob.munquinua.sigmud.dao.impl.CitaDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.ContribuyenteDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.MaterialDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.MovimientoDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.UsuarioDAOImpl;

/**
 * Pruebas de la lista blanca de orden (sin base de datos): una columna no permitida usa el orden por defecto, y
 * una con ; -- comillas o palabras SQL también. El texto del usuario NUNCA llega al SQL.
 */
class OrdenSqlTest {

    private static final Map<String, String> LISTA = Map.of("nombre", "nombres_razon_social", "dni", "dni_ruc");

    private OrdenSql.Orden resolver(String columna, String dir) {
        return OrdenSql.resolver(LISTA, columna, dir, "nombre", "asc", "id");
    }

    @Test
    void unaColumnaPermitidaSeTraduceAlFragmentoFijo() {
        OrdenSql.Orden o = resolver("dni", "desc");
        assertEquals("dni", o.columna());
        assertEquals("desc", o.direccion());
        assertEquals(" ORDER BY dni_ruc DESC, id", o.sql());
    }

    @Test
    void sinColumnaSeUsaElOrdenPorDefecto() {
        for (String vacio : new String[] {null, "", "   "}) {
            OrdenSql.Orden o = resolver(vacio, "desc");
            assertEquals("nombre", o.columna());
            assertEquals("asc", o.direccion(), "la dirección pedida no se aplica al orden por defecto");
            assertEquals(" ORDER BY nombres_razon_social ASC, id", o.sql());
        }
    }

    @Test
    void unaColumnaNoPermitidaUsaElOrdenPorDefecto() {
        OrdenSql.Orden o = resolver("password_hash", "desc");
        assertEquals("nombre", o.columna());
        assertEquals(" ORDER BY nombres_razon_social ASC, id", o.sql());
    }

    @Test
    void lasInyeccionesNuncaLlegaranAlSql() {
        String[] ataques = {
            "nombre;DROP TABLE contribuyente", "nombre; DROP TABLE contribuyente", "1 OR 1=1", "1; --",
            "nombre--", "nombre'", "nombre\"", "' OR '1'='1", "nombre /* x */", "nombre,(SELECT 1)",
            "nombres_razon_social", "NOMBRE", " nombre", "nombre ", "dni_ruc", "nombre\nDROP", "%00", "(SELECT 1)"
        };
        for (String ataque : ataques) {
            OrdenSql.Orden o = resolver(ataque, "desc; DROP TABLE x");
            assertEquals("nombre", o.columna(), "no permitido: " + ataque);
            assertEquals(" ORDER BY nombres_razon_social ASC, id", o.sql(), "se usó el orden por defecto con: " + ataque);
            assertFalse(o.sql().contains(";") || o.sql().contains("--") || o.sql().contains("'")
                    || o.sql().toLowerCase().contains("drop"), "el SQL no contiene texto del usuario: " + ataque);
        }
    }

    @Test
    void laDireccionSoloPuedeSerAscODesc() {
        assertEquals("desc", resolver("dni", "DESC").direccion());
        assertEquals("desc", resolver("dni", " desc ").direccion());
        assertEquals("asc", resolver("dni", "asc").direccion());
        assertEquals("asc", resolver("dni", "desc; DROP TABLE x").direccion(), "dirección inválida = ascendente");
        assertEquals("asc", resolver("dni", null).direccion());
        assertEquals(" ORDER BY dni_ruc ASC, id", resolver("dni", "'; --").sql());
    }

    @Test
    void elOrdenPorDefectoDescendentePuedeInvertirseConLaColumnaExplicita() {
        Map<String, String> lista = Map.of("fecha", "mv.fecha", "tipo", "mv.tipo");
        assertEquals(" ORDER BY mv.fecha DESC, mv.id DESC", OrdenSql.resolver(lista, "", "", "fecha", "desc", "mv.id DESC").sql());
        assertEquals(" ORDER BY mv.fecha ASC, mv.id DESC", OrdenSql.resolver(lista, "fecha", "asc", "fecha", "desc", "mv.id DESC").sql());
        assertEquals("desc", OrdenSql.resolver(lista, "fecha", "", "fecha", "desc", "mv.id DESC").direccion(),
                "pidió la columna por defecto sin dirección: se conserva su dirección por defecto");
    }

    // ---------- listas blancas reales de cada DAO ----------

    private void verificar(Map<String, String> orden, OrdenSql.Orden defecto) {
        assertFalse(orden.isEmpty());
        for (Map.Entry<String, String> e : orden.entrySet()) {
            assertTrue(e.getValue().matches("[a-z_.]+"), "fragmento fijo sin símbolos peligrosos: " + e.getValue());
        }
        assertTrue(orden.containsKey(defecto.columna()), "la columna por defecto está en la lista blanca");
        assertTrue(defecto.sql().startsWith(" ORDER BY "));
        assertFalse(defecto.sql().contains(";"));
    }

    @Test
    void lasListasBlancasDeLosDaoSonSeguras() {
        verificar(ContribuyenteDAOImpl.ORDEN, ContribuyenteDAOImpl.resolverOrden("x;DROP", "desc"));
        verificar(MaterialDAOImpl.ORDEN, MaterialDAOImpl.resolverOrden("1 OR 1=1", "desc"));
        verificar(UsuarioDAOImpl.ORDEN, UsuarioDAOImpl.resolverOrden("'--", "desc"));
        verificar(MovimientoDAOImpl.ORDEN, MovimientoDAOImpl.resolverOrden("fecha;--", "asc"));
        verificar(CitaDAOImpl.ORDEN, CitaDAOImpl.resolverOrden("\"", "asc"));
    }

    @Test
    void cadaDaoUsaSuOrdenPorDefecto() {
        assertEquals(" ORDER BY nombres_razon_social ASC, id_contribuyente", ContribuyenteDAOImpl.resolverOrden("nombre;DROP TABLE contribuyente", "desc").sql());
        assertEquals(" ORDER BY nombre ASC, id_material", MaterialDAOImpl.resolverOrden("1 OR 1=1", "desc").sql());
        assertEquals(" ORDER BY u.nombres ASC, u.id_usuario", UsuarioDAOImpl.resolverOrden("x", "desc").sql());
        assertEquals(" ORDER BY mv.fecha DESC, mv.id_movimiento DESC", MovimientoDAOImpl.resolverOrden("x", "asc").sql());
        assertEquals(" ORDER BY c.fecha DESC, c.hora DESC, c.id_cita DESC", CitaDAOImpl.resolverOrden("x", "asc").sql());
    }
}

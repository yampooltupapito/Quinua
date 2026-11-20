package pe.gob.munquinua.sigmud.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import pe.gob.munquinua.sigmud.model.RegistroAuditoria;

/** Pruebas sin base de datos: limpieza de texto, longitudes y comparación de cambios (diferencias). */
class AuditoriaTest {

    /** Objeto de ejemplo con campos normales y campos sensibles. */
    static class Ejemplo {
        String nombre;
        String telefono;
        Integer stock;
        String passwordHash;
        String clave;
        String tokenSecreto;

        Ejemplo(String nombre, String telefono, Integer stock) {
            this.nombre = nombre;
            this.telefono = telefono;
            this.stock = stock;
            this.passwordHash = "hash-" + nombre;
            this.clave = "clave-" + nombre;
            this.tokenSecreto = "tok-" + nombre;
        }
    }

    // ---------- limpieza ----------

    @Test
    void quitaSaltosDeLineaYCaracteresDeControl() {
        String limpio = Auditoria.limpiar("linea1\r\nlinea2\tlinea3\u0000fin", 100);
        assertFalse(limpio.contains("\n"));
        assertFalse(limpio.contains("\r"));
        assertFalse(limpio.contains("\t"));
        assertFalse(limpio.contains("\u0000"));
        assertEquals("linea1 linea2 linea3 fin", limpio);
    }

    @Test
    void quitaSeparadoresUnicodeDeLinea() {
        assertEquals("a b c", Auditoria.limpiar("a b c", 50));
    }

    @Test
    void recortaALaLongitudMaxima() {
        assertEquals(10, Auditoria.limpiar("x".repeat(500), 10).length());
    }

    @Test
    void unTextoNuloSeConvierteEnVacio() {
        assertEquals("", Auditoria.limpiar(null, 10));
    }

    @Test
    void noAlteraElTextoHtmlPorqueElEscapadoSeHaceAlMostrar() {
        assertEquals("<script>alert(1)</script>", Auditoria.limpiar("<script>alert(1)</script>", 100));
    }

    @Test
    void construirAplicaLasLongitudesDeCadaColumna() {
        RegistroAuditoria r = Auditoria.construir(1, "u".repeat(200), "A".repeat(200), "E".repeat(200), 5,
                "d".repeat(1000), "x".repeat(9000), "1".repeat(200));
        assertEquals(Auditoria.MAX_USERNAME, r.getUsername().length());
        assertEquals(Auditoria.MAX_ACCION, r.getAccion().length());
        assertEquals(Auditoria.MAX_ENTIDAD, r.getEntidad().length());
        assertEquals(Auditoria.MAX_DESCRIPCION, r.getDescripcion().length());
        assertEquals(Auditoria.MAX_DETALLE, r.getDetalle().length());
        assertEquals(Auditoria.MAX_IP, r.getIp().length());
    }

    @Test
    void construirSaneaUnUsuarioConSaltosDeLinea() {
        RegistroAuditoria r = Auditoria.construir(null, "admin\r\n2026-01-01 LOGIN_OK falso", Auditoria.LOGIN_FALLIDO,
                Auditoria.SESION, null, "desc\nlinea", null, "127.0.0.1");
        assertFalse(r.getUsername().contains("\n") || r.getUsername().contains("\r"));
        assertFalse(r.getDescripcion().contains("\n"));
    }

    @Test
    void unDetalleVacioQuedaNulo() {
        assertNull(Auditoria.construir(null, "a", "X", "Y", null, "d", "  ", "ip").getDetalle());
        assertNull(Auditoria.construir(null, "a", "X", "Y", null, "d", null, "ip").getDetalle());
    }

    // ---------- diferencias ----------

    @Test
    void listaSoloLoQueCambio() {
        String d = Auditoria.diferencias(new Ejemplo("Ana", "111", 5), new Ejemplo("Ana", "222", 5));
        assertEquals("telefono: 111 -> 222", d);
    }

    @Test
    void listaVariosCambiosSeparadosPorPuntoYComa() {
        String d = Auditoria.diferencias(new Ejemplo("Ana", "111", 5), new Ejemplo("Ana", "222", 9));
        assertTrue(d.contains("telefono: 111 -> 222"));
        assertTrue(d.contains("stock: 5 -> 9"));
        assertTrue(d.contains("; "));
    }

    @Test
    void sinCambiosDevuelveVacio() {
        assertEquals("", Auditoria.diferencias(new Ejemplo("Ana", "111", 5), new Ejemplo("Ana", "111", 5)));
    }

    @Test
    void nuloYVacioSeConsideranIguales() {
        assertEquals("", Auditoria.diferencias(new Ejemplo("Ana", null, 5), new Ejemplo("Ana", "  ", 5)));
    }

    @Test
    void muestraVacioCuandoUnValorEsNulo() {
        assertEquals("telefono: 111 -> (vacío)",
                Auditoria.diferencias(new Ejemplo("Ana", "111", 5), new Ejemplo("Ana", null, 5)));
    }

    @Test
    void nuncaIncluyeCamposSensibles() {
        // Cambia el nombre, y con él también passwordHash, clave y tokenSecreto
        String d = Auditoria.diferencias(new Ejemplo("Ana", "111", 5), new Ejemplo("Beto", "111", 5));
        assertEquals("nombre: Ana -> Beto", d);
        assertFalse(d.toLowerCase().contains("hash"));
        assertFalse(d.toLowerCase().contains("clave"));
        assertFalse(d.toLowerCase().contains("tok-"));
    }

    @Test
    void recortaLosValoresMuyLargos() {
        String d = Auditoria.diferencias(new Ejemplo("a".repeat(500), "1", 1), new Ejemplo("b".repeat(500), "1", 1));
        assertTrue(d.length() < 250, "cada valor se recorta a unos 80 caracteres");
    }

    @Test
    void objetosNulosODeDistintoTipoDevuelvenVacio() {
        assertEquals("", Auditoria.diferencias(null, new Ejemplo("a", "1", 1)));
        assertEquals("", Auditoria.diferencias(new Ejemplo("a", "1", 1), "otro tipo"));
    }

    @Test
    void elDetalleNoTieneSaltosDeLineaAunqueLosValoresLosTengan() {
        String d = Auditoria.diferencias(new Ejemplo("a\nb", "1", 1), new Ejemplo("c\r\nd", "1", 1));
        assertFalse(d.contains("\n") || d.contains("\r"));
    }
}

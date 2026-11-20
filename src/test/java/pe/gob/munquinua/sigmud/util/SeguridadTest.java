package pe.gob.munquinua.sigmud.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Pruebas de Seguridad: PBKDF2, hashes antiguos, contraseñas temporales y token aleatorio (sin base de datos). */
class SeguridadTest {

    // SHA-256 sin sal de "Admin123": el hash antiguo que todavía tienen guardado los usuarios de ejemplo
    private static final String LEGADO_ADMIN123 = "3b612c75a7b5048a435fb6ec81e52ff92d6d795a8b5a9c17070f6a63c97a53b2";

    // ---------- hash nuevo (PBKDF2) ----------

    @Test
    void elHashNuevoTieneElFormatoPbkdf2ConIteracionesSalYHash() {
        String h = Seguridad.hashear("Admin123");
        String[] partes = h.split("\\$");
        assertEquals(4, partes.length);
        assertEquals("pbkdf2", partes[0]);
        assertEquals("120000", partes[1]);
        assertEquals(16, Base64.getDecoder().decode(partes[2]).length, "sal de 16 bytes");
        assertEquals(32, Base64.getDecoder().decode(partes[3]).length, "hash de 256 bits");
        assertTrue(h.length() < 255, "cabe en la columna VARCHAR(255)");
    }

    @Test
    void elHashNuevoVerificaConLaContrasenaCorrecta() {
        assertTrue(Seguridad.verificar("Admin123", Seguridad.hashear("Admin123")));
        assertTrue(Seguridad.verificar("Contraseña con ñ y tildes áé", Seguridad.hashear("Contraseña con ñ y tildes áé")));
    }

    @Test
    void dosHashesDeLaMismaContrasenaSonDistintosPeroAmbosVerifican() {
        String a = Seguridad.hashear("Admin123");
        String b = Seguridad.hashear("Admin123");
        assertNotEquals(a, b, "la sal aleatoria cambia el resultado");
        assertTrue(Seguridad.verificar("Admin123", a));
        assertTrue(Seguridad.verificar("Admin123", b));
    }

    @Test
    void unaContrasenaIncorrectaFalla() {
        String h = Seguridad.hashear("Admin123");
        assertFalse(Seguridad.verificar("admin123", h));
        assertFalse(Seguridad.verificar("Admin1234", h));
        assertFalse(Seguridad.verificar("", h));
    }

    @Test
    void elHashNoEsElSha256DeLaContrasena() {
        assertFalse(Seguridad.hashear("Admin123").contains(Seguridad.sha256("Admin123")));
    }

    // ---------- hashes antiguos (SHA-256 sin sal) ----------

    @Test
    void unHashAntiguoSha256Verifica() {
        assertTrue(Seguridad.verificar("Admin123", LEGADO_ADMIN123));
        assertTrue(Seguridad.verificar("Admin123", LEGADO_ADMIN123.toUpperCase()), "mayúsculas o minúsculas");
        assertFalse(Seguridad.verificar("admin123", LEGADO_ADMIN123));
    }

    @Test
    void esLegadoReconoceSoloLos64Hexadecimales() {
        assertTrue(Seguridad.esLegado(LEGADO_ADMIN123));
        assertTrue(Seguridad.esLegado(Seguridad.sha256("otra")));
        assertFalse(Seguridad.esLegado(Seguridad.hashear("Admin123")));
        assertFalse(Seguridad.esLegado(LEGADO_ADMIN123 + "0"));
        assertFalse(Seguridad.esLegado(LEGADO_ADMIN123.substring(1)));
        assertFalse(Seguridad.esLegado("z".repeat(64)));
        assertFalse(Seguridad.esLegado(null));
        assertFalse(Seguridad.esLegado(""));
    }

    // ---------- formatos inválidos: devuelven false, nunca lanzan excepción ----------

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "no-es-un-hash", "pbkdf2", "pbkdf2$120000", "pbkdf2$120000$AAAA", "pbkdf2$abc$AAAA$BBBB",
        "pbkdf2$120000$%%%$@@@", "pbkdf2$120000$$", "otro$120000$AAAAAAAAAAAAAAAAAAAAAA==$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "pbkdf2$0$AAAAAAAAAAAAAAAAAAAAAA==$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "pbkdf2$-5$AAAAAAAAAAAAAAAAAAAAAA==$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "pbkdf2$99999999999$AAAAAAAAAAAAAAAAAAAAAA==$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "pbkdf2$120000$AAAAAAAAAAAAAAAAAAAAAA==$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=$extra"})
    void unFormatoInvalidoNoLanzaExcepcionYNoVerifica(String almacenado) {
        assertDoesNotThrow(() -> Seguridad.verificar("Admin123", almacenado));
        assertFalse(Seguridad.verificar("Admin123", almacenado));
    }

    @Test
    void unaContrasenaNulaNoVerificaNiLanzaExcepcion() {
        assertDoesNotThrow(() -> Seguridad.verificar(null, Seguridad.hashear("x")));
        assertFalse(Seguridad.verificar(null, Seguridad.hashear("x")));
    }

    @Test
    void unHashAlteradoNoVerifica() {
        String h = Seguridad.hashear("Admin123");
        String[] p = h.split("\\$");
        String otraSal = p[0] + "$" + p[1] + "$" + Base64.getEncoder().encodeToString(new byte[16]) + "$" + p[3];
        assertFalse(Seguridad.verificar("Admin123", otraSal));
        String otraIteracion = p[0] + "$" + 120001 + "$" + p[2] + "$" + p[3];
        assertFalse(Seguridad.verificar("Admin123", otraIteracion));
    }

    // ---------- contraseña temporal ----------

    @Test
    void laContrasenaTemporalTiene12CaracteresYCumpleLaPolitica() {
        for (int i = 0; i < 300; i++) {
            String t = Seguridad.generarPasswordTemporal();
            assertEquals(12, t.length());
            assertEquals(java.util.List.of(), Validador.politicaPassword(t, "usuario"), "la temporal " + t);
        }
    }

    @Test
    void lasContrasenasTemporalesSonDistintasEntreSi() {
        Set<String> vistas = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            vistas.add(Seguridad.generarPasswordTemporal());
        }
        assertEquals(200, vistas.size());
    }

    @Test
    void laContrasenaTemporalNoUsaCaracteresQueSeConfunden() {
        for (int i = 0; i < 200; i++) {
            assertFalse(Seguridad.generarPasswordTemporal().matches(".*[0O1lI].*"));
        }
    }

    // ---------- SHA-256 y token ----------

    @Test
    void sha256DeUnaCadenaConocidaEsElEsperado() {
        assertEquals(LEGADO_ADMIN123, Seguridad.sha256("Admin123"));
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Seguridad.sha256(""));
    }

    @Test
    void tokenAleatorioTiene43CaracteresBase64UrlSinRelleno() {
        assertTrue(Seguridad.tokenAleatorio().matches("^[A-Za-z0-9_-]{43}$"));
    }

    @Test
    void dosTokensAleatoriosSonDistintos() {
        assertNotEquals(Seguridad.tokenAleatorio(), Seguridad.tokenAleatorio());
    }
}

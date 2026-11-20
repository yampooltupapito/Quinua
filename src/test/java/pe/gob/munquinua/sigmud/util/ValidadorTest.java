package pe.gob.munquinua.sigmud.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Pruebas de Validador (expresiones regulares), sin base de datos. */
class ValidadorTest {

    // ---------- usuario ----------
    @ParameterizedTest
    @ValueSource(strings = {"admin", "atencion1", "juan.perez", "a_b-c9", "ABC", "abc"})
    void usuariosValidos(String s) {
        assertTrue(Validador.usuarioValido(s));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "ab", "a", "ab cd", "juan@perez", "' OR '1'='1", "admin;--", "<script>", "ñandu1"})
    void usuariosInvalidos(String s) {
        assertFalse(Validador.usuarioValido(s));
    }

    @Test
    void usuarioDe30CaracteresEsValidoYDe31No() {
        assertTrue(Validador.usuarioValido("a".repeat(30)));
        assertFalse(Validador.usuarioValido("a".repeat(31)));
    }

    // ---------- DNI ----------
    @ParameterizedTest
    @ValueSource(strings = {"12345678", "00000001", " 12345678 "})
    void dniValidos(String s) {
        assertTrue(Validador.dniValido(s));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "1234567", "123456789", "1234567a", "12 345678", "-1234567"})
    void dniInvalidos(String s) {
        assertFalse(Validador.dniValido(s));
    }

    // ---------- RUC ----------
    @ParameterizedTest
    @ValueSource(strings = {"20123456781", "10123456789"})
    void rucValidos(String s) {
        assertTrue(Validador.rucValido(s));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "30123456789", "15123456789", "2012345678", "201234567812", "2012345678a"})
    void rucInvalidos(String s) {
        assertFalse(Validador.rucValido(s));
    }

    // ---------- teléfono ----------
    @ParameterizedTest
    @ValueSource(strings = {"966123456", "900000000"})
    void telefonosValidos(String s) {
        assertTrue(Validador.telefonoValido(s));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "96612345", "8661234567", "866123456", "96612345a", "+51966123456"})
    void telefonosInvalidos(String s) {
        assertFalse(Validador.telefonoValido(s));
    }

    // ---------- email ----------
    @ParameterizedTest
    @ValueSource(strings = {"ana@example.com", "ana.torres+prueba@muni-quinua.gob.pe", "a_b@x.org"})
    void emailsValidos(String s) {
        assertTrue(Validador.emailValido(s));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "ana.example.com", "ana@", "@example.com", "ana@example", "ana @example.com", "ana@example.c"})
    void emailsInvalidos(String s) {
        assertFalse(Validador.emailValido(s));
    }

    // ---------- vacío ----------
    @Test
    void vacioDetectaNuloYEspacios() {
        assertTrue(Validador.vacio(null));
        assertTrue(Validador.vacio(""));
        assertTrue(Validador.vacio("   "));
        assertFalse(Validador.vacio(" a "));
    }
}

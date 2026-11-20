package pe.gob.munquinua.sigmud.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Pruebas de la política de contraseña (Validador.politicaPassword), sin base de datos. */
class PoliticaPasswordTest {

    private boolean hayError(List<String> errores, String fragmento) {
        return errores.stream().anyMatch(e -> e.contains(fragmento));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Admin123", "Abcdefg1", "Contraseña9", "xY7xxxxxxxxxxxxxxxxx", "Aa1aaaaa", "ÁrbolVerde5"})
    void contrasenasQueCumplenLaPolitica(String p) {
        assertEquals(List.of(), Validador.politicaPassword(p, "juan"));
    }

    @Test
    void menosDe8CaracteresEsInvalida() {
        assertTrue(hayError(Validador.politicaPassword("Ab1cdef", "juan"), "al menos 8 caracteres"));
        assertEquals(List.of(), Validador.politicaPassword("Ab1cdefg", "juan"), "8 justos es válida");
    }

    @Test
    void sinMayusculaEsInvalida() {
        List<String> e = Validador.politicaPassword("abcdefg1", "juan");
        assertEquals(1, e.size());
        assertTrue(hayError(e, "mayúscula"));
    }

    @Test
    void sinMinusculaEsInvalida() {
        List<String> e = Validador.politicaPassword("ABCDEFG1", "juan");
        assertEquals(1, e.size());
        assertTrue(hayError(e, "minúscula"));
    }

    @Test
    void sinNumeroEsInvalida() {
        List<String> e = Validador.politicaPassword("Abcdefgh", "juan");
        assertEquals(1, e.size());
        assertTrue(hayError(e, "número"));
    }

    @Test
    void noPuedeSerIgualAlNombreDeUsuarioSinImportarMayusculas() {
        assertTrue(hayError(Validador.politicaPassword("Juan1234", "juan1234"), "igual al nombre de usuario"));
        assertTrue(hayError(Validador.politicaPassword("JUAN1234", "Juan1234"), "igual al nombre de usuario"));
        assertEquals(List.of(), Validador.politicaPassword("Juan12345", "juan1234"), "parecida pero distinta es válida");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void nulaOVaciaIncumpleTodo(String p) {
        List<String> e = Validador.politicaPassword(p, "juan");
        assertTrue(hayError(e, "8 caracteres"));
        assertTrue(hayError(e, "mayúscula"));
        assertTrue(hayError(e, "minúscula"));
        assertTrue(hayError(e, "número"));
    }

    @Test
    void masDe100CaracteresEsInvalida() {
        assertTrue(hayError(Validador.politicaPassword("Aa1" + "x".repeat(98), "juan"), "100 caracteres"));
        assertEquals(List.of(), Validador.politicaPassword("Aa1" + "x".repeat(97), "juan"));
    }

    @Test
    void variosErroresSeAcumulanConMensajesEnEspanol() {
        List<String> e = Validador.politicaPassword("abc", "juan");
        assertEquals(3, e.size(), "corta, sin mayúscula y sin número");
    }

    @Test
    void elNombreDeUsuarioNuloNoRompe() {
        assertEquals(List.of(), Validador.politicaPassword("Admin123", null));
    }
}

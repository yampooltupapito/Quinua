package pe.gob.munquinua.sigmud.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.gob.munquinua.sigmud.model.Material;

/** Pruebas de MaterialValidador (sin base de datos). */
class MaterialValidadorTest {

    private Material valido() {
        Material m = new Material();
        m.setCodigo("MAT-006");
        m.setNombre("Clavos de 2 pulgadas");
        m.setUnidad("CAJA");
        m.setStockMinimo(5);
        return m;
    }

    private boolean hayError(List<String> errores, String fragmento) {
        return errores.stream().anyMatch(e -> e.contains(fragmento));
    }

    @Test
    void materialValidoNoTieneErrores() {
        assertEquals(List.of(), MaterialValidador.validar(valido()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MAT-001", "ABC-999", "XYZ-000"})
    void codigosValidos(String codigo) {
        Material m = valido();
        m.setCodigo(codigo);
        assertEquals(List.of(), MaterialValidador.validar(m));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "mat1", "mat-006", "MAT-6", "MAT-0066", "MA-006", "MATE-006", "MAT006", "MAT 006", "123-456"})
    void codigosInvalidos(String codigo) {
        Material m = valido();
        m.setCodigo(codigo);
        assertTrue(hayError(MaterialValidador.validar(m), "código"));
    }

    @Test
    void nombreDeMenosDe3OVacioEsInvalido() {
        Material m = valido();
        m.setNombre("ab");
        assertTrue(hayError(MaterialValidador.validar(m), "nombre"));
        m.setNombre("   ");
        assertTrue(hayError(MaterialValidador.validar(m), "nombre"));
        m.setNombre(null);
        assertTrue(hayError(MaterialValidador.validar(m), "nombre"));
    }

    @Test
    void nombreLimitesDe3Y100Caracteres() {
        Material m = valido();
        m.setNombre("abc");
        assertEquals(List.of(), MaterialValidador.validar(m));
        m.setNombre("a".repeat(100));
        assertEquals(List.of(), MaterialValidador.validar(m));
        m.setNombre("a".repeat(101));
        assertTrue(hayError(MaterialValidador.validar(m), "nombre"));
    }

    @Test
    void todasLasUnidadesDeLaListaSonValidas() {
        for (String unidad : List.of("UNIDAD", "PAQUETE", "BOLSA", "CAJA", "KILO", "LITRO", "METRO", "GALON")) {
            Material m = valido();
            m.setUnidad(unidad);
            assertEquals(List.of(), MaterialValidador.validar(m), unidad);
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "DOCENA", "unidad", "Caja", "CAJA "})
    void unidadesNoPermitidas(String unidad) {
        Material m = valido();
        m.setUnidad(unidad);
        assertTrue(hayError(MaterialValidador.validar(m), "unidad"));
    }

    @Test
    void stockMinimoNegativoEsInvalido() {
        Material m = valido();
        m.setStockMinimo(-1);
        assertTrue(hayError(MaterialValidador.validar(m), "stock mínimo"));
    }

    @Test
    void stockMinimoCeroEsValido() {
        Material m = valido();
        m.setStockMinimo(0);
        assertEquals(List.of(), MaterialValidador.validar(m));
    }

    @Test
    void materialNuloNoLanzaExcepcion() {
        assertEquals(1, MaterialValidador.validar(null).size());
    }

    @Test
    void variosErroresSeAcumulan() {
        Material m = new Material();
        m.setCodigo("x");
        m.setNombre("");
        m.setUnidad("DOCENA");
        m.setStockMinimo(-5);
        assertEquals(4, MaterialValidador.validar(m).size());
    }
}

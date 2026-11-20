package pe.gob.munquinua.sigmud.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pruebas de Csv: escape de celdas y protección contra fórmulas de Excel (sin base de datos). */
class CsvTest {

    @Test
    void textoSimpleNoCambia() {
        assertEquals("Mesa de Partes", Csv.celda("Mesa de Partes", false));
    }

    @Test
    void puntoYComaYComillasVanEntreComillas() {
        assertEquals("\"a;b\"", Csv.celda("a;b", false));
        assertEquals("\"dijo \"\"hola\"\"\"", Csv.celda("dijo \"hola\"", false));
    }

    @Test
    void textosQueEmpiezanComoFormulaLlevanApostrofo() {
        assertEquals("'=SUMA(A1)", Csv.celda("=SUMA(A1)", false));
        assertEquals("'+1", Csv.celda("+1", false));
        assertEquals("'-1", Csv.celda("-1", false));
        assertEquals("'@usuario", Csv.celda("@usuario", false));
    }

    @Test
    void losNumerosNegativosNoLlevanApostrofo() {
        assertEquals("-5", Csv.celda("-5", true));
    }

    @Test
    void nuloSeEscribeVacio() {
        assertEquals("", Csv.celda(null, false));
    }

    @Test
    void laLineaUsaPuntoYComaYTerminaConSaltoDeLinea() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Csv.escribirLinea(pw, List.of("Área", "10", "x;y"), List.of(false, true, false));
        pw.flush();
        assertEquals("Área;10;\"x;y\"\r\n", sw.toString());
    }
}

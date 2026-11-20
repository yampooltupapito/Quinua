package pe.gob.munquinua.sigmud.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.gob.munquinua.sigmud.model.Contribuyente;

/** Pruebas de ContribuyenteValidador (sin base de datos). */
class ContribuyenteValidadorTest {

    private Contribuyente natural() {
        Contribuyente c = new Contribuyente();
        c.setTipoPersona(Contribuyente.NATURAL);
        c.setDniRuc("45678912");
        c.setNombresRazonSocial("Ana Torres Rojas");
        c.setFechaNacimiento(LocalDate.of(1990, 5, 20));
        c.setTelefono("966123456");
        c.setEmail("ana@example.com");
        c.setDireccion("Jr. Principal 123");
        return c;
    }

    private Contribuyente juridica() {
        Contribuyente c = new Contribuyente();
        c.setTipoPersona(Contribuyente.JURIDICA);
        c.setDniRuc("20123456781");
        c.setNombresRazonSocial("Comercial Andina SAC");
        return c;
    }

    private boolean hayError(List<String> errores, String fragmento) {
        return errores.stream().anyMatch(e -> e.contains(fragmento));
    }

    @Test
    void personaNaturalValidaNoTieneErrores() {
        assertEquals(List.of(), ContribuyenteValidador.validar(natural()));
    }

    @Test
    void personaJuridicaValidaNoTieneErrores() {
        assertEquals(List.of(), ContribuyenteValidador.validar(juridica()));
    }

    @Test
    void camposOpcionalesPuedenIrVaciosONulos() {
        Contribuyente c = natural();
        c.setFechaNacimiento(null);
        c.setTelefono("");
        c.setEmail(null);
        c.setDireccion(null);
        assertEquals(List.of(), ContribuyenteValidador.validar(c));
    }

    @Test
    void dniDe7DigitosEsInvalido() {
        Contribuyente c = natural();
        c.setDniRuc("1234567");
        assertTrue(hayError(ContribuyenteValidador.validar(c), "DNI"));
    }

    @Test
    void dniConLetrasEsInvalido() {
        Contribuyente c = natural();
        c.setDniRuc("1234567a");
        assertTrue(hayError(ContribuyenteValidador.validar(c), "DNI"));
    }

    @Test
    void rucQueEmpiezaCon30EsInvalido() {
        Contribuyente c = juridica();
        c.setDniRuc("30123456789");
        assertTrue(hayError(ContribuyenteValidador.validar(c), "RUC"));
    }

    @Test
    void rucQueEmpiezaCon10SiEsValido() {
        Contribuyente c = juridica();
        c.setDniRuc("10123456789");
        assertEquals(List.of(), ContribuyenteValidador.validar(c));
    }

    @Test
    void rucDe10DigitosEsInvalido() {
        Contribuyente c = juridica();
        c.setDniRuc("2012345678");
        assertTrue(hayError(ContribuyenteValidador.validar(c), "RUC"));
    }

    @Test
    void tipoDePersonaObligatorio() {
        Contribuyente c = natural();
        c.setTipoPersona(null);
        assertTrue(hayError(ContribuyenteValidador.validar(c), "tipo de persona"));
        c.setTipoPersona("OTRO");
        assertTrue(hayError(ContribuyenteValidador.validar(c), "tipo de persona"));
    }

    @Test
    void telefonoDe8DigitosOQueNoEmpiezaCon9EsInvalido() {
        Contribuyente c = natural();
        c.setTelefono("96612345");
        assertTrue(hayError(ContribuyenteValidador.validar(c), "teléfono"));
        c.setTelefono("866123456");
        assertTrue(hayError(ContribuyenteValidador.validar(c), "teléfono"));
    }

    @Test
    void emailSinArrobaEsInvalido() {
        Contribuyente c = natural();
        c.setEmail("ana.example.com");
        assertTrue(hayError(ContribuyenteValidador.validar(c), "correo"));
    }

    @Test
    void emailDeMasDe100CaracteresEsInvalido() {
        Contribuyente c = natural();
        c.setEmail("a".repeat(95) + "@x.com");
        assertTrue(hayError(ContribuyenteValidador.validar(c), "correo"));
    }

    @Test
    void nombreObligatorioDeAlMenos3Caracteres() {
        Contribuyente c = natural();
        c.setNombresRazonSocial("  ab ");
        assertTrue(hayError(ContribuyenteValidador.validar(c), "obligatorio"));
        c.setNombresRazonSocial(null);
        assertTrue(hayError(ContribuyenteValidador.validar(c), "obligatorio"));
    }

    @Test
    void nombreDeMasDe150CaracteresEsInvalido() {
        Contribuyente c = natural();
        c.setNombresRazonSocial("a".repeat(151));
        assertTrue(hayError(ContribuyenteValidador.validar(c), "150"));
        c.setNombresRazonSocial("a".repeat(150));
        assertEquals(List.of(), ContribuyenteValidador.validar(c));
    }

    @Test
    void fechaDeNacimientoFuturaEsInvalida() {
        Contribuyente c = natural();
        c.setFechaNacimiento(LocalDate.now().plusDays(1));
        assertTrue(hayError(ContribuyenteValidador.validar(c), "futura"));
    }

    @Test
    void fechaDeNacimientoAnteriorA1900EsInvalida() {
        Contribuyente c = natural();
        c.setFechaNacimiento(LocalDate.of(1899, 12, 31));
        assertTrue(hayError(ContribuyenteValidador.validar(c), "1900"));
        c.setFechaNacimiento(LocalDate.of(1900, 1, 1));
        assertEquals(List.of(), ContribuyenteValidador.validar(c));
    }

    @Test
    void fechaDeNacimientoSoloParaPersonasNaturales() {
        Contribuyente c = juridica();
        c.setFechaNacimiento(LocalDate.of(2000, 1, 1));
        assertTrue(hayError(ContribuyenteValidador.validar(c), "solo corresponde a personas naturales"));
    }

    @Test
    void direccionDeMasDe200CaracteresEsInvalida() {
        Contribuyente c = natural();
        c.setDireccion("a".repeat(201));
        assertTrue(hayError(ContribuyenteValidador.validar(c), "200"));
        c.setDireccion("a".repeat(200));
        assertEquals(List.of(), ContribuyenteValidador.validar(c));
    }

    @Test
    void contribuyenteNuloNoLanzaExcepcion() {
        assertEquals(1, ContribuyenteValidador.validar(null).size());
    }

    @Test
    void variosErroresSeAcumulan() {
        Contribuyente c = natural();
        c.setDniRuc("1");
        c.setTelefono("1");
        c.setEmail("x");
        assertEquals(3, ContribuyenteValidador.validar(c).size());
    }
}

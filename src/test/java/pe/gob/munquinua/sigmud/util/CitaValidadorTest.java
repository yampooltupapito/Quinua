package pe.gob.munquinua.sigmud.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.gob.munquinua.sigmud.dao.AreaDAO;
import pe.gob.munquinua.sigmud.dao.ContribuyenteDAO;
import pe.gob.munquinua.sigmud.dao.UsuarioDAO;
import pe.gob.munquinua.sigmud.model.Area;
import pe.gob.munquinua.sigmud.model.Cita;
import pe.gob.munquinua.sigmud.model.Contribuyente;
import pe.gob.munquinua.sigmud.model.Usuario;

/**
 * Pruebas de CitaValidador SIN base de datos: los DAO son falsos (datos en memoria) y el reloj es fijo:
 * lunes 16/11/2026, 09:00 (hora de Lima).
 */
class CitaValidadorTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);   // lunes

    private CitaValidador validador;

    @BeforeEach
    void preparar() {
        validador = crearValidador(HOY.atTime(9, 0));
    }

    // ---------- DAO falsos ----------

    private CitaValidador crearValidador(LocalDateTime ahora) {
        Clock reloj = Clock.fixed(ahora.atZone(LIMA).toInstant(), LIMA);
        return new CitaValidador(contribuyentesFalsos(), areasFalsas(), usuariosFalsos(), reloj);
    }

    /** Contribuyente 1 = activo, 2 = dado de baja; cualquier otro no existe. */
    private ContribuyenteDAO contribuyentesFalsos() {
        return (ContribuyenteDAO) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {ContribuyenteDAO.class}, (p, m, a) -> {
                    if (!m.getName().equals("buscarPorId")) {
                        throw new UnsupportedOperationException(m.getName());
                    }
                    int id = (Integer) a[0];
                    if (id != 1 && id != 2) {
                        return Optional.empty();
                    }
                    Contribuyente c = new Contribuyente();
                    c.setIdContribuyente(id);
                    c.setEstado(id == 1);
                    return Optional.of(c);
                });
    }

    /** Áreas activas 1 y 2. */
    private AreaDAO areasFalsas() {
        return () -> List.of(area(1), area(2));
    }

    private Area area(int id) {
        Area a = new Area();
        a.setIdArea(id);
        a.setEstado(true);
        return a;
    }

    /** Funcionario 10 pertenece al área 1; funcionario 20, al área 2. */
    private UsuarioDAO usuariosFalsos() {
        return (UsuarioDAO) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {UsuarioDAO.class}, (p, m, a) -> {
                    if (!m.getName().equals("listarFuncionariosPorArea")) {
                        throw new UnsupportedOperationException(m.getName());
                    }
                    int area = (Integer) a[0];
                    Usuario u = new Usuario();
                    u.setIdUsuario(area == 1 ? 10 : 20);
                    return area == 1 || area == 2 ? List.of(u) : List.of();
                });
    }

    // ---------- datos de las pruebas ----------

    /** Cita correcta: martes 17/11/2026 a las 10:00. */
    private Cita citaValida() {
        Cita c = new Cita();
        c.setIdContribuyente(1);
        c.setIdArea(1);
        c.setIdFuncionario(10);
        c.setVentanilla(3);
        c.setFecha(HOY.plusDays(1));
        c.setHora(LocalTime.of(10, 0));
        c.setMotivo("Consulta de expediente");
        return c;
    }

    private boolean hayError(List<String> errores, String fragmento) {
        return errores.stream().anyMatch(e -> e.contains(fragmento));
    }

    // ---------- caso válido ----------

    @Test
    void citaValidaNoTieneErrores() {
        assertEquals(List.of(), validador.validar(citaValida()));
    }

    @Test
    void citaNulaNoLanzaExcepcion() {
        assertEquals(1, validador.validar(null).size());
    }

    @Test
    void citaVaciaAcumulaVariosErroresSinLanzarExcepcion() {
        assertTrue(validador.validar(new Cita()).size() >= 6);
    }

    // ---------- contribuyente ----------

    @Test
    void contribuyenteObligatorio() {
        Cita c = citaValida();
        c.setIdContribuyente(0);
        assertTrue(hayError(validador.validar(c), "Seleccione el contribuyente"));
    }

    @Test
    void contribuyenteInexistenteEsInvalido() {
        Cita c = citaValida();
        c.setIdContribuyente(99);
        assertTrue(hayError(validador.validar(c), "no existe"));
    }

    @Test
    void contribuyenteDadoDeBajaEsInvalido() {
        Cita c = citaValida();
        c.setIdContribuyente(2);
        assertTrue(hayError(validador.validar(c), "dado de baja"));
    }

    // ---------- área y funcionario ----------

    @Test
    void areaObligatoriaEInexistente() {
        Cita c = citaValida();
        c.setIdArea(0);
        assertTrue(hayError(validador.validar(c), "Seleccione el área"));
        c.setIdArea(99);
        assertTrue(hayError(validador.validar(c), "área seleccionada no es válida"));
    }

    @Test
    void funcionarioObligatorio() {
        Cita c = citaValida();
        c.setIdFuncionario(0);
        assertTrue(hayError(validador.validar(c), "Seleccione el funcionario"));
    }

    @Test
    void funcionarioDeOtraAreaEsInvalido() {
        Cita c = citaValida();
        c.setIdFuncionario(20);   // pertenece al área 2, pero la cita es del área 1
        assertTrue(hayError(validador.validar(c), "no pertenece al área"));
    }

    @Test
    void funcionarioDelAreaCorrectaEsValido() {
        Cita c = citaValida();
        c.setIdArea(2);
        c.setIdFuncionario(20);
        assertEquals(List.of(), validador.validar(c));
    }

    // ---------- fecha ----------

    @Test
    void fechaPasadaEsInvalida() {
        Cita c = citaValida();
        c.setFecha(HOY.minusDays(1));
        assertTrue(hayError(validador.validar(c), "anterior a hoy"));
    }

    @Test
    void fechaObligatoria() {
        Cita c = citaValida();
        c.setFecha(null);
        assertTrue(hayError(validador.validar(c), "Seleccione la fecha"));
    }

    @Test
    void sabadoYDomingoSonInvalidos() {
        Cita c = citaValida();
        c.setFecha(LocalDate.of(2026, 11, 21));   // sábado
        assertTrue(hayError(validador.validar(c), "lunes a viernes"));
        c.setFecha(LocalDate.of(2026, 11, 22));   // domingo
        assertTrue(hayError(validador.validar(c), "lunes a viernes"));
    }

    @Test
    void lunesAViernesSonValidos() {
        for (int dia = 17; dia <= 20; dia++) {
            Cita c = citaValida();
            c.setFecha(LocalDate.of(2026, 11, dia));
            assertEquals(List.of(), validador.validar(c), "día " + dia);
        }
    }

    @Test
    void anticipacionMaximaDe90Dias() {
        // Reloj en martes 17/11/2026: +90 días = lunes 15/02/2027 (válido) y +91 = martes 16/02/2027 (inválido)
        CitaValidador v = crearValidador(LocalDate.of(2026, 11, 17).atTime(9, 0));
        Cita c = citaValida();
        c.setFecha(LocalDate.of(2027, 2, 15));
        assertEquals(List.of(), v.validar(c));
        c.setFecha(LocalDate.of(2027, 2, 16));
        assertTrue(hayError(v.validar(c), "90 días"));
    }

    // ---------- hora ----------

    @ParameterizedTest
    @ValueSource(strings = {"08:00", "08:30", "12:00", "13:30", "16:00", "16:30"})
    void horasValidas(String hora) {
        Cita c = citaValida();
        c.setHora(LocalTime.parse(hora));
        assertEquals(List.of(), validador.validar(c));
    }

    @ParameterizedTest
    @ValueSource(strings = {"07:30", "00:00", "16:31", "16:45", "17:00", "23:30"})
    void horasFueraDeHorario(String hora) {
        Cita c = citaValida();
        c.setHora(LocalTime.parse(hora));
        assertTrue(hayError(validador.validar(c), "entre 08:00 y 16:30"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"09:15", "09:45", "10:01", "08:10"})
    void horasQueNoSonMultiploDe30Minutos(String hora) {
        Cita c = citaValida();
        c.setHora(LocalTime.parse(hora));
        assertTrue(hayError(validador.validar(c), "intervalos de 30 minutos"));
    }

    @Test
    void horaConSegundosNoEsValida() {
        Cita c = citaValida();
        c.setHora(LocalTime.of(10, 0, 30));
        assertTrue(hayError(validador.validar(c), "intervalos de 30 minutos"));
    }

    @Test
    void horaObligatoria() {
        Cita c = citaValida();
        c.setHora(null);
        assertTrue(hayError(validador.validar(c), "Seleccione la hora"));
    }

    @Test
    void siLaFechaEsHoyLaHoraNoPuedeHaberPasado() {
        Cita c = citaValida();
        c.setFecha(HOY);                        // hoy, y el reloj marca las 09:00
        c.setHora(LocalTime.of(8, 30));
        assertTrue(hayError(validador.validar(c), "ya pasó"));
        c.setHora(LocalTime.of(9, 0));
        assertFalse(hayError(validador.validar(c), "ya pasó"), "la hora actual todavía es válida");
        c.setHora(LocalTime.of(9, 30));
        assertEquals(List.of(), validador.validar(c));
    }

    @Test
    void enOtroDiaUnaHoraTempranaNoSeConsideraPasada() {
        Cita c = citaValida();   // mañana a las 10:00; el reloj marca las 09:00 de hoy
        c.setHora(LocalTime.of(8, 0));
        assertEquals(List.of(), validador.validar(c));
    }

    // ---------- ventanilla ----------

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10})
    void ventanillasValidas(int ventanilla) {
        Cita c = citaValida();
        c.setVentanilla(ventanilla);
        assertEquals(List.of(), validador.validar(c));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 11, 255})
    void ventanillasInvalidas(int ventanilla) {
        Cita c = citaValida();
        c.setVentanilla(ventanilla);
        assertTrue(hayError(validador.validar(c), "ventanilla"));
    }

    // ---------- motivo ----------

    @Test
    void motivoObligatorio() {
        Cita c = citaValida();
        c.setMotivo("   ");
        assertTrue(hayError(validador.validar(c), "motivo es obligatorio"));
        c.setMotivo(null);
        assertTrue(hayError(validador.validar(c), "motivo es obligatorio"));
    }

    @Test
    void motivoLimiteDe200Caracteres() {
        Cita c = citaValida();
        c.setMotivo("a".repeat(200));
        assertEquals(List.of(), validador.validar(c));
        c.setMotivo("a".repeat(201));
        assertTrue(hayError(validador.validar(c), "200 caracteres"));
    }
}

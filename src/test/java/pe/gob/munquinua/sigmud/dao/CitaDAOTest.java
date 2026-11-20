package pe.gob.munquinua.sigmud.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.gob.munquinua.sigmud.dao.impl.CitaDAOImpl;
import pe.gob.munquinua.sigmud.exception.CitaCruzadaException;
import pe.gob.munquinua.sigmud.exception.CitaEstadoException;
import pe.gob.munquinua.sigmud.exception.CitaNoEncontradaException;
import pe.gob.munquinua.sigmud.model.Cita;

/**
 * Pruebas de CitaDAO (cruces de horario y estados) contra mun_quinua_test.
 * Las citas de prueba llevan el motivo "TEST-JUNIT" y se borran al terminar cada prueba.
 */
class CitaDAOTest extends BaseDaoTest {

    private static final String MOTIVO = "TEST-JUNIT";
    private static final LocalTime DIEZ = LocalTime.of(10, 0);

    private final CitaDAO dao = new CitaDAOImpl();
    private int idContribuyente;
    private int idFuncionario1;   // atencion1 (área 1)
    private int idFuncionario2;   // atencion2 (área 2)
    private LocalDate dia;        // un día hábil futuro

    @BeforeEach
    void preparar() {
        idContribuyente = contar("SELECT MIN(id_contribuyente) FROM contribuyente WHERE estado = 1");
        idFuncionario1 = contar("SELECT id_usuario FROM usuario WHERE username = 'atencion1'");
        idFuncionario2 = contar("SELECT id_usuario FROM usuario WHERE username = 'atencion2'");
        LocalDate d = LocalDate.now().plusDays(10);
        while (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
            d = d.plusDays(1);
        }
        dia = d;
    }

    @AfterEach
    void limpiar() {
        ejecutar("DELETE FROM cita WHERE motivo = ?", MOTIVO);
    }

    private Cita cita(int funcionario, int area, LocalDate fecha, LocalTime hora) {
        Cita c = new Cita();
        c.setIdContribuyente(idContribuyente);
        c.setIdArea(area);
        c.setIdFuncionario(funcionario);
        c.setVentanilla(1);
        c.setFecha(fecha);
        c.setHora(hora);
        c.setMotivo(MOTIVO);
        return c;
    }

    private int citasActivas(int funcionario, LocalDate fecha, LocalTime hora) {
        return contar("SELECT COUNT(*) FROM cita WHERE id_funcionario = ? AND fecha = ? AND hora = ? "
                + "AND estado <> 'CANCELADA'", funcionario, java.sql.Date.valueOf(fecha), java.sql.Time.valueOf(hora));
    }

    // ---------- cruce de horario ----------

    @Test
    void insertarGuardaLaCitaProgramadaConSuId() throws Exception {
        Cita c = cita(idFuncionario1, 1, dia, DIEZ);
        dao.insertar(c);
        assertTrue(c.getIdCita() > 0);
        Cita guardada = dao.buscarPorId(c.getIdCita()).orElseThrow();
        assertEquals("PROGRAMADA", guardada.getEstado());
        assertEquals(DIEZ, guardada.getHora());
        assertEquals("Mesa de Partes", guardada.getNombreArea());
        assertEquals("Funcionario de Atencion 1", guardada.getNombreFuncionario());
        assertTrue(guardada.getNombreContribuyente() != null && guardada.getDniRuc() != null);
    }

    @Test
    void elMismoFuncionarioFechaYHoraSeRechazaPorCruce() throws Exception {
        dao.insertar(cita(idFuncionario1, 1, dia, DIEZ));
        CitaCruzadaException e = assertThrows(CitaCruzadaException.class,
                () -> dao.insertar(cita(idFuncionario1, 1, dia, DIEZ)));
        assertTrue(e.getMessage().contains("10:00"), "el mensaje indica la hora ocupada");
        assertEquals(1, citasActivas(idFuncionario1, dia, DIEZ), "solo quedó la primera cita");
    }

    @Test
    void otroFuncionarioALaMismaHoraSiSePermite() throws Exception {
        dao.insertar(cita(idFuncionario1, 1, dia, DIEZ));
        dao.insertar(cita(idFuncionario2, 2, dia, DIEZ));
        assertEquals(1, citasActivas(idFuncionario1, dia, DIEZ));
        assertEquals(1, citasActivas(idFuncionario2, dia, DIEZ));
    }

    @Test
    void elMismoFuncionarioEnOtraHoraOOtroDiaSiSePermite() throws Exception {
        dao.insertar(cita(idFuncionario1, 1, dia, DIEZ));
        dao.insertar(cita(idFuncionario1, 1, dia, LocalTime.of(10, 30)));
        dao.insertar(cita(idFuncionario1, 1, dia.plusDays(1), DIEZ));
        assertEquals(3, contar("SELECT COUNT(*) FROM cita WHERE motivo = ?", MOTIVO));
    }

    @Test
    void existeCruceYHorasOcupadasConsideranSoloCitasNoCanceladas() throws Exception {
        Cita c = cita(idFuncionario1, 1, dia, DIEZ);
        dao.insertar(c);
        assertTrue(dao.existeCruce(idFuncionario1, dia, DIEZ, 0));
        assertFalse(dao.existeCruce(idFuncionario1, dia, DIEZ, c.getIdCita()), "excluyendo la propia cita no hay cruce");
        assertFalse(dao.existeCruce(idFuncionario2, dia, DIEZ, 0));
        assertEquals(List.of(DIEZ), dao.horasOcupadas(idFuncionario1, dia));
    }

    // ---------- cancelar libera el horario ----------

    @Test
    void cancelarUnaCitaLiberaElHorario() throws Exception {
        Cita c = cita(idFuncionario1, 1, dia, DIEZ);
        dao.insertar(c);
        dao.cambiarEstado(c.getIdCita(), Cita.CANCELADA);

        assertEquals("CANCELADA", dao.buscarPorId(c.getIdCita()).orElseThrow().getEstado());
        assertFalse(dao.existeCruce(idFuncionario1, dia, DIEZ, 0), "la cancelada ya no bloquea");
        assertTrue(dao.horasOcupadas(idFuncionario1, dia).isEmpty());

        dao.insertar(cita(idFuncionario1, 1, dia, DIEZ));   // se puede volver a agendar esa hora
        assertEquals(1, citasActivas(idFuncionario1, dia, DIEZ));
        assertEquals(2, contar("SELECT COUNT(*) FROM cita WHERE motivo = ? AND hora = ?", MOTIVO, java.sql.Time.valueOf(DIEZ)));
    }

    // ---------- segunda defensa: el UNIQUE de la base ----------

    @Test
    void siFallaLaConsultaPreviaElUniqueDeLaBaseTambienRechazaElCruce() throws Exception {
        // Simula dos personas agendando a la vez: la consulta previa "no ve" la cita de la otra persona.
        CitaDAOImpl sinConsultaPrevia = new CitaDAOImpl() {
            @Override
            protected boolean hayCruce(Connection cn, int f, LocalDate d, LocalTime h, int x) {
                return false;
            }
        };
        sinConsultaPrevia.insertar(cita(idFuncionario1, 1, dia, DIEZ));
        CitaCruzadaException e = assertThrows(CitaCruzadaException.class,
                () -> sinConsultaPrevia.insertar(cita(idFuncionario1, 1, dia, DIEZ)));
        assertInstanceOf(SQLIntegrityConstraintViolationException.class, e.getCause(),
                "el rechazo vino del UNIQUE uq_cita_funcionario_horario");
        assertEquals(1, citasActivas(idFuncionario1, dia, DIEZ));
    }

    // ---------- reprogramar ----------

    @Test
    void reprogramarAUnaHoraOcupadaSeRechaza() throws Exception {
        dao.insertar(cita(idFuncionario1, 1, dia, DIEZ));
        Cita otra = cita(idFuncionario1, 1, dia, LocalTime.of(11, 0));
        dao.insertar(otra);
        assertThrows(CitaCruzadaException.class, () -> dao.reprogramar(otra.getIdCita(), dia, DIEZ, 1));
        assertEquals(LocalTime.of(11, 0), dao.buscarPorId(otra.getIdCita()).orElseThrow().getHora(), "no cambió");
    }

    @Test
    void reprogramarSinCambiarLaHoraSiSePermite() throws Exception {
        Cita c = cita(idFuncionario1, 1, dia, DIEZ);
        dao.insertar(c);
        dao.reprogramar(c.getIdCita(), dia, DIEZ, 7);   // mismo horario, otra ventanilla
        assertEquals(7, dao.buscarPorId(c.getIdCita()).orElseThrow().getVentanilla());
    }

    @Test
    void reprogramarAUnTurnoLibreCambiaFechaHoraYVentanilla() throws Exception {
        Cita c = cita(idFuncionario1, 1, dia, DIEZ);
        dao.insertar(c);
        LocalDate otroDia = dia.plusDays(7);
        dao.reprogramar(c.getIdCita(), otroDia, LocalTime.of(15, 30), 4);
        Cita r = dao.buscarPorId(c.getIdCita()).orElseThrow();
        assertEquals(otroDia, r.getFecha());
        assertEquals(LocalTime.of(15, 30), r.getHora());
        assertEquals(4, r.getVentanilla());
        assertFalse(dao.existeCruce(idFuncionario1, dia, DIEZ, 0), "el turno anterior quedó libre");
    }

    // ---------- reglas de estado ----------

    @Test
    void unaCitaCanceladaNoPuedePasarAAtendida() throws Exception {
        Cita c = cita(idFuncionario1, 1, dia, DIEZ);
        dao.insertar(c);
        dao.cambiarEstado(c.getIdCita(), Cita.CANCELADA);
        assertThrows(CitaEstadoException.class, () -> dao.cambiarEstado(c.getIdCita(), Cita.ATENDIDA));
        assertEquals("CANCELADA", dao.buscarPorId(c.getIdCita()).orElseThrow().getEstado());
    }

    @Test
    void unaCitaCanceladaOAtendidaNoSeReprograma() throws Exception {
        Cita c = cita(idFuncionario1, 1, dia, DIEZ);
        dao.insertar(c);
        dao.cambiarEstado(c.getIdCita(), Cita.CANCELADA);
        assertThrows(CitaEstadoException.class, () -> dao.reprogramar(c.getIdCita(), dia, LocalTime.of(12, 0), 1));

        Cita pasada = cita(idFuncionario2, 2, LocalDate.now().minusDays(1), DIEZ);   // directo a la BD (fecha pasada)
        dao.insertar(pasada);
        dao.cambiarEstado(pasada.getIdCita(), Cita.ATENDIDA);
        assertThrows(CitaEstadoException.class, () -> dao.reprogramar(pasada.getIdCita(), dia, DIEZ, 1));
        assertThrows(CitaEstadoException.class, () -> dao.cambiarEstado(pasada.getIdCita(), Cita.CANCELADA));
    }

    @Test
    void soloSePuedeAtenderElDiaDeLaCitaODespues() throws Exception {
        Cita futura = cita(idFuncionario1, 1, dia, DIEZ);
        dao.insertar(futura);
        assertThrows(CitaEstadoException.class, () -> dao.cambiarEstado(futura.getIdCita(), Cita.ATENDIDA));

        Cita hoy = cita(idFuncionario2, 2, LocalDate.now(), DIEZ);
        dao.insertar(hoy);
        dao.cambiarEstado(hoy.getIdCita(), Cita.ATENDIDA);
        assertEquals("ATENDIDA", dao.buscarPorId(hoy.getIdCita()).orElseThrow().getEstado());
    }

    @Test
    void unEstadoNuevoInvalidoOUnaCitaInexistenteSeRechazan() throws Exception {
        Cita c = cita(idFuncionario1, 1, dia, DIEZ);
        dao.insertar(c);
        assertThrows(CitaEstadoException.class, () -> dao.cambiarEstado(c.getIdCita(), Cita.PROGRAMADA));
        assertThrows(CitaEstadoException.class, () -> dao.cambiarEstado(c.getIdCita(), "XYZ"));
        assertThrows(CitaNoEncontradaException.class, () -> dao.cambiarEstado(999999, Cita.CANCELADA));
        assertThrows(CitaNoEncontradaException.class, () -> dao.reprogramar(999999, dia, DIEZ, 1));
    }

    // ---------- listados ----------

    @Test
    void listarPorFechaSeOrdenaPorHoraYFiltraPorAreaOpcional() throws Exception {
        dao.insertar(cita(idFuncionario1, 1, dia, LocalTime.of(14, 0)));
        dao.insertar(cita(idFuncionario2, 2, dia, LocalTime.of(9, 0)));
        List<Cita> todas = dao.listarPorFecha(dia, null);
        assertEquals(2, todas.size());
        assertEquals(LocalTime.of(9, 0), todas.get(0).getHora(), "ordenadas por hora");
        List<Cita> area1 = dao.listarPorFecha(dia, 1);
        assertEquals(1, area1.size());
        assertEquals(LocalTime.of(14, 0), area1.get(0).getHora());
    }

    @Test
    void listarPorContribuyenteTraeSusCitas() throws Exception {
        dao.insertar(cita(idFuncionario1, 1, dia, DIEZ));
        assertTrue(dao.listarPorContribuyente(idContribuyente).stream().anyMatch(x -> MOTIVO.equals(x.getMotivo())));
    }
}

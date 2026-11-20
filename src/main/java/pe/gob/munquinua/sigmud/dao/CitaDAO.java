package pe.gob.munquinua.sigmud.dao;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import pe.gob.munquinua.sigmud.exception.CitaCruzadaException;
import pe.gob.munquinua.sigmud.exception.CitaEstadoException;
import pe.gob.munquinua.sigmud.exception.CitaNoEncontradaException;
import pe.gob.munquinua.sigmud.model.Cita;
import pe.gob.munquinua.sigmud.model.Pagina;
import pe.gob.munquinua.sigmud.model.ResumenCitas;

/** Acceso a datos de citas: registro, reprogramación, cambios de estado y control de cruces de horario. */
public interface CitaDAO {

    /**
     * Inserta una cita PROGRAMADA y deja el id generado. Defensa 1: consulta existeCruce.
     * Defensa 2: si aun así la base rechaza por el UNIQUE, también lanza CitaCruzadaException.
     */
    void insertar(Cita cita) throws CitaCruzadaException;

    /** Cambia fecha, hora y ventanilla. Solo para citas PROGRAMADA; revisa el cruce excluyendo la propia cita. */
    void reprogramar(int idCita, LocalDate fecha, LocalTime hora, int ventanilla)
            throws CitaNoEncontradaException, CitaEstadoException, CitaCruzadaException;

    /**
     * Pasa una cita PROGRAMADA a ATENDIDA o CANCELADA. ATENDIDA solo el día de la cita o después.
     * Una cita ya ATENDIDA o CANCELADA no se puede cambiar.
     */
    void cambiarEstado(int idCita, String nuevoEstado) throws CitaNoEncontradaException, CitaEstadoException;

    Optional<Cita> buscarPorId(int idCita);

    /** Citas de una fecha con nombres de contribuyente, área y funcionario, ordenadas por hora. idArea es opcional (null = todas). */
    List<Cita> listarPorFecha(LocalDate fecha, Integer idArea);

    /** Citas de un contribuyente, las más recientes primero. */
    List<Cita> listarPorContribuyente(int idContribuyente);

    /**
     * Citas de un contribuyente, de la más reciente a la más antigua, con área y funcionario. estado es opcional
     * (PROGRAMADA, ATENDIDA o CANCELADA; null o vacío = todos).
     */
    List<Cita> listarPorContribuyente(int idContribuyente, String estado);

    /**
     * Una página del historial de citas del contribuyente, con filtro opcional por estado y orden por columna (fecha,
     * hora, area, funcionario, motivo o estado; otro valor = fecha descendente). LIMIT y OFFSET van como parámetros.
     */
    Pagina<Cita> listarPorContribuyentePagina(int idContribuyente, String estado, int pagina, int tamano,
                                              String orden, String dir);

    /** Total de citas del contribuyente y cuántas están programadas, atendidas y canceladas (una sola consulta). */
    ResumenCitas resumenPorContribuyente(int idContribuyente);

    /** La próxima cita PROGRAMADA del contribuyente desde hoy (la más cercana), o null si no tiene. */
    Cita proximaCita(int idContribuyente);

    /** true si el funcionario ya tiene una cita NO cancelada en esa fecha y hora (excluye idCitaExcluida; use 0 si es nueva). */
    boolean existeCruce(int idFuncionario, LocalDate fecha, LocalTime hora, int idCitaExcluida);

    /** Horas ya tomadas por el funcionario en esa fecha (citas no canceladas), de menor a mayor. */
    List<LocalTime> horasOcupadas(int idFuncionario, LocalDate fecha);
}

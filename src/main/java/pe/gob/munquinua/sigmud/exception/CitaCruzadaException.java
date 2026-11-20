package pe.gob.munquinua.sigmud.exception;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** El funcionario ya tiene una cita (no cancelada) en esa fecha y hora. */
public class CitaCruzadaException extends Exception {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    public CitaCruzadaException(LocalDate fecha, LocalTime hora) {
        this(fecha, hora, null);
    }

    public CitaCruzadaException(LocalDate fecha, LocalTime hora, Throwable causa) {
        super("El funcionario ya tiene una cita el " + fecha.format(FECHA) + " a las "
                + hora.format(HORA) + ". Elija otro horario.", causa);
    }
}

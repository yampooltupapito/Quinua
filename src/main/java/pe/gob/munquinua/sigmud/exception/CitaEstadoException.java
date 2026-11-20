package pe.gob.munquinua.sigmud.exception;

/** La operación no está permitida por el estado de la cita (por ejemplo, reprogramar una cancelada). */
public class CitaEstadoException extends Exception {

    private static final long serialVersionUID = 1L;

    public CitaEstadoException(String mensaje) {
        super(mensaje);
    }
}

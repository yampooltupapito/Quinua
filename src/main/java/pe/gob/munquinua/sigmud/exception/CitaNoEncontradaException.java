package pe.gob.munquinua.sigmud.exception;

/** No existe la cita indicada. */
public class CitaNoEncontradaException extends Exception {

    private static final long serialVersionUID = 1L;

    public CitaNoEncontradaException() {
        super("La cita no existe.");
    }
}

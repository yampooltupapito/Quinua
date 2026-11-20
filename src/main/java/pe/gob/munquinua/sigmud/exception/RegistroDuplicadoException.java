package pe.gob.munquinua.sigmud.exception;

/** Ya existe un registro con el mismo valor único (por ejemplo, el DNI/RUC). */
public class RegistroDuplicadoException extends Exception {

    private static final long serialVersionUID = 1L;

    public RegistroDuplicadoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

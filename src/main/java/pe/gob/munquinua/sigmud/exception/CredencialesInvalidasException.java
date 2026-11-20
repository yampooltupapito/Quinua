package pe.gob.munquinua.sigmud.exception;

/** Usuario o contraseña incorrectos. El mensaje no revela cuál de los dos falló. */
public class CredencialesInvalidasException extends Exception {

    private static final long serialVersionUID = 1L;

    public CredencialesInvalidasException() {
        super("Usuario o contraseña incorrectos.");
    }
}

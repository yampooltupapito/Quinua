package pe.gob.munquinua.sigmud.exception;

/** La cuenta está desactivada (por 5 intentos fallidos o por el administrador). */
public class CuentaBloqueadaException extends Exception {

    private static final long serialVersionUID = 1L;

    public CuentaBloqueadaException() {
        super("La cuenta está bloqueada. Comuníquese con el administrador.");
    }
}

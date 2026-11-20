package pe.gob.munquinua.sigmud.exception;

/**
 * Error de acceso a datos. Envuelve la SQLException original para que
 * el detalle técnico vaya al log y el usuario vea un mensaje claro.
 */
public class DaoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DaoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public DaoException(String mensaje) {
        super(mensaje);
    }
}

package pe.gob.munquinua.sigmud.exception;

/** Se intentó desactivar o quitar el rol al último ADMIN activo: el sistema siempre debe tener al menos uno. */
public class UltimoAdminException extends Exception {

    private static final long serialVersionUID = 1L;

    public UltimoAdminException() {
        super("Siempre debe quedar al menos un administrador activo.");
    }
}

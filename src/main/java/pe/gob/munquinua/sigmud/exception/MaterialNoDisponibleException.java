package pe.gob.munquinua.sigmud.exception;

/** El material no existe o está dado de baja, así que no admite movimientos. */
public class MaterialNoDisponibleException extends Exception {

    private static final long serialVersionUID = 1L;

    public MaterialNoDisponibleException() {
        super("El material no existe o está dado de baja.");
    }
}

package pe.gob.munquinua.sigmud.util;

import java.util.ArrayList;
import java.util.List;
import pe.gob.munquinua.sigmud.model.Usuario;

/**
 * Valida los datos de un usuario que escribe el ADMIN (nuevo o edición) y devuelve los errores en español.
 * No consulta la base de datos: que el nombre de usuario sea único y que el área exista lo comprueba el servlet.
 */
public final class UsuarioValidador {

    /** Roles que existen en el sistema. */
    public static final List<String> ROLES = List.of("ADMIN", "ATENCION", "ALMACEN");
    private static final int MIN_NOMBRES = 3;
    private static final int MAX_NOMBRES = 100;

    private UsuarioValidador() { }

    /** @param esNuevo en una edición el nombre de usuario no se puede cambiar, así que no se valida */
    public static List<String> validar(Usuario u, boolean esNuevo) {
        List<String> errores = new ArrayList<>();
        if (u == null) {
            errores.add("No se recibieron los datos del usuario.");
            return errores;
        }
        if (esNuevo && !Validador.usuarioValido(u.getUsername())) {
            errores.add("El nombre de usuario debe tener de 3 a 30 caracteres: letras, números, punto, guion o guion bajo.");
        }
        String nombres = u.getNombres() == null ? "" : u.getNombres().trim();
        if (nombres.length() < MIN_NOMBRES || nombres.length() > MAX_NOMBRES) {
            errores.add("El nombre completo es obligatorio y debe tener de 3 a 100 caracteres.");
        }
        if (u.getRol() == null || !ROLES.contains(u.getRol())) {
            errores.add("Seleccione un rol válido (ADMIN, ATENCION o ALMACEN).");
        } else if ("ATENCION".equals(u.getRol()) && (u.getIdArea() == null || u.getIdArea() <= 0)) {
            errores.add("Un usuario de ATENCION debe tener un área asignada.");
        }
        return errores;
    }
}

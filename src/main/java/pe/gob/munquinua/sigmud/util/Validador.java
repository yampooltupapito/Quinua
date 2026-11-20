package pe.gob.munquinua.sigmud.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Validaciones con expresiones regulares. Todas devuelven false si el texto es nulo. */
public final class Validador {

    /** 3 a 30 caracteres: letras, números, punto, guion y guion bajo. */
    private static final Pattern USUARIO = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");
    /** DNI: exactamente 8 dígitos. */
    private static final Pattern DNI = Pattern.compile("^\\d{8}$");
    /** RUC: 11 dígitos que empiezan con 10 (persona natural) o 20 (persona jurídica). */
    private static final Pattern RUC = Pattern.compile("^(10|20)\\d{9}$");
    /** Celular: 9 dígitos que empiezan con 9. */
    private static final Pattern TELEFONO = Pattern.compile("^9\\d{8}$");
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD = 100;

    private Validador() { }

    /** true si el texto es nulo o solo tiene espacios. */
    public static boolean vacio(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static boolean usuarioValido(String s) { return coincide(USUARIO, s); }

    /**
     * Política de contraseña: mínimo 8 caracteres, al menos una mayúscula, una minúscula y un número, y no puede
     * ser igual al nombre de usuario. Devuelve los mensajes de error en español (lista vacía si cumple).
     *
     * @param username nombre de usuario con el que se compara (puede ser null)
     */
    public static List<String> politicaPassword(String password, String username) {
        List<String> errores = new ArrayList<>();
        String p = password == null ? "" : password;
        if (p.length() < MIN_PASSWORD) {
            errores.add("La contraseña debe tener al menos " + MIN_PASSWORD + " caracteres.");
        }
        if (p.length() > MAX_PASSWORD) {
            errores.add("La contraseña no puede pasar de " + MAX_PASSWORD + " caracteres.");
        }
        if (!p.matches(".*\\p{Lu}.*")) {
            errores.add("La contraseña debe incluir al menos una letra mayúscula.");
        }
        if (!p.matches(".*\\p{Ll}.*")) {
            errores.add("La contraseña debe incluir al menos una letra minúscula.");
        }
        if (!p.matches(".*\\d.*")) {
            errores.add("La contraseña debe incluir al menos un número.");
        }
        if (username != null && !username.isEmpty() && p.equalsIgnoreCase(username.trim())) {
            errores.add("La contraseña no puede ser igual al nombre de usuario.");
        }
        return errores;
    }

    public static boolean dniValido(String s) { return coincide(DNI, s); }

    public static boolean rucValido(String s) { return coincide(RUC, s); }

    public static boolean telefonoValido(String s) { return coincide(TELEFONO, s); }

    public static boolean emailValido(String s) { return coincide(EMAIL, s); }

    private static boolean coincide(Pattern patron, String texto) {
        return texto != null && patron.matcher(texto.trim()).matches();
    }
}

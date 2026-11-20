package pe.gob.munquinua.sigmud.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import pe.gob.munquinua.sigmud.model.Material;

/** Valida un Material y devuelve la lista de errores en español (vacía si todo está bien). */
public final class MaterialValidador {

    /** Unidades permitidas (también alimentan la lista desplegable del formulario). */
    public static final List<String> UNIDADES =
            List.of("UNIDAD", "PAQUETE", "BOLSA", "CAJA", "KILO", "LITRO", "METRO", "GALON");

    /** Tres letras mayúsculas, guion y tres dígitos. Ejemplo: MAT-006. */
    private static final Pattern CODIGO = Pattern.compile("^[A-Z]{3}-\\d{3}$");
    /** Entero sin signo (si fuera negativo tendría el signo menos y no coincide). */
    private static final Pattern ENTERO_NO_NEGATIVO = Pattern.compile("^\\d+$");
    private static final int MIN_NOMBRE = 3;
    private static final int MAX_NOMBRE = 100;

    private MaterialValidador() { }

    public static List<String> validar(Material m) {
        List<String> errores = new ArrayList<>();
        if (m == null) {
            errores.add("No se recibieron los datos del material.");
            return errores;
        }
        if (m.getCodigo() == null || !CODIGO.matcher(m.getCodigo().trim()).matches()) {
            errores.add("El código debe tener el formato AAA-999 (tres letras mayúsculas, guion y tres dígitos). Ejemplo: MAT-006.");
        }
        String nombre = m.getNombre() == null ? "" : m.getNombre().trim();
        if (nombre.length() < MIN_NOMBRE || nombre.length() > MAX_NOMBRE) {
            errores.add("El nombre es obligatorio y debe tener de 3 a 100 caracteres.");
        }
        if (m.getUnidad() == null || !UNIDADES.contains(m.getUnidad())) {
            errores.add("Seleccione una unidad de la lista: " + String.join(", ", UNIDADES) + ".");
        }
        if (!ENTERO_NO_NEGATIVO.matcher(String.valueOf(m.getStockMinimo())).matches()) {
            errores.add("El stock mínimo debe ser un número entero mayor o igual a 0.");
        }
        return errores;
    }
}

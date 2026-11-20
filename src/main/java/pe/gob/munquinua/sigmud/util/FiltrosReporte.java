package pe.gob.munquinua.sigmud.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Filtros del reporte leídos de la petición y validados. Solo se validan los filtros
 * que aplican al reporte elegido. Si hay errores, la consulta no se ejecuta.
 */
public final class FiltrosReporte {

    public static final Set<String> REPORTES = Set.of("stock", "movimientos", "resumen", "areas");
    public static final int MAX_DIAS = 366;
    private static final int DIAS_POR_DEFECTO = 30;
    private static final Pattern ENTERO = Pattern.compile("^\\d{1,9}$");

    private String reporte = "stock";
    private LocalDate desde;
    private LocalDate hasta;
    private String desdeTexto = "";
    private String hastaTexto = "";
    private String tipo = "";
    private Integer idArea;
    private Integer idMaterial;
    private String orden = "";
    private String dir = "asc";
    private final List<String> errores = new ArrayList<>();

    /** @param parametro función que devuelve el valor de un parámetro de la petición (o null) */
    public FiltrosReporte(Function<String, String> parametro) {
        String r = limpio(parametro.apply("reporte"));
        reporte = REPORTES.contains(r) ? r : "stock";
        orden = limpio(parametro.apply("orden"));
        dir = "desc".equals(limpio(parametro.apply("dir"))) ? "desc" : "asc";
        desdeTexto = limpio(parametro.apply("desde"));
        hastaTexto = limpio(parametro.apply("hasta"));
        if (desdeTexto.isEmpty()) {
            desdeTexto = LocalDate.now().minusDays(DIAS_POR_DEFECTO).toString();
        }
        if (hastaTexto.isEmpty()) {
            hastaTexto = LocalDate.now().toString();
        }
        tipo = limpio(parametro.apply("tipo"));
        validar(limpio(parametro.apply("area")), limpio(parametro.apply("material")));
    }

    private void validar(String area, String material) {
        if (!"stock".equals(reporte)) {
            validarPeriodo();
        }
        if ("movimientos".equals(reporte)) {
            validarTipo();
            idArea = entero(area, "El área no es válida.");
            idMaterial = entero(material, "El material no es válido.");
        }
    }

    private void validarPeriodo() {
        desde = fecha(desdeTexto, "La fecha \"desde\" no tiene un formato válido.");
        hasta = fecha(hastaTexto, "La fecha \"hasta\" no tiene un formato válido.");
        if (desde == null || hasta == null) {
            return;
        }
        if (desde.isAfter(hasta)) {
            errores.add("La fecha \"desde\" no puede ser mayor que la fecha \"hasta\".");
        } else if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MAX_DIAS) {
            errores.add("El periodo no puede pasar de " + MAX_DIAS + " días.");
        }
    }

    private void validarTipo() {
        if (!tipo.isEmpty() && !"ENTRADA".equals(tipo) && !"SALIDA".equals(tipo)) {
            errores.add("El tipo de movimiento no es válido.");
            tipo = "";
        }
    }

    private LocalDate fecha(String texto, String mensajeError) {
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            errores.add(mensajeError);
            return null;
        }
    }

    private Integer entero(String texto, String mensajeError) {
        if (texto.isEmpty()) {
            return null;
        }
        if (!ENTERO.matcher(texto).matches()) {
            errores.add(mensajeError);
            return null;
        }
        return Integer.valueOf(texto);
    }

    private static String limpio(String s) {
        return s == null ? "" : s.trim();
    }

    public void agregarError(String mensaje) { errores.add(mensaje); }

    public String getReporte() { return reporte; }
    public LocalDate getDesde() { return desde; }
    public LocalDate getHasta() { return hasta; }
    public String getDesdeTexto() { return desdeTexto; }
    public String getHastaTexto() { return hastaTexto; }
    public String getTipo() { return tipo; }
    public Integer getIdArea() { return idArea; }
    public Integer getIdMaterial() { return idMaterial; }
    public String getOrden() { return orden; }
    public String getDir() { return dir; }
    public List<String> getErrores() { return errores; }
}

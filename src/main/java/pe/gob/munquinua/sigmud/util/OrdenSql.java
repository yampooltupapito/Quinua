package pe.gob.munquinua.sigmud.util;

import java.util.Map;

/**
 * Orden de una consulta por columna SIN concatenar texto del usuario.
 * Cada DAO tiene una lista blanca (Map) que traduce el nombre que viene del navegador (por ejemplo "nombre") a un
 * fragmento FIJO de SQL (por ejemplo "nombres_razon_social"). Si el nombre no está en la lista (o trae ; -- comillas,
 * etc.) se usa el orden por defecto. La dirección solo puede ser ASC o DESC (se escribe el literal, no el texto recibido).
 */
public final class OrdenSql {

    private OrdenSql() { }

    /**
     * Resultado de resolver el orden pedido.
     *
     * @param columna   nombre efectivo (siempre una clave de la lista blanca)
     * @param direccion "asc" o "desc"
     * @param sql       cláusula " ORDER BY ..." con fragmentos fijos
     */
    public record Orden(String columna, String direccion, String sql) { }

    /** Dirección normalizada: solo "asc" o "desc"; cualquier otro valor devuelve el valor de respaldo. */
    public static String normalizarDireccion(String dir, String respaldo) {
        String d = dir == null ? "" : dir.trim();
        if ("asc".equalsIgnoreCase(d)) {
            return "asc";
        }
        if ("desc".equalsIgnoreCase(d)) {
            return "desc";
        }
        return respaldo;
    }

    /**
     * Resuelve el orden: la columna pedida solo se usa si está en la lista blanca; si no, se usa la columna por
     * defecto con su dirección por defecto.
     *
     * @param listaBlanca   nombre permitido -> fragmento fijo de SQL
     * @param pedida        columna pedida por el usuario (no confiable)
     * @param dirPedida     dirección pedida (no confiable)
     * @param porDefecto    nombre (de la lista blanca) del orden por defecto
     * @param dirPorDefecto "asc" o "desc" de ese orden por defecto
     * @param desempate     fragmento fijo para desempatar (por ejemplo "id_contribuyente"); puede ser ""
     */
    public static Orden resolver(Map<String, String> listaBlanca, String pedida, String dirPedida,
                                 String porDefecto, String dirPorDefecto, String desempate) {
        boolean valida = pedida != null && listaBlanca.containsKey(pedida);
        String columna = valida ? pedida : porDefecto;
        String respaldo = columna.equals(porDefecto) ? dirPorDefecto : "asc";
        String direccion = valida ? normalizarDireccion(dirPedida, respaldo) : dirPorDefecto;
        StringBuilder sql = new StringBuilder(" ORDER BY ").append(listaBlanca.get(columna))
                .append("desc".equals(direccion) ? " DESC" : " ASC");
        if (desempate != null && !desempate.isEmpty()) {
            sql.append(", ").append(desempate);
        }
        return new Orden(columna, direccion, sql.toString());
    }
}

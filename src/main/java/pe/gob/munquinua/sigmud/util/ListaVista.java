package pe.gob.munquinua.sigmud.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import pe.gob.munquinua.sigmud.model.Pagina;

/**
 * Lee del pedido los parámetros de un listado paginado y ordenable (pagina, tamano, orden, dir) y deja en la petición
 * lo que necesitan fragmentos/paginacion.jsp y el tag thOrden. Los valores que llegan del navegador nunca se confían:
 * página y tamaño se normalizan en {@link Pagina}; el orden lo valida la lista blanca de cada DAO ({@link OrdenSql}).
 */
public final class ListaVista {

    private static final int MAX_TEXTO = 30;

    private ListaVista() { }

    /** Página pedida; si no es un entero válido, 1 (después se corrige al rango real). */
    public static int pagina(HttpServletRequest req) {
        return entero(req.getParameter("pagina"), 1);
    }

    /** Tamaño pedido; Pagina.normalizarTamano lo convierte a 10, 20 o 50. */
    public static int tamano(HttpServletRequest req) {
        return Pagina.normalizarTamano(entero(req.getParameter("tamano"), Pagina.TAMANO_POR_DEFECTO));
    }

    /** Nombre de columna pedido (texto corto); el DAO decide si es válido. */
    public static String orden(HttpServletRequest req) {
        String o = req.getParameter("orden");
        o = o == null ? "" : o.trim();
        return o.length() > MAX_TEXTO ? o.substring(0, MAX_TEXTO) : o;
    }

    /** Dirección pedida (el DAO la normaliza a asc o desc). */
    public static String dir(HttpServletRequest req) {
        String d = req.getParameter("dir");
        return d == null ? "" : d.trim();
    }

    /**
     * Deja en la petición: pag (la página), listaBase (dirección del listado), listaFiltros (filtros que se conservan
     * en los enlaces), ordenActual y dirActual (para las flechas de los encabezados).
     */
    public static void preparar(HttpServletRequest req, Pagina<?> pag, String rutaBase, Map<String, String> filtros,
                                OrdenSql.Orden orden) {
        Map<String, String> limpios = new LinkedHashMap<>();
        filtros.forEach((k, v) -> {
            if (v != null && !v.isEmpty()) {
                limpios.put(k, v);
            }
        });
        req.setAttribute("pag", pag);
        req.setAttribute("listaBase", rutaBase);   // sin el contexto: c:url lo agrega
        req.setAttribute("listaFiltros", limpios);
        req.setAttribute("ordenActual", orden.columna());
        req.setAttribute("dirActual", orden.direccion());
    }

    private static int entero(String s, int porDefecto) {
        try {
            return s == null || s.isBlank() ? porDefecto : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }
}

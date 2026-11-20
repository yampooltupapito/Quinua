package pe.gob.munquinua.sigmud.model;

import java.io.Serializable;
import java.util.List;

/**
 * Una página de resultados de un listado (paginación del lado del servidor).
 * Los métodos estáticos normalizan lo que llega del navegador: el tamaño solo puede ser 10, 20 o 50 y la página
 * se corrige a un valor entre 1 y la última que existe.
 *
 * @param <T> tipo de los elementos
 */
public class Pagina<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Tamaños de página permitidos. */
    public static final List<Integer> TAMANOS = List.of(10, 20, 50);
    public static final int TAMANO_POR_DEFECTO = 10;

    private final List<T> items;
    private final int paginaActual;
    private final int tamano;
    private final long totalRegistros;
    private final int totalPaginas;

    public Pagina(List<T> items, int paginaActual, int tamano, long totalRegistros) {
        this.items = items == null ? List.of() : items;
        this.tamano = normalizarTamano(tamano);
        this.totalPaginas = paginasTotales(totalRegistros, this.tamano);
        this.paginaActual = normalizarPagina(paginaActual, totalRegistros, this.tamano);
        this.totalRegistros = Math.max(0, totalRegistros);
    }

    /** Cantidad de páginas para un total de registros (mínimo 1, aunque no haya registros). */
    public static int paginasTotales(long totalRegistros, int tamano) {
        int t = normalizarTamano(tamano);
        if (totalRegistros <= 0) {
            return 1;
        }
        return (int) Math.min(Integer.MAX_VALUE, (totalRegistros + t - 1) / t);
    }

    /** Solo 10, 20 o 50; cualquier otro valor se convierte en 10. */
    public static int normalizarTamano(int tamano) {
        return TAMANOS.contains(tamano) ? tamano : TAMANO_POR_DEFECTO;
    }

    /** Página pedida corregida: mínimo 1 y máximo la última que existe. */
    public static int normalizarPagina(int pedida, long totalRegistros, int tamano) {
        int ultima = paginasTotales(totalRegistros, tamano);
        return Math.max(1, Math.min(pedida, ultima));
    }

    /** Posición (OFFSET) del primer registro de la página; se calcula con long para no desbordar. */
    public static long desplazamiento(int paginaActual, int tamano) {
        return (long) (Math.max(1, paginaActual) - 1) * normalizarTamano(tamano);
    }

    public List<T> getItems() { return items; }
    public int getPaginaActual() { return paginaActual; }
    public int getTamano() { return tamano; }
    public long getTotalRegistros() { return totalRegistros; }
    public int getTotalPaginas() { return totalPaginas; }

    /** Número (desde 1) del primer registro mostrado; 0 si no hay registros. */
    public long getDesde() {
        return totalRegistros == 0 ? 0 : desplazamiento(paginaActual, tamano) + 1;
    }

    /** Número del último registro mostrado; 0 si no hay registros. */
    public long getHasta() {
        return totalRegistros == 0 ? 0 : desplazamiento(paginaActual, tamano) + items.size();
    }

    public boolean isHayAnterior() { return paginaActual > 1; }
    public boolean isHaySiguiente() { return paginaActual < totalPaginas; }
}

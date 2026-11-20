package pe.gob.munquinua.sigmud.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Pruebas de Pagina: cálculo de páginas totales y límites, corrección de la página y del tamaño (sin base de datos). */
class PaginaTest {

    @Test
    void calculaLasPaginasTotales() {
        assertEquals(1, Pagina.paginasTotales(0, 10), "sin registros hay 1 página (vacía)");
        assertEquals(1, Pagina.paginasTotales(1, 10));
        assertEquals(1, Pagina.paginasTotales(10, 10));
        assertEquals(2, Pagina.paginasTotales(11, 10));
        assertEquals(3, Pagina.paginasTotales(41, 20));
        assertEquals(2, Pagina.paginasTotales(100, 50));
        assertEquals(1, Pagina.paginasTotales(-7, 10), "un total negativo se trata como vacío");
    }

    @Test
    void elTamanoSoloPuedeSer10_20o50() {
        assertEquals(10, Pagina.normalizarTamano(10));
        assertEquals(20, Pagina.normalizarTamano(20));
        assertEquals(50, Pagina.normalizarTamano(50));
        for (int invalido : new int[] {0, -1, 5, 15, 100, 100000, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            assertEquals(10, Pagina.normalizarTamano(invalido), "tamaño " + invalido + " se normaliza a 10");
        }
    }

    @Test
    void laPaginaFueraDeRangoSeCorrige() {
        assertEquals(1, Pagina.normalizarPagina(-5, 100, 10), "mínimo 1");
        assertEquals(1, Pagina.normalizarPagina(0, 100, 10));
        assertEquals(3, Pagina.normalizarPagina(3, 100, 10));
        assertEquals(10, Pagina.normalizarPagina(999999, 100, 10), "máximo: la última que existe");
        assertEquals(1, Pagina.normalizarPagina(999999, 0, 10), "sin registros siempre la 1");
        assertEquals(10, Pagina.normalizarPagina(Integer.MAX_VALUE, 100, 10));
    }

    @Test
    void elConstructorNormalizaTodo() {
        Pagina<String> p = new Pagina<>(List.of("a", "b"), 999, 100000, 25);
        assertEquals(10, p.getTamano());
        assertEquals(3, p.getTotalPaginas());
        assertEquals(3, p.getPaginaActual());
        assertEquals(25, p.getTotalRegistros());
    }

    @Test
    void calculaElDesdeYElHastaParaMostrar() {
        List<Integer> cinco = List.of(1, 2, 3, 4, 5);
        Pagina<Integer> ultima = new Pagina<>(cinco, 3, 10, 25);
        assertEquals(21, ultima.getDesde());
        assertEquals(25, ultima.getHasta());
        Pagina<Integer> primera = new Pagina<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 1, 10, 25);
        assertEquals(1, primera.getDesde());
        assertEquals(10, primera.getHasta());
        Pagina<Integer> vacia = new Pagina<>(List.of(), 1, 10, 0);
        assertEquals(0, vacia.getDesde());
        assertEquals(0, vacia.getHasta());
    }

    @Test
    void sabeSiHayAnteriorYSiguiente() {
        Pagina<Integer> p = new Pagina<>(List.of(), 2, 10, 30);
        assertTrue(p.isHayAnterior());
        assertTrue(p.isHaySiguiente());
        assertFalse(new Pagina<>(List.of(), 1, 10, 30).isHayAnterior());
        assertFalse(new Pagina<>(List.of(), 3, 10, 30).isHaySiguiente());
        assertFalse(new Pagina<>(List.of(), 1, 10, 0).isHaySiguiente());
    }

    @Test
    void elDesplazamientoNoSeDesbordaConPaginasEnormes() {
        assertEquals(0, Pagina.desplazamiento(1, 10));
        assertEquals(20, Pagina.desplazamiento(3, 10));
        assertEquals(0, Pagina.desplazamiento(-4, 10), "página menor que 1 = desde el principio");
        assertEquals((long) (Integer.MAX_VALUE - 1) * 50, Pagina.desplazamiento(Integer.MAX_VALUE, 50), "se calcula con long");
        assertTrue(Pagina.desplazamiento(Integer.MAX_VALUE, 50) > 0);
    }

    @Test
    void laListaDeItemsNuncaEsNula() {
        assertEquals(0, new Pagina<String>(null, 1, 10, 0).getItems().size());
    }
}

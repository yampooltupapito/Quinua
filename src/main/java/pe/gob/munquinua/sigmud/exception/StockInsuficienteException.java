package pe.gob.munquinua.sigmud.exception;

/** Se intentó una SALIDA mayor al stock disponible. */
public class StockInsuficienteException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int stockDisponible;

    public StockInsuficienteException(int stockDisponible) {
        super("Stock insuficiente: solo hay " + stockDisponible + " unidades disponibles.");
        this.stockDisponible = stockDisponible;
    }

    public int getStockDisponible() {
        return stockDisponible;
    }
}

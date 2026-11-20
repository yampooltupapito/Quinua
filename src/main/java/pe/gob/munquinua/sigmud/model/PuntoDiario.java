package pe.gob.munquinua.sigmud.model;

import java.io.Serializable;
import java.time.LocalDate;

/** Entradas y salidas de stock de un día (un punto del gráfico de barras del panel). */
public class PuntoDiario implements Serializable {

    private static final long serialVersionUID = 1L;

    private final LocalDate fecha;
    private final long entradas;
    private final long salidas;

    public PuntoDiario(LocalDate fecha, long entradas, long salidas) {
        this.fecha = fecha;
        this.entradas = entradas;
        this.salidas = salidas;
    }

    public LocalDate getFecha() { return fecha; }
    public long getEntradas() { return entradas; }
    public long getSalidas() { return salidas; }
}

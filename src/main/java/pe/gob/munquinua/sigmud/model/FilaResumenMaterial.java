package pe.gob.munquinua.sigmud.model;

import java.io.Serializable;

/** Fila del resumen por material en un periodo: entradas, salidas, saldo y cantidad de movimientos. */
public class FilaResumenMaterial implements Serializable {

    private static final long serialVersionUID = 1L;

    private String codigo;
    private String nombre;
    private String unidad;
    private long totalEntradas;
    private long totalSalidas;
    private long saldo;                 // entradas - salidas del periodo
    private long cantidadMovimientos;
    private long stockActual;           // stock guardado en la tabla material
    private long saldoHistorico;        // entradas - salidas de TODOS los movimientos del material

    public FilaResumenMaterial() { }

    /** Verificación: el stock actual debe ser igual a entradas - salidas de todo el historial. */
    public boolean isConsistente() {
        return stockActual == saldoHistorico;
    }

    public long getStockActual() { return stockActual; }
    public void setStockActual(long stockActual) { this.stockActual = stockActual; }
    public long getSaldoHistorico() { return saldoHistorico; }
    public void setSaldoHistorico(long saldoHistorico) { this.saldoHistorico = saldoHistorico; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public long getTotalEntradas() { return totalEntradas; }
    public void setTotalEntradas(long totalEntradas) { this.totalEntradas = totalEntradas; }
    public long getTotalSalidas() { return totalSalidas; }
    public void setTotalSalidas(long totalSalidas) { this.totalSalidas = totalSalidas; }
    public long getSaldo() { return saldo; }
    public void setSaldo(long saldo) { this.saldo = saldo; }
    public long getCantidadMovimientos() { return cantidadMovimientos; }
    public void setCantidadMovimientos(long cantidadMovimientos) { this.cantidadMovimientos = cantidadMovimientos; }
}

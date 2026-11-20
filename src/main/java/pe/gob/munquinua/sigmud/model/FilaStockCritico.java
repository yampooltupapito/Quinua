package pe.gob.munquinua.sigmud.model;

import java.io.Serializable;

/** Fila del reporte de stock crítico: un material con stock menor o igual al mínimo. */
public class FilaStockCritico implements Serializable {

    private static final long serialVersionUID = 1L;

    private String codigo;
    private String nombre;
    private String unidad;
    private int stock;
    private int stockMinimo;
    private int faltante;      // stockMinimo - stock

    public FilaStockCritico() { }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }
    public int getFaltante() { return faltante; }
    public void setFaltante(int faltante) { this.faltante = faltante; }
}

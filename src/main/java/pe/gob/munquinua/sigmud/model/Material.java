package pe.gob.munquinua.sigmud.model;

import java.io.Serializable;

/** Material del almacén (tabla material). El stock solo cambia con movimientos, nunca a mano. */
public class Material implements Serializable {

    private static final long serialVersionUID = 1L;

    private int idMaterial;
    private String codigo;      // formato MAT-006
    private String nombre;
    private String unidad;
    private int stock;
    private int stockMinimo;
    private boolean estado;     // true = activo, false = dado de baja

    public Material() { }

    /** true cuando el stock llegó al mínimo o está por debajo. */
    public boolean esCritico() {
        return stock <= stockMinimo;
    }

    public int getIdMaterial() { return idMaterial; }
    public void setIdMaterial(int idMaterial) { this.idMaterial = idMaterial; }
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
    public boolean isEstado() { return estado; }
    public void setEstado(boolean estado) { this.estado = estado; }
}

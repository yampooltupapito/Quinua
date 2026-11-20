package pe.gob.munquinua.sigmud.model;

import java.io.Serializable;

/** Fila del reporte de salidas por área: unidades entregadas y número de salidas. */
public class FilaSalidaArea implements Serializable {

    private static final long serialVersionUID = 1L;

    private String area;
    private long totalUnidades;
    private long cantidadSalidas;

    public FilaSalidaArea() { }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public long getTotalUnidades() { return totalUnidades; }
    public void setTotalUnidades(long totalUnidades) { this.totalUnidades = totalUnidades; }
    public long getCantidadSalidas() { return cantidadSalidas; }
    public void setCantidadSalidas(long cantidadSalidas) { this.cantidadSalidas = cantidadSalidas; }
}

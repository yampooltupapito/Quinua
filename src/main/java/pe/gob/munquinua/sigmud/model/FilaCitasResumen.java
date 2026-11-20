package pe.gob.munquinua.sigmud.model;

import java.io.Serializable;

/** Fila del reporte de citas: por área o por funcionario (para funcionario, "area" trae su área). */
public class FilaCitasResumen implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombre;          // área o funcionario
    private String area;            // solo se usa en el cuadro por funcionario
    private long total;
    private long programadas;
    private long atendidas;
    private long canceladas;

    public FilaCitasResumen() { }

    /** Porcentaje de atención = atendidas / total de citas * 100 (0 si no hay citas). */
    public double getPorcentajeAtencion() {
        return total == 0 ? 0 : atendidas * 100.0 / total;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getProgramadas() { return programadas; }
    public void setProgramadas(long programadas) { this.programadas = programadas; }
    public long getAtendidas() { return atendidas; }
    public void setAtendidas(long atendidas) { this.atendidas = atendidas; }
    public long getCanceladas() { return canceladas; }
    public void setCanceladas(long canceladas) { this.canceladas = canceladas; }
}

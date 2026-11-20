package pe.gob.munquinua.sigmud.model;

import java.io.Serializable;

/** Conteo de citas de un contribuyente por estado (indicadores de la ficha). */
public class ResumenCitas implements Serializable {

    private static final long serialVersionUID = 1L;

    private long total;
    private long programadas;
    private long atendidas;
    private long canceladas;

    public ResumenCitas() { }

    public ResumenCitas(long total, long programadas, long atendidas, long canceladas) {
        this.total = total;
        this.programadas = programadas;
        this.atendidas = atendidas;
        this.canceladas = canceladas;
    }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getProgramadas() { return programadas; }
    public void setProgramadas(long programadas) { this.programadas = programadas; }
    public long getAtendidas() { return atendidas; }
    public void setAtendidas(long atendidas) { this.atendidas = atendidas; }
    public long getCanceladas() { return canceladas; }
    public void setCanceladas(long canceladas) { this.canceladas = canceladas; }
}

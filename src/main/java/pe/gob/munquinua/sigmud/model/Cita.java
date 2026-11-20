package pe.gob.munquinua.sigmud.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/** Cita de atención (tabla cita). Los campos "nombre..." y dniRuc solo sirven para mostrar (JOIN). */
public class Cita implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String PROGRAMADA = "PROGRAMADA";
    public static final String ATENDIDA = "ATENDIDA";
    public static final String CANCELADA = "CANCELADA";

    private int idCita;
    private int idContribuyente;
    private int idArea;
    private int idFuncionario;
    private int ventanilla;
    private LocalDate fecha;
    private LocalTime hora;
    private String motivo;
    private String estado = PROGRAMADA;
    // Solo para mostrar
    private String nombreContribuyente;
    private String dniRuc;
    private String nombreArea;
    private String nombreFuncionario;

    public Cita() { }

    public int getIdCita() { return idCita; }
    public void setIdCita(int idCita) { this.idCita = idCita; }
    public int getIdContribuyente() { return idContribuyente; }
    public void setIdContribuyente(int idContribuyente) { this.idContribuyente = idContribuyente; }
    public int getIdArea() { return idArea; }
    public void setIdArea(int idArea) { this.idArea = idArea; }
    public int getIdFuncionario() { return idFuncionario; }
    public void setIdFuncionario(int idFuncionario) { this.idFuncionario = idFuncionario; }
    public int getVentanilla() { return ventanilla; }
    public void setVentanilla(int ventanilla) { this.ventanilla = ventanilla; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    /** Fecha lista para mostrar (dd/MM/yyyy). */
    public String getFechaTexto() { return fecha == null ? "" : fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
    /** Hora lista para mostrar (HH:mm). */
    public String getHoraTexto() { return hora == null ? "" : hora.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")); }
    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getNombreContribuyente() { return nombreContribuyente; }
    public void setNombreContribuyente(String nombreContribuyente) { this.nombreContribuyente = nombreContribuyente; }
    public String getDniRuc() { return dniRuc; }
    public void setDniRuc(String dniRuc) { this.dniRuc = dniRuc; }
    public String getNombreArea() { return nombreArea; }
    public void setNombreArea(String nombreArea) { this.nombreArea = nombreArea; }
    public String getNombreFuncionario() { return nombreFuncionario; }
    public void setNombreFuncionario(String nombreFuncionario) { this.nombreFuncionario = nombreFuncionario; }
}

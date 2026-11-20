package pe.gob.munquinua.sigmud.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Contribuyente (tabla contribuyente). NATURAL usa DNI de 8 dígitos; JURIDICA usa RUC de 11. */
public class Contribuyente implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String NATURAL = "NATURAL";
    public static final String JURIDICA = "JURIDICA";

    private int idContribuyente;
    private String tipoPersona;
    private String dniRuc;
    private String nombresRazonSocial;
    private LocalDate fechaNacimiento;   // puede ser null
    private String telefono;
    private String email;
    private String direccion;
    private boolean estado;              // true = activo, false = dado de baja
    private LocalDateTime fechaRegistro;

    public Contribuyente() { }

    public int getIdContribuyente() { return idContribuyente; }
    public void setIdContribuyente(int idContribuyente) { this.idContribuyente = idContribuyente; }
    public String getTipoPersona() { return tipoPersona; }
    public void setTipoPersona(String tipoPersona) { this.tipoPersona = tipoPersona; }
    public String getDniRuc() { return dniRuc; }
    public void setDniRuc(String dniRuc) { this.dniRuc = dniRuc; }
    public String getNombresRazonSocial() { return nombresRazonSocial; }
    public void setNombresRazonSocial(String nombresRazonSocial) { this.nombresRazonSocial = nombresRazonSocial; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    /** Fecha de nacimiento lista para mostrar (dd/MM/yyyy); "" si no hay. */
    public String getFechaNacimientoTexto() {
        return fechaNacimiento == null ? "" : fechaNacimiento.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
    /** Fecha de registro lista para mostrar (dd/MM/yyyy HH:mm); "" si no hay. */
    public String getFechaRegistroTexto() {
        return fechaRegistro == null ? "" : fechaRegistro.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public boolean isEstado() { return estado; }
    public void setEstado(boolean estado) { this.estado = estado; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}

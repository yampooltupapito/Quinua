package pe.gob.munquinua.sigmud.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Movimiento de stock (tabla movimiento_stock) más los nombres para mostrar en pantalla. */
public class MovimientoStock implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ENTRADA = "ENTRADA";
    public static final String SALIDA = "SALIDA";
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private int idMovimiento;
    private int idMaterial;
    private String tipo;            // ENTRADA o SALIDA
    private int cantidad;
    private LocalDateTime fecha;
    private Integer idArea;         // solo en SALIDA; puede ser null
    private int idUsuario;
    private String observacion;
    // Datos de otras tablas (JOIN), solo para mostrar
    private String nombreMaterial;
    private String nombreArea;
    private String nombreUsuario;

    public MovimientoStock() { }

    /** Fecha lista para mostrar, por ejemplo 14/11/2026 13:03:18. */
    public String getFechaFormateada() {
        return fecha == null ? "" : fecha.format(FORMATO);
    }

    public int getIdMovimiento() { return idMovimiento; }
    public void setIdMovimiento(int idMovimiento) { this.idMovimiento = idMovimiento; }
    public int getIdMaterial() { return idMaterial; }
    public void setIdMaterial(int idMaterial) { this.idMaterial = idMaterial; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Integer getIdArea() { return idArea; }
    public void setIdArea(Integer idArea) { this.idArea = idArea; }
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getNombreMaterial() { return nombreMaterial; }
    public void setNombreMaterial(String nombreMaterial) { this.nombreMaterial = nombreMaterial; }
    public String getNombreArea() { return nombreArea; }
    public void setNombreArea(String nombreArea) { this.nombreArea = nombreArea; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
}

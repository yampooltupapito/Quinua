package pe.gob.munquinua.sigmud.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Un registro de la auditoría: quién (usuario), qué (acción, entidad y descripción), cuándo y desde dónde (IP). */
public class RegistroAuditoria implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private long idAuditoria;
    private LocalDateTime fechaHora;
    private Integer idUsuario;      // null si no había sesión
    private String username = "";
    private String accion = "";
    private String entidad = "";
    private Integer idEntidad;
    private String descripcion = "";
    private String detalle;         // cambios campo por campo (puede ser null)
    private String ip = "";

    public RegistroAuditoria() { }

    /** Fecha y hora listas para mostrar. */
    public String getFechaTexto() {
        return fechaHora == null ? "" : fechaHora.format(FORMATO);
    }

    /**
     * Clase de color (Bootstrap) de la insignia de la acción: verde = crear/activar, rojo = baja, bloqueo o fallo,
     * gris = sesión, azul = el resto (actualizaciones, movimientos, contraseñas).
     */
    public String getClaseInsignia() {
        if (accion.startsWith("LOGIN_OK") || accion.equals("LOGOUT")) {
            return "text-bg-secondary";
        }
        if (accion.contains("CREAR") || accion.contains("REACTIVAR") || accion.equals("USUARIO_ACTIVAR")) {
            return "text-bg-success";
        }
        if (accion.contains("BAJA") || accion.contains("CANCELAR") || accion.equals("USUARIO_DESACTIVAR")
                || accion.equals("CUENTA_BLOQUEADA") || accion.equals("LOGIN_FALLIDO")) {
            return "text-bg-danger";
        }
        return "text-bg-primary";
    }

    public long getIdAuditoria() { return idAuditoria; }
    public void setIdAuditoria(long idAuditoria) { this.idAuditoria = idAuditoria; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }
    public Integer getIdEntidad() { return idEntidad; }
    public void setIdEntidad(Integer idEntidad) { this.idEntidad = idEntidad; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
}

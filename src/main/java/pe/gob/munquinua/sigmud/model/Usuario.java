package pe.gob.munquinua.sigmud.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Usuario del sistema (tabla usuario).
 * passwordHash solo se llena al buscar en la BD para comparar el login;
 * se borra (null) antes de guardar el usuario en la sesión.
 */
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    private int idUsuario;
    private String username;
    private String passwordHash;
    private String nombres;
    private String rol;              // ADMIN, ATENCION o ALMACEN
    private Integer idArea;          // puede ser nulo
    private boolean estado;          // true = activo, false = bloqueado/inactivo
    private int intentosFallidos;
    private LocalDateTime ultimoAcceso;
    private boolean debeCambiarPassword;   // true = debe cambiar su contraseña antes de usar el sistema
    private String nombreArea;             // solo para mostrar (JOIN con area)

    public Usuario() { }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public Integer getIdArea() { return idArea; }
    public void setIdArea(Integer idArea) { this.idArea = idArea; }
    public boolean isEstado() { return estado; }
    public void setEstado(boolean estado) { this.estado = estado; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }
    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    public void setUltimoAcceso(LocalDateTime ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }
    public boolean isDebeCambiarPassword() { return debeCambiarPassword; }
    public void setDebeCambiarPassword(boolean debeCambiarPassword) { this.debeCambiarPassword = debeCambiarPassword; }
    public String getNombreArea() { return nombreArea; }
    public void setNombreArea(String nombreArea) { this.nombreArea = nombreArea; }

    /** true si la cuenta quedó bloqueada por intentos fallidos (5 o más). */
    public boolean isBloqueada() {
        return intentosFallidos >= 5;
    }

    /** Último acceso listo para mostrar (dd/MM/yyyy HH:mm) o "Nunca". */
    public String getUltimoAccesoTexto() {
        return ultimoAcceso == null ? "Nunca" : ultimoAcceso.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}

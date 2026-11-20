package pe.gob.munquinua.sigmud.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Filtros opcionales de la consulta de auditoría. Un filtro null o vacío significa "sin ese filtro". */
public class FiltrosAuditoria implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate desde;     // incluye todo el día
    private LocalDate hasta;     // incluye todo el día
    private LocalDateTime desdeMomento;   // desde un instante exacto (por ejemplo "las últimas 24 horas"); si está, reemplaza a "desde"
    private String username;     // contiene este texto
    private String accion;       // exacta
    private String entidad;      // exacta
    private Integer idEntidad;  // exacto (se usa junto con entidad)
    private String texto;       // contiene este texto en la descripción

    public FiltrosAuditoria() { }

    public LocalDate getDesde() { return desde; }
    public void setDesde(LocalDate desde) { this.desde = desde; }
    public LocalDate getHasta() { return hasta; }
    public LocalDateTime getDesdeMomento() { return desdeMomento; }
    public void setDesdeMomento(LocalDateTime desdeMomento) { this.desdeMomento = desdeMomento; }
    public void setHasta(LocalDate hasta) { this.hasta = hasta; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }
    /** Id de la entidad (con "entidad": por ejemplo CONTRIBUYENTE e id 5). null = cualquiera. */
    public Integer getIdEntidad() { return idEntidad; }
    public void setIdEntidad(Integer idEntidad) { this.idEntidad = idEntidad; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
}

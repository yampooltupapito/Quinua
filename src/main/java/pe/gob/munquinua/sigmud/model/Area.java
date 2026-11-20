package pe.gob.munquinua.sigmud.model;

import java.io.Serializable;

/** Área o unidad orgánica de la municipalidad (tabla area). */
public class Area implements Serializable {

    private static final long serialVersionUID = 1L;

    private int idArea;
    private String nombre;
    private boolean estado;

    public Area() { }

    public int getIdArea() { return idArea; }
    public void setIdArea(int idArea) { this.idArea = idArea; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public boolean isEstado() { return estado; }
    public void setEstado(boolean estado) { this.estado = estado; }
}

package pe.gob.munquinua.sigmud.model;

import java.io.Serializable;
import java.util.List;

/**
 * Reporte ya listo para mostrar: título, columnas, filas y filas de totales, todo como texto.
 * La misma tabla alimenta la pantalla (reportes.jsp) y el archivo CSV, así ambos son idénticos.
 */
public class TablaReporte implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String titulo;
    private final List<String> columnas;
    private final List<Boolean> numericas;          // true = columna numérica (se alinea a la derecha)
    private final List<List<String>> filas;
    private final List<List<String>> filasTotales;  // vacía si no hay datos

    public TablaReporte(String titulo, List<String> columnas, List<Boolean> numericas,
                        List<List<String>> filas, List<List<String>> filasTotales) {
        this.titulo = titulo;
        this.columnas = columnas;
        this.numericas = numericas;
        this.filas = filas;
        this.filasTotales = filasTotales;
    }

    public String getTitulo() { return titulo; }
    public List<String> getColumnas() { return columnas; }
    public List<Boolean> getNumericas() { return numericas; }
    public List<List<String>> getFilas() { return filas; }
    public List<List<String>> getFilasTotales() { return filasTotales; }
    public boolean isVacia() { return filas.isEmpty(); }
}

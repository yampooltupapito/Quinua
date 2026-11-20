package pe.gob.munquinua.sigmud.model;

import java.io.Serializable;

/** Un punto de un gráfico: una etiqueta (estado, mes, área...) y su valor. */
public class EtiquetaValor implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String etiqueta;
    private final long valor;

    public EtiquetaValor(String etiqueta, long valor) {
        this.etiqueta = etiqueta;
        this.valor = valor;
    }

    public String getEtiqueta() { return etiqueta; }
    public long getValor() { return valor; }
}

package pe.gob.munquinua.sigmud.util;

import java.io.PrintWriter;
import java.util.List;

/** Escritura de CSV para Excel en español: separador punto y coma, UTF-8 con BOM. */
public final class Csv {

    public static final String BOM_UTF8 = "﻿";   // para que Excel muestre bien tildes y ñ
    public static final String SEPARADOR = ";";

    private Csv() { }

    /** Escribe una línea. numericas indica qué columnas son números (no se les antepone apóstrofo). */
    public static void escribirLinea(PrintWriter w, List<String> celdas, List<Boolean> numericas) {
        StringBuilder linea = new StringBuilder();
        for (int i = 0; i < celdas.size(); i++) {
            if (i > 0) {
                linea.append(SEPARADOR);
            }
            boolean numerica = i < numericas.size() && numericas.get(i);
            linea.append(celda(celdas.get(i), numerica));
        }
        w.write(linea.append("\r\n").toString());
    }

    /**
     * Entre comillas si tiene ; " o saltos de línea; los textos que empiezan con = + - @
     * llevan un apóstrofo delante para que Excel no los ejecute como fórmula.
     */
    public static String celda(String valor, boolean numerica) {
        String v = valor == null ? "" : valor;
        if (!numerica && !v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(SEPARADOR) || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}

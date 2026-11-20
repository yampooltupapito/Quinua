package pe.gob.munquinua.sigmud.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import pe.gob.munquinua.sigmud.dao.ReporteCitasDAO;
import pe.gob.munquinua.sigmud.dao.impl.ReporteCitasDAOImpl;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.model.FilaCitasResumen;
import pe.gob.munquinua.sigmud.model.TablaReporte;
import pe.gob.munquinua.sigmud.util.Csv;
import pe.gob.munquinua.sigmud.util.FiltrosReporte;

/**
 * Reporte de citas por área y por funcionario en un rango de fechas.
 * GET muestra la página; GET con accion=csv descarga los dos cuadros como CSV.
 * El acceso (ADMIN y ATENCION) lo controla AuthFilter para toda la ruta /citas/*.
 */
@WebServlet("/citas/reportes")
public class ReporteCitasServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(ReporteCitasServlet.class.getName());
    private static final String VISTA = "/WEB-INF/views/citas/reportes.jsp";
    private static final String MSG_GENERICO = "No se pudo generar el reporte. Intente nuevamente.";
    private static final Locale LOCALE = Locale.forLanguageTag("es-PE");

    /** Columnas por las que se puede ordenar (clave -> etiqueta y Comparator). */
    private static final Map<String, String> ETIQUETAS = new LinkedHashMap<>();
    private static final Map<String, Comparator<FilaCitasResumen>> ORDEN = new LinkedHashMap<>();

    static {
        ETIQUETAS.put("nombre", "Nombre");
        ETIQUETAS.put("total", "Total de citas");
        ETIQUETAS.put("programadas", "Programadas");
        ETIQUETAS.put("atendidas", "Atendidas");
        ETIQUETAS.put("canceladas", "Canceladas");
        ETIQUETAS.put("porcentaje", "% de atención");
        ORDEN.put("nombre", Comparator.comparing(FilaCitasResumen::getNombre, String.CASE_INSENSITIVE_ORDER));
        ORDEN.put("total", Comparator.comparingLong(FilaCitasResumen::getTotal));
        ORDEN.put("programadas", Comparator.comparingLong(FilaCitasResumen::getProgramadas));
        ORDEN.put("atendidas", Comparator.comparingLong(FilaCitasResumen::getAtendidas));
        ORDEN.put("canceladas", Comparator.comparingLong(FilaCitasResumen::getCanceladas));
        ORDEN.put("porcentaje", Comparator.comparingDouble(FilaCitasResumen::getPorcentajeAtencion));
    }

    private final ReporteCitasDAO dao = new ReporteCitasDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Se reutiliza FiltrosReporte forzando el reporte "resumen": valida desde/hasta (máx. 366 días) y orden/dir.
        FiltrosReporte f = new FiltrosReporte(k -> "reporte".equals(k) ? "resumen" : req.getParameter(k));
        TablaReporte porArea = null;
        TablaReporte porFuncionario = null;
        if (f.getErrores().isEmpty()) {
            try {
                porArea = tabla("Citas por área", ordenar(dao.citasPorArea(f.getDesde(), f.getHasta()), f), false);
                porFuncionario = tabla("Citas por funcionario",
                        ordenar(dao.citasPorFuncionario(f.getDesde(), f.getHasta()), f), true);
            } catch (DaoException e) {
                LOG.log(Level.SEVERE, e.getMessage(), e); // detalle solo al log
                f.agregarError(MSG_GENERICO);
                porArea = null;
                porFuncionario = null;
            }
        }
        if (porArea != null && "csv".equals(req.getParameter("accion"))) {
            enviarCsv(resp, porArea, porFuncionario);
            return;
        }
        req.setAttribute("f", f);
        req.setAttribute("porArea", porArea);
        req.setAttribute("porFuncionario", porFuncionario);
        req.setAttribute("opcionesOrden", ETIQUETAS);
        req.getRequestDispatcher(VISTA).forward(req, resp);
    }

    /** Ordena con el Comparator elegido (inverso si dir=desc). Sin orden válido, no cambia el orden. */
    private List<FilaCitasResumen> ordenar(List<FilaCitasResumen> datos, FiltrosReporte f) {
        Comparator<FilaCitasResumen> c = ORDEN.get(f.getOrden());
        if (c == null) {
            return datos;
        }
        return datos.stream().sorted("desc".equals(f.getDir()) ? c.reversed() : c).toList();
    }

    /** Arma el cuadro (filas + fila de totales con Stream). conArea agrega la columna del área del funcionario. */
    private TablaReporte tabla(String titulo, List<FilaCitasResumen> datos, boolean conArea) {
        List<List<String>> filas = new ArrayList<>();
        for (FilaCitasResumen x : datos) {
            filas.add(fila(x.getNombre(), x.getArea(), x.getTotal(), x.getProgramadas(), x.getAtendidas(),
                    x.getCanceladas(), x.getPorcentajeAtencion(), conArea));
        }
        long total = datos.stream().mapToLong(FilaCitasResumen::getTotal).sum();
        long programadas = datos.stream().mapToLong(FilaCitasResumen::getProgramadas).sum();
        long atendidas = datos.stream().mapToLong(FilaCitasResumen::getAtendidas).sum();
        long canceladas = datos.stream().mapToLong(FilaCitasResumen::getCanceladas).sum();
        double porcentaje = total == 0 ? 0 : atendidas * 100.0 / total;
        List<List<String>> totales = datos.isEmpty() ? List.of() : List.of(
                fila("TOTAL", "", total, programadas, atendidas, canceladas, porcentaje, conArea));
        List<String> columnas = new ArrayList<>(List.of(conArea ? "Funcionario" : "Área"));
        List<Boolean> numericas = new ArrayList<>(List.of(false));
        if (conArea) {
            columnas.add("Área");
            numericas.add(false);
        }
        columnas.addAll(List.of("Total de citas", "Programadas", "Atendidas", "Canceladas", "% de atención"));
        numericas.addAll(List.of(true, true, true, true, true));
        return new TablaReporte(titulo, columnas, numericas, filas, totales);
    }

    private List<String> fila(String nombre, String area, long total, long programadas, long atendidas,
                              long canceladas, double porcentaje, boolean conArea) {
        List<String> celdas = new ArrayList<>();
        celdas.add(nombre);
        if (conArea) {
            celdas.add(area == null ? "" : area);
        }
        celdas.add(String.valueOf(total));
        celdas.add(String.valueOf(programadas));
        celdas.add(String.valueOf(atendidas));
        celdas.add(String.valueOf(canceladas));
        celdas.add(String.format(LOCALE, "%.1f%%", porcentaje));
        return celdas;
    }

    // ---------- CSV ----------

    private void enviarCsv(HttpServletResponse resp, TablaReporte porArea, TablaReporte porFuncionario)
            throws IOException {
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"reporte-citas-" + LocalDate.now() + ".csv\"");
        PrintWriter w = resp.getWriter();
        w.write(Csv.BOM_UTF8);
        escribirCuadro(w, porArea);
        Csv.escribirLinea(w, List.of(""), List.of(false));   // línea en blanco entre los dos cuadros
        escribirCuadro(w, porFuncionario);
        w.flush();
    }

    private void escribirCuadro(PrintWriter w, TablaReporte t) {
        Csv.escribirLinea(w, List.of(t.getTitulo()), List.of(false));
        Csv.escribirLinea(w, t.getColumnas(), t.getNumericas());
        for (List<String> fila : t.getFilas()) {
            Csv.escribirLinea(w, fila, t.getNumericas());
        }
        for (List<String> fila : t.getFilasTotales()) {
            Csv.escribirLinea(w, fila, t.getNumericas());
        }
    }
}

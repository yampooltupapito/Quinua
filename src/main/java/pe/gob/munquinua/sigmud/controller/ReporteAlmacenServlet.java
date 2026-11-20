package pe.gob.munquinua.sigmud.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import pe.gob.munquinua.sigmud.dao.AreaDAO;
import pe.gob.munquinua.sigmud.dao.MaterialDAO;
import pe.gob.munquinua.sigmud.dao.ReporteAlmacenDAO;
import pe.gob.munquinua.sigmud.dao.impl.AreaDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.MaterialDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.ReporteAlmacenDAOImpl;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.model.FilaResumenMaterial;
import pe.gob.munquinua.sigmud.model.FilaSalidaArea;
import pe.gob.munquinua.sigmud.model.FilaStockCritico;
import pe.gob.munquinua.sigmud.model.MovimientoStock;
import pe.gob.munquinua.sigmud.model.TablaReporte;
import pe.gob.munquinua.sigmud.util.Csv;
import pe.gob.munquinua.sigmud.util.FiltrosReporte;

/**
 * Reportes del almacén: stock crítico, movimientos, resumen por material y salidas por área.
 * GET muestra la página; GET con accion=csv descarga el MISMO reporte como CSV.
 * El acceso (ADMIN y ALMACEN) lo controla AuthFilter para toda la ruta /almacen/*.
 */
@WebServlet("/almacen/reportes")
public class ReporteAlmacenServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(ReporteAlmacenServlet.class.getName());
    private static final String VISTA = "/WEB-INF/views/almacen/reportes.jsp";
    private static final String MSG_GENERICO = "No se pudo generar el reporte. Intente nuevamente.";

    /** Un criterio de orden: la etiqueta que ve el usuario y el Comparator que se aplica. */
    private record Criterio<T>(String etiqueta, Comparator<T> comparador) { }

    private static final Map<String, Criterio<FilaStockCritico>> ORDEN_STOCK = new LinkedHashMap<>();
    private static final Map<String, Criterio<MovimientoStock>> ORDEN_MOVIMIENTOS = new LinkedHashMap<>();
    private static final Map<String, Criterio<FilaResumenMaterial>> ORDEN_RESUMEN = new LinkedHashMap<>();
    private static final Map<String, Criterio<FilaSalidaArea>> ORDEN_AREAS = new LinkedHashMap<>();

    static {
        Comparator<String> texto = String.CASE_INSENSITIVE_ORDER;
        ORDEN_STOCK.put("codigo", new Criterio<>("Código", Comparator.comparing(FilaStockCritico::getCodigo, texto)));
        ORDEN_STOCK.put("nombre", new Criterio<>("Nombre", Comparator.comparing(FilaStockCritico::getNombre, texto)));
        ORDEN_STOCK.put("stock", new Criterio<>("Stock", Comparator.comparingInt(FilaStockCritico::getStock)));
        ORDEN_STOCK.put("faltante", new Criterio<>("Faltante", Comparator.comparingInt(FilaStockCritico::getFaltante)));

        ORDEN_MOVIMIENTOS.put("fecha", new Criterio<>("Fecha", Comparator.comparing(MovimientoStock::getFecha,
                Comparator.nullsLast(Comparator.naturalOrder()))));
        ORDEN_MOVIMIENTOS.put("tipo", new Criterio<>("Tipo", Comparator.comparing(MovimientoStock::getTipo, texto)));
        ORDEN_MOVIMIENTOS.put("material", new Criterio<>("Material",
                Comparator.comparing(MovimientoStock::getNombreMaterial, texto)));
        ORDEN_MOVIMIENTOS.put("cantidad", new Criterio<>("Cantidad", Comparator.comparingInt(MovimientoStock::getCantidad)));
        ORDEN_MOVIMIENTOS.put("area", new Criterio<>("Área", Comparator.comparing(MovimientoStock::getNombreArea,
                Comparator.nullsLast(texto))));
        ORDEN_MOVIMIENTOS.put("usuario", new Criterio<>("Usuario",
                Comparator.comparing(MovimientoStock::getNombreUsuario, texto)));

        ORDEN_RESUMEN.put("codigo", new Criterio<>("Código", Comparator.comparing(FilaResumenMaterial::getCodigo, texto)));
        ORDEN_RESUMEN.put("nombre", new Criterio<>("Material", Comparator.comparing(FilaResumenMaterial::getNombre, texto)));
        ORDEN_RESUMEN.put("entradas", new Criterio<>("Entradas", Comparator.comparingLong(FilaResumenMaterial::getTotalEntradas)));
        ORDEN_RESUMEN.put("salidas", new Criterio<>("Salidas", Comparator.comparingLong(FilaResumenMaterial::getTotalSalidas)));
        ORDEN_RESUMEN.put("saldo", new Criterio<>("Saldo", Comparator.comparingLong(FilaResumenMaterial::getSaldo)));
        ORDEN_RESUMEN.put("movimientos", new Criterio<>("Movimientos",
                Comparator.comparingLong(FilaResumenMaterial::getCantidadMovimientos)));

        ORDEN_AREAS.put("area", new Criterio<>("Área", Comparator.comparing(FilaSalidaArea::getArea, texto)));
        ORDEN_AREAS.put("unidades", new Criterio<>("Unidades entregadas", Comparator.comparingLong(FilaSalidaArea::getTotalUnidades)));
        ORDEN_AREAS.put("salidas", new Criterio<>("N.º de salidas", Comparator.comparingLong(FilaSalidaArea::getCantidadSalidas)));
    }

    private final ReporteAlmacenDAO reporteDao = new ReporteAlmacenDAOImpl();
    private final AreaDAO areaDao = new AreaDAOImpl();
    private final MaterialDAO materialDao = new MaterialDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        FiltrosReporte f = new FiltrosReporte(req::getParameter);
        TablaReporte tabla = null;
        if (f.getErrores().isEmpty()) {
            try {
                tabla = construir(f);
                cargarListas(req, f);
            } catch (DaoException e) {
                LOG.log(Level.SEVERE, e.getMessage(), e); // detalle solo al log
                f.agregarError(MSG_GENERICO);
                tabla = null;
            }
        }
        if (tabla != null && "csv".equals(req.getParameter("accion"))) {
            enviarCsv(resp, f, tabla);
            return;
        }
        req.setAttribute("f", f);
        req.setAttribute("tabla", tabla);
        req.setAttribute("opcionesOrden", opcionesOrden());
        req.getRequestDispatcher(VISTA).forward(req, resp);
    }

    /** Listas para los filtros de la página (solo el reporte de movimientos las usa). */
    private void cargarListas(HttpServletRequest req, FiltrosReporte f) {
        if ("movimientos".equals(f.getReporte())) {
            req.setAttribute("areas", areaDao.listarActivas());
            req.setAttribute("materiales", materialDao.listar());
        }
    }

    // ---------- armado de cada reporte ----------

    private TablaReporte construir(FiltrosReporte f) {
        return switch (f.getReporte()) {
            case "movimientos" -> tablaMovimientos(f);
            case "resumen" -> tablaResumen(f);
            case "areas" -> tablaAreas(f);
            default -> tablaStock(f);
        };
    }

    private TablaReporte tablaStock(FiltrosReporte f) {
        List<FilaStockCritico> datos = ordenar(reporteDao.stockCritico(), ORDEN_STOCK, f);
        List<List<String>> filas = datos.stream().map(x -> List.of(x.getCodigo(), x.getNombre(), x.getUnidad(),
                String.valueOf(x.getStock()), String.valueOf(x.getStockMinimo()),
                String.valueOf(x.getFaltante()))).toList();
        // Totales en memoria con Stream
        int totalStock = datos.stream().mapToInt(FilaStockCritico::getStock).sum();
        int totalMinimo = datos.stream().mapToInt(FilaStockCritico::getStockMinimo).sum();
        int totalFaltante = datos.stream().mapToInt(FilaStockCritico::getFaltante).sum();
        List<List<String>> totales = datos.isEmpty() ? List.of() : List.of(List.of(
                "TOTAL (" + datos.size() + " materiales)", "", "",
                String.valueOf(totalStock), String.valueOf(totalMinimo), String.valueOf(totalFaltante)));
        return new TablaReporte("Stock crítico",
                List.of("Código", "Nombre", "Unidad", "Stock", "Stock mínimo", "Faltante"),
                List.of(false, false, false, true, true, true), filas, totales);
    }

    private TablaReporte tablaMovimientos(FiltrosReporte f) {
        List<MovimientoStock> datos = ordenar(reporteDao.movimientosPorFecha(
                f.getDesde(), f.getHasta(), f.getTipo(), f.getIdArea(), f.getIdMaterial()), ORDEN_MOVIMIENTOS, f);
        List<List<String>> filas = datos.stream().map(m -> List.of(m.getFechaFormateada(), m.getTipo(),
                m.getNombreMaterial(), String.valueOf(m.getCantidad()), nz(m.getNombreArea()),
                m.getNombreUsuario(), nz(m.getObservacion()))).toList();
        // Suma de cantidades agrupada por tipo (ENTRADA / SALIDA) con Collectors.groupingBy
        Map<String, Integer> porTipo = datos.stream().collect(Collectors.groupingBy(
                MovimientoStock::getTipo, Collectors.summingInt(MovimientoStock::getCantidad)));
        int entradas = porTipo.getOrDefault(MovimientoStock.ENTRADA, 0);
        int salidas = porTipo.getOrDefault(MovimientoStock.SALIDA, 0);
        List<List<String>> totales = datos.isEmpty() ? List.of() : List.of(
                filaTotal("TOTAL ENTRADAS (" + datos.stream().filter(m -> MovimientoStock.ENTRADA.equals(m.getTipo())).count() + ")", 3, 7, entradas),
                filaTotal("TOTAL SALIDAS (" + datos.stream().filter(m -> MovimientoStock.SALIDA.equals(m.getTipo())).count() + ")", 3, 7, salidas),
                filaTotal("SALDO DEL PERIODO", 3, 7, entradas - salidas));
        return new TablaReporte("Movimientos por fecha",
                List.of("Fecha y hora", "Tipo", "Material", "Cantidad", "Área", "Usuario", "Observación"),
                List.of(false, false, false, true, false, false, false), filas, totales);
    }

    private TablaReporte tablaResumen(FiltrosReporte f) {
        List<FilaResumenMaterial> datos = ordenar(
                reporteDao.resumenPorMaterial(f.getDesde(), f.getHasta()), ORDEN_RESUMEN, f);
        List<List<String>> filas = datos.stream().map(x -> List.of(x.getCodigo(), x.getNombre(), x.getUnidad(),
                String.valueOf(x.getTotalEntradas()), String.valueOf(x.getTotalSalidas()),
                String.valueOf(x.getSaldo()), String.valueOf(x.getCantidadMovimientos()),
                String.valueOf(x.getStockActual()), x.isConsistente() ? "OK" : "REVISAR")).toList();
        long entradas = datos.stream().mapToLong(FilaResumenMaterial::getTotalEntradas).sum();
        long salidas = datos.stream().mapToLong(FilaResumenMaterial::getTotalSalidas).sum();
        long saldo = datos.stream().mapToLong(FilaResumenMaterial::getSaldo).sum();
        long movimientos = datos.stream().mapToLong(FilaResumenMaterial::getCantidadMovimientos).sum();
        long stockActual = datos.stream().mapToLong(FilaResumenMaterial::getStockActual).sum();
        long conDiferencia = datos.stream().filter(x -> !x.isConsistente()).count();
        List<List<String>> totales = datos.isEmpty() ? List.of() : List.of(List.of("TOTAL", "", "",
                String.valueOf(entradas), String.valueOf(salidas), String.valueOf(saldo), String.valueOf(movimientos),
                String.valueOf(stockActual), conDiferencia == 0 ? "Todo OK" : conDiferencia + " con diferencia"));
        return new TablaReporte("Resumen por material",
                List.of("Código", "Material", "Unidad", "Entradas", "Salidas", "Saldo del periodo", "Movimientos",
                        "Stock actual", "Verificación"),
                List.of(false, false, false, true, true, true, true, true, false), filas, totales);
    }

    private TablaReporte tablaAreas(FiltrosReporte f) {
        List<FilaSalidaArea> datos = ordenar(
                reporteDao.salidasPorArea(f.getDesde(), f.getHasta()), ORDEN_AREAS, f);
        long unidades = datos.stream().mapToLong(FilaSalidaArea::getTotalUnidades).sum();
        long salidas = datos.stream().mapToLong(FilaSalidaArea::getCantidadSalidas).sum();
        // Las 3 primeras columnas no cambian; "% del total" y "Verificación" se agregan al final.
        List<List<String>> filas = datos.stream().map(x -> List.of(x.getArea(),
                String.valueOf(x.getTotalUnidades()), String.valueOf(x.getCantidadSalidas()),
                porcentaje(x.getTotalUnidades(), unidades), "")).toList();
        List<List<String>> totales = datos.isEmpty() ? List.of() : List.of(List.of("TOTAL",
                String.valueOf(unidades), String.valueOf(salidas), porcentaje(unidades, unidades),
                verificarSalidas(f, unidades)));
        return new TablaReporte("Salidas por área",
                List.of("Área", "Unidades entregadas", "N.º de salidas", "% del total", "Verificación"),
                List.of(false, true, true, true, false), filas, totales);
    }

    /**
     * Verificación: el total entregado a las áreas debe ser igual a la suma de todas las SALIDAS
     * del periodo según el detalle de movimientos (consulta independiente).
     */
    private String verificarSalidas(FiltrosReporte f, long totalAreas) {
        long totalMovimientos = reporteDao.movimientosPorFecha(f.getDesde(), f.getHasta(), "SALIDA", null, null)
                .stream().mapToLong(MovimientoStock::getCantidad).sum();
        return totalMovimientos == totalAreas ? "OK" : "REVISAR (movimientos: " + totalMovimientos + ")";
    }

    private String porcentaje(long parte, long total) {
        double p = total == 0 ? 0 : parte * 100.0 / total;
        return String.format(Locale.forLanguageTag("es-PE"), "%.1f%%", p);
    }

    // ---------- orden ----------

    /** Ordena con el Comparator elegido (y su inverso si dir=desc). Sin orden válido, no cambia nada. */
    private <T> List<T> ordenar(List<T> lista, Map<String, Criterio<T>> criterios, FiltrosReporte f) {
        Criterio<T> c = criterios.get(f.getOrden());
        if (c == null) {
            return lista;
        }
        Comparator<T> cmp = "desc".equals(f.getDir()) ? c.comparador().reversed() : c.comparador();
        return lista.stream().sorted(cmp).toList();
    }

    /**
     * Opciones del desplegable "Ordenar por" de cada reporte: reporte -> (clave -> etiqueta).
     * La página las pinta todas y reportes.js muestra solo las del reporte elegido.
     */
    private Map<String, Map<String, String>> opcionesOrden() {
        Map<String, Map<String, String>> todas = new LinkedHashMap<>();
        todas.put("stock", etiquetas(ORDEN_STOCK));
        todas.put("movimientos", etiquetas(ORDEN_MOVIMIENTOS));
        todas.put("resumen", etiquetas(ORDEN_RESUMEN));
        todas.put("areas", etiquetas(ORDEN_AREAS));
        return todas;
    }

    private <T> Map<String, String> etiquetas(Map<String, Criterio<T>> criterios) {
        Map<String, String> m = new LinkedHashMap<>();
        criterios.forEach((clave, criterio) -> m.put(clave, criterio.etiqueta()));
        return m;
    }

    // ---------- CSV ----------

    private void enviarCsv(HttpServletResponse resp, FiltrosReporte f, TablaReporte tabla) throws IOException {
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"reporte-almacen-"
                + f.getReporte() + "-" + LocalDate.now() + ".csv\"");
        PrintWriter w = resp.getWriter();
        w.write(Csv.BOM_UTF8);
        Csv.escribirLinea(w, tabla.getColumnas(), tabla.getNumericas());
        for (List<String> fila : tabla.getFilas()) {
            Csv.escribirLinea(w, fila, tabla.getNumericas());
        }
        for (List<String> fila : tabla.getFilasTotales()) {
            Csv.escribirLinea(w, fila, tabla.getNumericas());
        }
        w.flush();
    }

    // ---------- auxiliares ----------

    /** Fila de totales: texto en la primera columna y el número en la columna indicada. */
    private List<String> filaTotal(String etiqueta, int columnaNumero, int totalColumnas, long valor) {
        String[] celdas = new String[totalColumnas];
        java.util.Arrays.fill(celdas, "");
        celdas[0] = etiqueta;
        celdas[columnaNumero] = String.valueOf(valor);
        return List.of(celdas);
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}

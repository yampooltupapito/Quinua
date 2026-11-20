package pe.gob.munquinua.sigmud.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import pe.gob.munquinua.sigmud.dao.AuditoriaDAO;
import pe.gob.munquinua.sigmud.dao.impl.AuditoriaDAOImpl;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.model.FiltrosAuditoria;
import pe.gob.munquinua.sigmud.model.RegistroAuditoria;
import pe.gob.munquinua.sigmud.util.Auditoria;
import pe.gob.munquinua.sigmud.util.Csv;

/**
 * Consulta de la auditoría (solo ADMIN: AuthFilter lo exige por ruta). Solo LEE: no existe ninguna opción para
 * editar ni eliminar registros (y la base de datos lo impide con triggers).
 * GET: filtros (fechas, usuario, acción, entidad, texto), paginación de 20 y detalle en un modal.
 * GET con formato=csv: exporta el mismo resultado (hasta 10000 filas) como CSV.
 */
@WebServlet("/auditoria")
public class AuditoriaServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(AuditoriaServlet.class.getName());
    private static final String VISTA = "/WEB-INF/views/auditoria/listado.jsp";
    private static final int TAMANO_PAGINA = 20;
    private static final int MAX_CSV = 10_000;
    private static final int DIAS_POR_DEFECTO = 7;
    private static final int MAX_DIAS = 366;
    private static final String MSG_GENERICO = "No se pudo consultar la auditoría. Intente nuevamente.";

    private final AuditoriaDAO dao = new AuditoriaDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<String> errores = new ArrayList<>();
        FiltrosAuditoria f = leerFiltros(req, errores);
        boolean csv = "csv".equals(req.getParameter("formato"));
        List<RegistroAuditoria> lista = List.of();
        long total = 0;
        int pagina = leerPagina(req.getParameter("pagina"));
        if (errores.isEmpty()) {
            try {
                total = dao.contar(f);
                pagina = Math.min(pagina, (int) Math.max(1, (total + TAMANO_PAGINA - 1) / TAMANO_PAGINA));
                if (csv) {
                    enviarCsv(resp, dao.buscar(f, 1, MAX_CSV));
                    return;
                }
                lista = dao.buscar(f, pagina, TAMANO_PAGINA);
            } catch (DaoException e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);   // detalle solo al log
                errores.add(MSG_GENERICO);
            }
        }
        mostrar(req, resp, f, errores, lista, total, pagina);
    }

    // ---------- filtros ----------

    /** Lee y valida los filtros. Por defecto: últimos 7 días; el periodo máximo es de 366 días. */
    private FiltrosAuditoria leerFiltros(HttpServletRequest req, List<String> errores) {
        FiltrosAuditoria f = new FiltrosAuditoria();
        LocalDate hoy = LocalDate.now();
        f.setDesde(fecha(req.getParameter("desde"), hoy.minusDays(DIAS_POR_DEFECTO - 1L), "desde", errores));
        f.setHasta(fecha(req.getParameter("hasta"), hoy, "hasta", errores));
        if (f.getDesde() != null && f.getHasta() != null) {
            if (f.getDesde().isAfter(f.getHasta())) {
                errores.add("La fecha \"desde\" no puede ser mayor que la fecha \"hasta\".");
            } else if (ChronoUnit.DAYS.between(f.getDesde(), f.getHasta()) + 1 > MAX_DIAS) {
                errores.add("El periodo no puede pasar de " + MAX_DIAS + " días.");
            }
        }
        f.setUsername(texto(req.getParameter("usuario"), 40));
        String accion = texto(req.getParameter("accion"), 30);
        f.setAccion(Auditoria.ACCIONES.contains(accion) ? accion : "");
        String entidad = texto(req.getParameter("entidad"), 30);
        f.setEntidad(Auditoria.ENTIDADES.contains(entidad) ? entidad : "");
        f.setTexto(texto(req.getParameter("q"), 100));
        return f;
    }

    private LocalDate fecha(String texto, LocalDate porDefecto, String nombre, List<String> errores) {
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty()) {
            return porDefecto;
        }
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            errores.add("La fecha \"" + nombre + "\" no tiene un formato válido.");
            return null;
        }
    }

    private String texto(String s, int maximo) {
        String t = s == null ? "" : s.trim();
        return t.length() > maximo ? t.substring(0, maximo) : t;
    }

    private int leerPagina(String s) {
        try {
            return Math.max(1, Integer.parseInt(s == null ? "1" : s.trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    // ---------- pantalla ----------

    private void mostrar(HttpServletRequest req, HttpServletResponse resp, FiltrosAuditoria f, List<String> errores,
                         List<RegistroAuditoria> lista, long total, int pagina) throws ServletException, IOException {
        LocalDate hoy = LocalDate.now();
        req.setAttribute("desdeTexto", f.getDesde() != null ? f.getDesde().toString()
                : texto(req.getParameter("desde"), 10));
        req.setAttribute("hastaTexto", f.getHasta() != null ? f.getHasta().toString()
                : texto(req.getParameter("hasta"), 10));
        req.setAttribute("f", f);
        req.setAttribute("errores", errores);
        req.setAttribute("lista", lista);
        req.setAttribute("total", total);
        req.setAttribute("pagina", pagina);
        req.setAttribute("paginas", Math.max(1, (total + TAMANO_PAGINA - 1) / TAMANO_PAGINA));
        req.setAttribute("tamanoPagina", TAMANO_PAGINA);
        req.setAttribute("acciones", Auditoria.ACCIONES);
        req.setAttribute("entidades", Auditoria.ENTIDADES);
        req.setAttribute("hoy", hoy.toString());
        req.getRequestDispatcher(VISTA).forward(req, resp);
    }

    // ---------- CSV ----------

    private void enviarCsv(HttpServletResponse resp, List<RegistroAuditoria> lista) throws IOException {
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"auditoria-" + LocalDate.now() + ".csv\"");
        PrintWriter w = resp.getWriter();
        w.write(Csv.BOM_UTF8);
        List<Boolean> numericas = List.of(false, false, false, false, true, false, false, false);
        Csv.escribirLinea(w, List.of("Fecha y hora", "Usuario", "Acción", "Entidad", "ID entidad", "Descripción",
                "Detalle", "IP"), numericas);
        for (RegistroAuditoria r : lista) {
            Csv.escribirLinea(w, List.of(r.getFechaTexto(), r.getUsername(), r.getAccion(), r.getEntidad(),
                    r.getIdEntidad() == null ? "" : String.valueOf(r.getIdEntidad()), r.getDescripcion(),
                    r.getDetalle() == null ? "" : r.getDetalle(), r.getIp()), numericas);
        }
        w.flush();
    }
}

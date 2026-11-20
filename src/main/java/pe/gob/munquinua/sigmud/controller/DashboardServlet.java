package pe.gob.munquinua.sigmud.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.filter.AuthFilter;
import pe.gob.munquinua.sigmud.model.Usuario;
import pe.gob.munquinua.sigmud.util.PanelDatos;

/**
 * Página principal (/menu): el panel (dashboard) del rol de la SESIÓN. Muestra las tarjetas de acceso a los
 * módulos permitidos y los indicadores y listas del rol; los gráficos los dibuja dashboard.js con /dashboard/datos.
 * Reemplaza al antiguo MenuServlet.
 */
@WebServlet("/menu")
public class DashboardServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(DashboardServlet.class.getName());

    private final PanelDatos panelDatos = new PanelDatos();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Usuario u = (Usuario) req.getSession().getAttribute(LoginServlet.SESION_USUARIO);   // el rol sale SOLO de la sesión
        String vista = switch (u.getRol()) {
            case "ADMIN" -> "admin";
            case "ATENCION" -> "atencion";
            case "ALMACEN" -> "almacen";
            default -> null;
        };
        if (vista == null) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        // Tarjetas de acceso a los módulos (la misma lógica de roles que ya tenía el menú)
        req.setAttribute("verContribuyentes", AuthFilter.rolPermite("/contribuyentes", u.getRol()));
        req.setAttribute("verAlmacen", AuthFilter.rolPermite("/almacen", u.getRol()));
        req.setAttribute("verCitas", AuthFilter.rolPermite("/citas", u.getRol()));
        req.setAttribute("verUsuarios", AuthFilter.rolPermite("/usuarios", u.getRol()));
        req.setAttribute("verAuditoria", AuthFilter.rolPermite("/auditoria", u.getRol()));
        cargarPanel(req, u);
        req.getRequestDispatcher("/WEB-INF/views/dashboard/" + vista + ".jsp").forward(req, resp);
    }

    /** Si la base falla, la página igual se muestra (con un aviso) y dashboard.js reintenta cada 60 s. */
    private void cargarPanel(HttpServletRequest req, Usuario u) {
        try {
            req.setAttribute("panel", panelDatos.construir(u.getRol(), u.getIdUsuario()));
        } catch (DaoException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            req.setAttribute("avisoPanel", "No se pudieron cargar los indicadores por ahora.");
        }
    }
}

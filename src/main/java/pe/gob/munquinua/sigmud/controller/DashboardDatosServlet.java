package pe.gob.munquinua.sigmud.controller;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.model.Usuario;
import pe.gob.munquinua.sigmud.util.PanelDatos;

/**
 * Datos de los gráficos e indicadores del panel en JSON (GET /dashboard/datos).
 * El rol y el id del usuario salen SIEMPRE de la sesión: ningún parámetro de la petición se lee,
 * así un ATENCION no puede pedir datos del ADMIN ni de otro funcionario.
 * Sin sesión, AuthFilter responde 401.
 */
@WebServlet("/dashboard/datos")
public class DashboardDatosServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(DashboardDatosServlet.class.getName());
    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final PanelDatos panelDatos = new PanelDatos();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Usuario u = (Usuario) req.getSession().getAttribute(LoginServlet.SESION_USUARIO);
        if (u == null) {   // no debería pasar (AuthFilter ya responde 401), pero se comprueba igual
            responder(resp, HttpServletResponse.SC_UNAUTHORIZED, falla("La sesión expiró. Vuelva a iniciar sesión."));
            return;
        }
        try {
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("ok", true);
            datos.put("rol", u.getRol());
            datos.put("actualizado", LocalTime.now().format(HORA));
            datos.putAll(panelDatos.construir(u.getRol(), u.getIdUsuario()));
            responder(resp, HttpServletResponse.SC_OK, datos);
        } catch (DaoException | IllegalArgumentException e) {
            LOG.log(Level.SEVERE, "No se pudo armar el panel de " + u.getRol(), e);   // detalle solo al log
            responder(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    falla("No se pudo cargar la información. Intente nuevamente."));
        }
    }

    private Map<String, Object> falla(String mensaje) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", false);
        r.put("mensaje", mensaje);
        return r;
    }

    private void responder(HttpServletResponse resp, int estado, Map<String, Object> cuerpo) throws IOException {
        resp.setStatus(estado);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(GSON.toJson(cuerpo));
    }
}

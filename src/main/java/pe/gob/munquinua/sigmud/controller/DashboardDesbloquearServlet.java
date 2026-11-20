package pe.gob.munquinua.sigmud.controller;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import pe.gob.munquinua.sigmud.dao.UsuarioDAO;
import pe.gob.munquinua.sigmud.dao.impl.UsuarioDAOImpl;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.model.Usuario;
import pe.gob.munquinua.sigmud.util.Auditoria;

/**
 * Desbloquea una cuenta bloqueada por intentos fallidos (POST /dashboard/desbloquear, respuesta JSON).
 * Solo ADMIN: AuthFilter lo exige por ruta y aquí se vuelve a comprobar el rol de la SESIÓN.
 * El token CSRF lo exige CsrfFilter (cabecera X-CSRF-Token). Cada desbloqueo queda en el log del servidor.
 */
@WebServlet("/dashboard/desbloquear")
public class DashboardDesbloquearServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(DashboardDesbloquearServlet.class.getName());
    private static final Gson GSON = new Gson();
    private static final Pattern ENTERO = Pattern.compile("^\\d{1,9}$");
    private static final String MSG_GENERICO = "No se pudo completar la operación. Intente nuevamente.";

    private final UsuarioDAO usuarioDao = new UsuarioDAOImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Usuario admin = (Usuario) req.getSession().getAttribute(LoginServlet.SESION_USUARIO);
        if (admin == null || !"ADMIN".equals(admin.getRol())) {   // el rol se verifica en el servidor
            responder(resp, HttpServletResponse.SC_FORBIDDEN, falla("No tiene permiso para esta acción."));
            return;
        }
        String idTexto = req.getParameter("id") == null ? "" : req.getParameter("id").trim();
        if (!ENTERO.matcher(idTexto).matches()) {
            responder(resp, HttpServletResponse.SC_OK, falla("El usuario indicado no es válido."));
            return;
        }
        try {
            responder(resp, HttpServletResponse.SC_OK, desbloquear(req, Integer.parseInt(idTexto), admin));
        } catch (DaoException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            responder(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, falla(MSG_GENERICO));
        }
    }

    private Map<String, Object> desbloquear(HttpServletRequest req, int idUsuario, Usuario admin) {
        Optional<Usuario> objetivo = usuarioDao.buscarPorId(idUsuario);
        if (objetivo.isEmpty()) {
            return falla("El usuario no existe.");
        }
        if (!usuarioDao.desbloquear(idUsuario)) {
            return falla("La cuenta de " + objetivo.get().getUsername() + " no está bloqueada.");
        }
        LOG.log(Level.WARNING, "Cuenta DESBLOQUEADA: usuario={0} (id={1}) por administrador={2} (id={3}) fecha={4}",
                new Object[] {objetivo.get().getUsername(), idUsuario, admin.getUsername(), admin.getIdUsuario(),
                    LocalDateTime.now()});
        Auditoria.registrar(req, Auditoria.USUARIO_DESBLOQUEADO, Auditoria.USUARIO, idUsuario,
                "Cuenta desbloqueada: " + objetivo.get().getUsername(),
                "intentos fallidos: " + objetivo.get().getIntentosFallidos() + " -> 0");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        r.put("mensaje", "Cuenta de " + objetivo.get().getUsername() + " desbloqueada.");
        return r;
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

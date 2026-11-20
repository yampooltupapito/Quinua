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
import pe.gob.munquinua.sigmud.util.Seguridad;

/**
 * Acciones Ajax de la gestión de usuarios (POST, respuesta JSON): /usuarios/restablecer y /usuarios/desbloquear.
 * Solo ADMIN: AuthFilter lo exige por ruta y aquí se vuelve a comprobar el rol de la SESIÓN. El token CSRF lo exige
 * CsrfFilter (cabecera X-CSRF-Token). La contraseña temporal solo viaja en esta respuesta: no se guarda ni se registra.
 */
@WebServlet({"/usuarios/restablecer", "/usuarios/desbloquear"})
public class UsuarioAjaxServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(UsuarioAjaxServlet.class.getName());
    private static final Gson GSON = new Gson();
    private static final Pattern ENTERO = Pattern.compile("^\\d{1,9}$");
    private static final String MSG_GENERICO = "No se pudo completar la operación. Intente nuevamente.";

    private final UsuarioDAO dao = new UsuarioDAOImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Usuario admin = (Usuario) req.getSession().getAttribute(LoginServlet.SESION_USUARIO);
        if (admin == null || !"ADMIN".equals(admin.getRol())) {   // el rol se verifica en el servidor
            responder(resp, HttpServletResponse.SC_FORBIDDEN, falla("No tiene permiso para esta acción."));
            return;
        }
        String id = req.getParameter("id") == null ? "" : req.getParameter("id").trim();
        if (!ENTERO.matcher(id).matches()) {
            responder(resp, HttpServletResponse.SC_OK, falla("El usuario indicado no es válido."));
            return;
        }
        try {
            boolean restablecer = req.getServletPath().endsWith("/restablecer");
            Map<String, Object> r = restablecer ? restablecer(req, Integer.parseInt(id), admin) : desbloquear(req, Integer.parseInt(id), admin);
            responder(resp, HttpServletResponse.SC_OK, r);
        } catch (DaoException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            responder(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, falla(MSG_GENERICO));
        }
    }

    /** Genera una contraseña temporal nueva, la guarda hasheada con debeCambiarPassword = true y la devuelve UNA vez. */
    private Map<String, Object> restablecer(HttpServletRequest req, int idUsuario, Usuario admin) {
        if (idUsuario == admin.getIdUsuario()) {
            return falla("Para cambiar su propia contraseña use \"Mi perfil\".");
        }
        Optional<Usuario> objetivo = dao.buscarPorId(idUsuario);
        if (objetivo.isEmpty()) {
            return falla("El usuario no existe.");
        }
        String temporal = Seguridad.generarPasswordTemporal();
        dao.actualizarPassword(idUsuario, Seguridad.hashear(temporal), true);
        LOG.log(Level.WARNING, "Contraseña RESTABLECIDA: usuario={0} (id={1}) por administrador={2} fecha={3}",
                new Object[] {objetivo.get().getUsername(), idUsuario, admin.getUsername(), LocalDateTime.now()});   // sin la contraseña
        // Auditoría: solo quién y a quién; la contraseña temporal NO se guarda
        Auditoria.registrar(req, Auditoria.PASSWORD_RESTABLECIDA, Auditoria.USUARIO, idUsuario,
                "Contraseña restablecida a " + objetivo.get().getUsername(), "debe cambiarla en el próximo inicio de sesión");
        Map<String, Object> r = exito("Contraseña restablecida. El usuario deberá cambiarla al ingresar.");
        r.put("usuario", objetivo.get().getUsername());
        r.put("passwordTemporal", temporal);
        return r;
    }

    private Map<String, Object> desbloquear(HttpServletRequest req, int idUsuario, Usuario admin) {
        Optional<Usuario> objetivo = dao.buscarPorId(idUsuario);
        if (objetivo.isEmpty()) {
            return falla("El usuario no existe.");
        }
        if (!dao.desbloquear(idUsuario)) {
            return falla("La cuenta de " + objetivo.get().getUsername() + " no está bloqueada.");
        }
        LOG.log(Level.WARNING, "Cuenta DESBLOQUEADA: usuario={0} (id={1}) por administrador={2} (id={3}) fecha={4}",
                new Object[] {objetivo.get().getUsername(), idUsuario, admin.getUsername(), admin.getIdUsuario(),
                    LocalDateTime.now()});
        Auditoria.registrar(req, Auditoria.USUARIO_DESBLOQUEADO, Auditoria.USUARIO, idUsuario,
                "Cuenta desbloqueada: " + objetivo.get().getUsername(), "intentos fallidos: " + objetivo.get().getIntentosFallidos() + " -> 0");
        return exito("Cuenta de " + objetivo.get().getUsername() + " desbloqueada.");
    }

    private Map<String, Object> exito(String mensaje) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        r.put("mensaje", mensaje);
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

package pe.gob.munquinua.sigmud.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import pe.gob.munquinua.sigmud.dao.UsuarioDAO;
import pe.gob.munquinua.sigmud.dao.impl.UsuarioDAOImpl;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.model.Usuario;
import pe.gob.munquinua.sigmud.util.Auditoria;
import pe.gob.munquinua.sigmud.util.Seguridad;
import pe.gob.munquinua.sigmud.util.Validador;

/**
 * Mi perfil (todos los roles). GET /perfil: datos del usuario de la sesión. GET/POST /perfil/password: cambio de
 * contraseña (contraseña actual, nueva y confirmación). El usuario sale SIEMPRE de la sesión.
 * Si la contraseña actual es incorrecta cuenta como intento fallido igual que en el login.
 * Un usuario con contraseña temporal solo puede usar /perfil/password (lo fuerza AuthFilter).
 */
@WebServlet({"/perfil", "/perfil/password"})
public class PerfilServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(PerfilServlet.class.getName());
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final int MAX_CLAVE = 100;
    private static final String MSG_GENERICO = "No se pudo completar la operación. Intente nuevamente.";

    private final UsuarioDAO dao = new UsuarioDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        mostrarFlash(req);
        if (req.getServletPath().equals("/perfil/password")) {
            req.setAttribute("errores", new ArrayList<String>());
            req.getRequestDispatcher("/WEB-INF/views/perfil/password.jsp").forward(req, resp);
            return;
        }
        HttpSession s = req.getSession();
        req.setAttribute("inicioSesion", LocalDateTime.ofInstant(Instant.ofEpochMilli(s.getCreationTime()),
                ZoneId.systemDefault()).format(FECHA_HORA));
        req.getRequestDispatcher("/WEB-INF/views/perfil/perfil.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!req.getServletPath().equals("/perfil/password")) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        Usuario sesion = (Usuario) req.getSession().getAttribute(LoginServlet.SESION_USUARIO);
        List<String> errores = new ArrayList<>();
        try {
            Optional<Usuario> actual = dao.buscarPorUsername(sesion.getUsername());   // con hash e intentos actuales
            if (actual.isEmpty()) {
                resp.sendRedirect(req.getContextPath() + "/logout");
                return;
            }
            if (cambiar(req, resp, actual.get(), errores)) {
                return;   // éxito (ya respondió) o la cuenta se bloqueó (ya respondió)
            }
        } catch (DaoException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            errores.add(MSG_GENERICO);
        }
        req.setAttribute("errores", errores);
        req.getRequestDispatcher("/WEB-INF/views/perfil/password.jsp").forward(req, resp);
    }

    /** @return true si ya se envió la respuesta (cambio exitoso o cuenta bloqueada); false para mostrar los errores */
    private boolean cambiar(HttpServletRequest req, HttpServletResponse resp, Usuario actual, List<String> errores)
            throws IOException {
        String contrasenaActual = parametro(req, "actual");
        String nueva = parametro(req, "nueva");
        String confirmacion = parametro(req, "confirmacion");

        if (contrasenaActual.isEmpty() || contrasenaActual.length() > MAX_CLAVE
                || !Seguridad.verificar(contrasenaActual, actual.getPasswordHash())) {
            return claveActualIncorrecta(req, resp, actual, errores);
        }
        errores.addAll(Validador.politicaPassword(nueva, actual.getUsername()));
        if (!nueva.equals(confirmacion)) {
            errores.add("La confirmación no coincide con la nueva contraseña.");
        }
        if (nueva.equals(contrasenaActual)) {
            errores.add("La nueva contraseña debe ser distinta de la actual.");
        }
        if (!errores.isEmpty()) {
            return false;
        }
        dao.actualizarPassword(actual.getIdUsuario(), Seguridad.hashear(nueva), false);
        Usuario sesion = (Usuario) req.getSession().getAttribute(LoginServlet.SESION_USUARIO);
        sesion.setDebeCambiarPassword(false);
        guardarFlash(req, "success", "Contraseña actualizada correctamente.");
        Auditoria.registrar(req, Auditoria.PASSWORD_CAMBIADA, Auditoria.USUARIO, actual.getIdUsuario(),
                "Contraseña cambiada por el propio usuario: " + actual.getUsername(), null);   // nunca la contraseña
        req.changeSessionId();   // nuevo identificador de sesión tras un cambio de credenciales
        LOG.log(Level.INFO, "Contraseña cambiada: usuario={0} fecha={1}", new Object[] {actual.getUsername(), LocalDateTime.now()});
        resp.sendRedirect(req.getContextPath() + "/perfil");   // Post-Redirect-Get
        return true;
    }

    /** Cuenta el intento fallido como en el login; si con este intento se bloquea la cuenta, cierra la sesión. */
    private boolean claveActualIncorrecta(HttpServletRequest req, HttpServletResponse resp, Usuario actual,
                                               List<String> errores) throws IOException {
        dao.registrarIntentoFallido(actual.getIdUsuario());
        LOG.log(Level.WARNING, "Intento fallido al cambiar la contraseña: usuario={0} fecha={1}",
                new Object[] {actual.getUsername(), LocalDateTime.now()});
        if (actual.getIntentosFallidos() + 1 >= UsuarioDAOImpl.MAX_INTENTOS) {
            LOG.log(Level.WARNING, "CUENTA BLOQUEADA tras {0} intentos fallidos: usuario={1} fecha={2}",
                    new Object[] {UsuarioDAOImpl.MAX_INTENTOS, actual.getUsername(), LocalDateTime.now()});
            req.getSession().invalidate();
            resp.sendRedirect(req.getContextPath() + "/login");
            return true;
        }
        errores.add("La contraseña actual no es correcta.");
        return false;
    }

    private String parametro(HttpServletRequest req, String nombre) {
        String v = req.getParameter(nombre);
        return v == null ? "" : v;   // las contraseñas no se recortan (los espacios cuentan)
    }

    private void guardarFlash(HttpServletRequest req, String tipo, String texto) {
        HttpSession s = req.getSession();
        s.setAttribute("flashTipo", tipo);
        s.setAttribute("flashTexto", texto);
    }

    private void mostrarFlash(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("flashTexto") == null) {
            return;
        }
        req.setAttribute("mensaje", s.getAttribute("flashTexto"));
        req.setAttribute("mensajeTipo", s.getAttribute("flashTipo"));
        s.removeAttribute("flashTexto");
        s.removeAttribute("flashTipo");
    }
}

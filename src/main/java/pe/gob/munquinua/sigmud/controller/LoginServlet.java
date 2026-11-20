package pe.gob.munquinua.sigmud.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import pe.gob.munquinua.sigmud.dao.UsuarioDAO;
import pe.gob.munquinua.sigmud.dao.impl.UsuarioDAOImpl;
import pe.gob.munquinua.sigmud.filter.CsrfFilter;
import pe.gob.munquinua.sigmud.exception.CredencialesInvalidasException;
import pe.gob.munquinua.sigmud.exception.CuentaBloqueadaException;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.model.Usuario;
import pe.gob.munquinua.sigmud.util.Auditoria;
import pe.gob.munquinua.sigmud.util.Seguridad;
import pe.gob.munquinua.sigmud.util.Validador;

/** Inicio de sesión: valida credenciales, controla los intentos fallidos y crea la sesión (con su token CSRF). */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    public static final String SESION_USUARIO = "usuario";
    private static final Logger LOG = Logger.getLogger(LoginServlet.class.getName());
    private static final String VISTA = "/WEB-INF/views/login.jsp";
    private static final int MAX_CLAVE = 100;
    private static final int MAX_USUARIO_LOG = 30;
    /** Hash de relleno (PBKDF2): se verifica igual aunque el usuario no exista, para tardar lo mismo. */
    private static final String HASH_FALSO = Seguridad.hashear("usuario-inexistente");

    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s != null && s.getAttribute(SESION_USUARIO) != null) {
            resp.sendRedirect(req.getContextPath() + "/menu");
            return;
        }
        req.getRequestDispatcher(VISTA).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String clave = req.getParameter("clave");
        try {
            Usuario u = autenticar(req, username, clave);
            iniciarSesion(req, u);
            Auditoria.registrar(req, Auditoria.LOGIN_OK, Auditoria.SESION, null, "Inicio de sesión correcto", null);
            resp.sendRedirect(req.getContextPath() + "/menu"); // Post-Redirect-Get
        } catch (CredencialesInvalidasException | CuentaBloqueadaException e) {
            req.setAttribute("mensaje", e.getMessage());
            req.setAttribute("username", username == null ? "" : username.trim());
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            req.getRequestDispatcher(VISTA).forward(req, resp);
        }
    }

    /**
     * Valida, busca el usuario y compara el hash. Si falla, cuenta el intento
     * (solo en cuentas activas). Todo fallo devuelve el mismo mensaje genérico;
     * el aviso de cuenta bloqueada solo se da cuando la contraseña es correcta.
     */
    private Usuario autenticar(HttpServletRequest req, String username, String clave)
            throws CredencialesInvalidasException, CuentaBloqueadaException {
        if (!Validador.usuarioValido(username) || clave == null
                || clave.isEmpty() || clave.length() > MAX_CLAVE) {
            registrarFallo(req, username, null, "datos con formato inválido");
            throw new CredencialesInvalidasException();
        }
        Optional<Usuario> opt = usuarioDAO.buscarPorUsername(username.trim());
        String hash = opt.map(Usuario::getPasswordHash).orElse(HASH_FALSO);
        boolean claveOk = Seguridad.verificar(clave, hash) && opt.isPresent();
        if (!claveOk) {
            contarIntentoFallido(req, username, opt);
            throw new CredencialesInvalidasException();
        }
        Usuario u = opt.get();
        if (!u.isEstado()) {
            registrarFallo(req, username, u.getIdUsuario(), "cuenta bloqueada (se usó la contraseña correcta)");
            throw new CuentaBloqueadaException();
        }
        usuarioDAO.registrarAccesoExitoso(u.getIdUsuario());
        migrarHashAntiguo(u, clave);
        return u;
    }

    /**
     * Migración automática: si la contraseña estaba guardada con el hash antiguo (SHA-256 sin sal), se vuelve a
     * guardar con PBKDF2 y sal usando la contraseña que el usuario acaba de escribir. Si falla (por ejemplo, la base
     * aún no tiene el campo ampliado), el inicio de sesión continúa igual y se avisa en el log.
     */
    private void migrarHashAntiguo(Usuario u, String clave) {
        if (!Seguridad.esLegado(u.getPasswordHash())) {
            return;
        }
        try {
            usuarioDAO.actualizarPassword(u.getIdUsuario(), Seguridad.hashear(clave), u.isDebeCambiarPassword());
            LOG.log(Level.INFO, "Contraseña migrada a PBKDF2: usuario={0}", limpiar(u.getUsername()));
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "No se pudo migrar el hash de la contraseña (¿se ejecutó sql/04_usuarios.sql?)", e);
        }
    }

    /** Suma el intento fallido (solo cuentas activas) y deja en el log el fallo y, si ocurre, el bloqueo. */
    private void contarIntentoFallido(HttpServletRequest req, String username, Optional<Usuario> opt) {
        if (opt.isEmpty()) {
            registrarFallo(req, username, null, "usuario inexistente");
            return;
        }
        registrarFallo(req, username, opt.get().getIdUsuario(), opt.get().isEstado() ? "contraseña incorrecta" : "cuenta ya bloqueada");
        if (opt.get().isEstado()) {
            usuarioDAO.registrarIntentoFallido(opt.get().getIdUsuario());
            if (opt.get().getIntentosFallidos() + 1 >= UsuarioDAOImpl.MAX_INTENTOS) {
                LOG.log(Level.WARNING, "CUENTA BLOQUEADA tras {0} intentos fallidos: usuario={1} fecha={2}",
                        new Object[] {UsuarioDAOImpl.MAX_INTENTOS, limpiar(username), LocalDateTime.now()});
                Auditoria.registrarComo(req, null, username, Auditoria.CUENTA_BLOQUEADA, Auditoria.USUARIO,
                        opt.get().getIdUsuario(), "Cuenta bloqueada tras " + UsuarioDAOImpl.MAX_INTENTOS
                        + " intentos fallidos: " + opt.get().getUsername(), null);
            }
        }
    }

    /** Registra en el log del servidor un intento fallido: usuario, fecha y hora (nunca la contraseña). */
    private void registrarFallo(HttpServletRequest req, String username, Integer idCuenta, String motivo) {
        Auditoria.registrarComo(req, null, username, Auditoria.LOGIN_FALLIDO, Auditoria.SESION, idCuenta,
                "Inicio de sesión fallido", "motivo: " + motivo);
        LOG.log(Level.WARNING, "Intento de login fallido: usuario={0} fecha={1} motivo={2}",
                new Object[] {limpiar(username), LocalDateTime.now(), motivo});
    }

    /** Evita que el usuario escrito inyecte saltos de línea o texto engañoso en el log. */
    private String limpiar(String texto) {
        String t = texto == null ? "" : texto.trim();
        t = t.length() > MAX_USUARIO_LOG ? t.substring(0, MAX_USUARIO_LOG) : t;
        return t.replaceAll("[^A-Za-z0-9._-]", "?");
    }

    /** Invalida la sesión anterior y crea una NUEVA (evita fijación de sesión) con su token CSRF. */
    private void iniciarSesion(HttpServletRequest req, Usuario u) {
        HttpSession anterior = req.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        u.setPasswordHash(null); // el hash nunca va a la sesión
        HttpSession nueva = req.getSession(true);
        nueva.setAttribute(SESION_USUARIO, u);
        nueva.setAttribute(CsrfFilter.ATRIBUTO_SESION, Seguridad.tokenAleatorio());
    }
}

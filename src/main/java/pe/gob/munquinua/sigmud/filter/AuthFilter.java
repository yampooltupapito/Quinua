package pe.gob.munquinua.sigmud.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import pe.gob.munquinua.sigmud.controller.LoginServlet;
import pe.gob.munquinua.sigmud.model.Usuario;

/**
 * Control de acceso: solo /login y /assets/* son públicos. El resto exige sesión (las rutas /dashboard/* responden 401),
 * y las rutas de cada módulo exigen además uno de los roles permitidos (si no: 403).
 * Un usuario con contraseña temporal (debeCambiarPassword) solo puede usar /perfil/password, /logout y /assets.
 * Se registra en web.xml (ahí se fija el orden de los filtros).
 */
public class AuthFilter implements Filter {

    /** Roles permitidos por ruta. Para agregar un módulo basta una línea aquí. */
    private static final Map<String, Set<String>> ROLES_POR_RUTA = Map.of(
            "/contribuyentes", Set.of("ADMIN", "ATENCION"),
            "/almacen", Set.of("ADMIN", "ALMACEN"),
            "/citas", Set.of("ADMIN", "ATENCION"),
            "/dashboard/datos", Set.of("ADMIN", "ATENCION", "ALMACEN"),
            "/dashboard/desbloquear", Set.of("ADMIN"),
            "/usuarios", Set.of("ADMIN"),
            "/auditoria", Set.of("ADMIN"));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String ruta = req.getRequestURI().substring(req.getContextPath().length());

        if (esPublica(ruta)) {
            chain.doFilter(req, resp);
            return;
        }
        evitarCache(resp);
        Usuario u = usuarioDeSesion(req);
        if (u == null && ruta.startsWith("/dashboard/")) {
            sesionExpirada(resp);   // llamadas Ajax del panel: 401 con JSON (no una redirección al login)
        } else if (u == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
        } else if (u.isDebeCambiarPassword() && !esRutaDeCambioDeClave(ruta)) {
            forzarCambioDeClave(req, resp);   // contraseña temporal: primero debe cambiarla
        } else if (!rolPermite(ruta, u.getRol())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } else {
            chain.doFilter(req, resp);
        }
    }

    /** Con una contraseña temporal solo se permiten el cambio de contraseña y el cierre de sesión (/assets ya es público). */
    private boolean esRutaDeCambioDeClave(String ruta) {
        return ruta.equals("/perfil/password") || ruta.equals("/logout");
    }

    /** Deja un mensaje explicativo (se muestra una sola vez) y redirige a la pantalla de cambio de contraseña. */
    private void forzarCambioDeClave(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession();
        s.setAttribute("flashTipo", "warning");
        s.setAttribute("flashTexto", "Su contraseña es temporal: debe cambiarla ahora para poder usar el sistema.");
        resp.sendRedirect(req.getContextPath() + "/perfil/password");
    }

    /** Responde 401 con un JSON simple (lo usa el panel, que consulta por Ajax). */
    private void sesionExpirada(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write("{\"ok\":false,\"mensaje\":\"La sesión expiró. Vuelva a iniciar sesión.\"}");
    }

    /** true si el rol puede entrar a la ruta (las rutas sin restricción de rol pasan). */
    public static boolean rolPermite(String ruta, String rol) {
        for (Map.Entry<String, Set<String>> e : ROLES_POR_RUTA.entrySet()) {
            String base = e.getKey();
            if (ruta.equals(base) || ruta.startsWith(base + "/")) {
                return e.getValue().contains(rol);
            }
        }
        return true;
    }

    private boolean esPublica(String ruta) {
        return ruta.equals("/login") || ruta.startsWith("/assets/");
    }

    private Usuario usuarioDeSesion(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (Usuario) s.getAttribute(LoginServlet.SESION_USUARIO);
    }

    /** El navegador no debe guardar páginas protegidas (botón "atrás" tras cerrar sesión). */
    private void evitarCache(HttpServletResponse resp) {
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);
    }
}

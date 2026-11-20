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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protección CSRF: todo POST (menos /login) debe traer el token de la sesión, en el parámetro "csrf"
 * (formularios) o en la cabecera X-CSRF-Token (Ajax). Si falta o no coincide, responde 403.
 * Se registra en web.xml después de AuthFilter.
 */
public class CsrfFilter implements Filter {

    /** Nombre del atributo de sesión donde LoginServlet guarda el token. */
    public static final String ATRIBUTO_SESION = "csrfToken";
    public static final String PARAMETRO = "csrf";
    public static final String CABECERA = "X-CSRF-Token";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        if (!"POST".equalsIgnoreCase(req.getMethod()) || esLogin(req) || tokenValido(req)) {
            chain.doFilter(request, response);
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    private boolean esLogin(HttpServletRequest req) {
        return "/login".equals(req.getRequestURI().substring(req.getContextPath().length()));
    }

    /** Compara en tiempo constante el token recibido con el de la sesión. */
    private boolean tokenValido(HttpServletRequest req) {
        HttpSession sesion = req.getSession(false);
        Object esperado = sesion == null ? null : sesion.getAttribute(ATRIBUTO_SESION);
        String recibido = req.getParameter(PARAMETRO);
        if (recibido == null || recibido.isEmpty()) {
            recibido = req.getHeader(CABECERA);
        }
        if (esperado == null || recibido == null) {
            return false;
        }
        return MessageDigest.isEqual(esperado.toString().getBytes(StandardCharsets.UTF_8),
                recibido.getBytes(StandardCharsets.UTF_8));
    }
}

package pe.gob.munquinua.sigmud.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Agrega cabeceras de seguridad a todas las respuestas: evita que el navegador "adivine" tipos de
 * archivo, impide mostrar el sistema dentro de un iframe y limita los recursos al mismo origen (CSP).
 * Se registra en web.xml (ahí se fija el orden de los filtros).
 */
public class SecurityHeadersFilter implements Filter {

    /** Solo recursos del mismo origen; sin scripts en línea. Los estilos en línea se permiten (atributos style). */
    private static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data:; font-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse resp = (HttpServletResponse) res;
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Referrer-Policy", "same-origin");
        resp.setHeader("Content-Security-Policy", CSP);
        chain.doFilter(req, res);
    }
}

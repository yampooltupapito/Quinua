package pe.gob.munquinua.sigmud.listener;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Al iniciar la aplicación comprueba si existe el logo institucional (el archivo lo coloca la municipalidad en
 * webapp/assets/img/logo.png) y deja el resultado en el contexto para que las páginas decidan si lo muestran:
 *   logoDisponible = true si existe /assets/img/logo.png
 *   faviconPng     = true si hay logo Y existe /assets/img/favicon.png (si no, se usa assets/img/favicon.svg)
 * Si no hay logo, las páginas muestran un ícono genérico de Bootstrap Icons (nunca se inventa un escudo).
 */
@WebListener
public class ContextoListener implements ServletContextListener {

    public static final String LOGO = "logoDisponible";
    public static final String FAVICON_PNG = "faviconPng";
    static final String RUTA_LOGO = "/assets/img/logo.png";
    static final String RUTA_FAVICON_PNG = "/assets/img/favicon.png";

    private static final Logger LOG = Logger.getLogger(ContextoListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        boolean logo = existe(ctx, RUTA_LOGO);
        ctx.setAttribute(LOGO, logo);
        ctx.setAttribute(FAVICON_PNG, logo && existe(ctx, RUTA_FAVICON_PNG));
        LOG.log(Level.INFO, "Logo institucional disponible: {0}", logo);
    }

    /** true si el recurso existe dentro de la aplicación (getResource devuelve null si no existe). */
    static boolean existe(ServletContext ctx, String ruta) {
        try {
            return ctx.getResource(ruta) != null;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}

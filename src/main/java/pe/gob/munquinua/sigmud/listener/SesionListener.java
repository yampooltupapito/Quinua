package pe.gob.munquinua.sigmud.listener;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import java.util.concurrent.atomic.AtomicInteger;

/** Cuenta las sesiones activas y las deja en el contexto como "sesionesActivas". */
@WebListener
public class SesionListener implements HttpSessionListener {

    public static final String ATRIBUTO = "sesionesActivas";
    private static final AtomicInteger ACTIVAS = new AtomicInteger();

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        actualizar(se, ACTIVAS.incrementAndGet());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        actualizar(se, ACTIVAS.updateAndGet(n -> Math.max(0, n - 1)));
    }

    private void actualizar(HttpSessionEvent se, int total) {
        se.getSession().getServletContext().setAttribute(ATRIBUTO, total);
    }
}

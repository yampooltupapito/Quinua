package pe.gob.munquinua.sigmud.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import pe.gob.munquinua.sigmud.util.Auditoria;

/** Cierra la sesión y vuelve al login. */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        if (s != null && s.getAttribute(LoginServlet.SESION_USUARIO) != null) {
            Auditoria.registrar(req, Auditoria.LOGOUT, Auditoria.SESION, null, "Cierre de sesión", null);
        }
        if (s != null) {
            s.invalidate();
        }
        resp.sendRedirect(req.getContextPath() + "/login");
    }
}

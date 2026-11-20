package pe.gob.munquinua.sigmud.controller;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import pe.gob.munquinua.sigmud.dao.MaterialDAO;
import pe.gob.munquinua.sigmud.dao.MovimientoDAO;
import pe.gob.munquinua.sigmud.dao.impl.MaterialDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.MovimientoDAOImpl;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.exception.MaterialNoDisponibleException;
import pe.gob.munquinua.sigmud.exception.StockInsuficienteException;
import pe.gob.munquinua.sigmud.model.Material;
import pe.gob.munquinua.sigmud.model.MovimientoStock;
import pe.gob.munquinua.sigmud.model.Pagina;
import pe.gob.munquinua.sigmud.model.Usuario;
import pe.gob.munquinua.sigmud.util.ListaVista;

/**
 * Entradas y salidas de stock. POST registrar: responde JSON (lo llama $.ajax).
 * GET historial: página con los movimientos de un material.
 * El acceso (ADMIN y ALMACEN) lo controla AuthFilter para toda la ruta /almacen/*.
 */
@WebServlet("/almacen/movimiento")
public class MovimientoServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(MovimientoServlet.class.getName());
    private static final Gson GSON = new Gson();
    private static final Pattern ENTERO = Pattern.compile("^\\d{1,9}$");
    private static final int MAX_CANTIDAD = 100000;
    private static final int MAX_OBSERVACION = 200;
    private static final String MSG_GENERICO = "No se pudo completar la operación. Intente nuevamente.";

    private final MovimientoDAO movimientoDao = new MovimientoDAOImpl();
    private final MaterialDAO materialDao = new MaterialDAOImpl();

    // ---------- POST: registrar (JSON) ----------

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!"registrar".equals(texto(req, "accion"))) {
            responder(resp, HttpServletResponse.SC_BAD_REQUEST, falla("Acción no válida."));
            return;
        }
        String error = validar(req);
        if (error != null) {
            responder(resp, HttpServletResponse.SC_OK, falla(error));
            return;
        }
        String tipo = texto(req, "tipo");
        int idMaterial = entero(req.getParameter("idMaterial"));
        // El usuario SIEMPRE sale de la sesión, nunca del formulario.
        Usuario u = (Usuario) req.getSession().getAttribute(LoginServlet.SESION_USUARIO);
        String obs = texto(req, "observacion");
        Integer area = MovimientoStock.SALIDA.equals(tipo) ? entero(req.getParameter("idArea")) : null;
        try {
            int stock = movimientoDao.registrarMovimiento(idMaterial, tipo,
                    entero(req.getParameter("cantidad")), area, u.getIdUsuario(), obs.isEmpty() ? null : obs,
                    req.getRemoteAddr());   // la auditoría se guarda dentro de la misma transacción
            responder(resp, HttpServletResponse.SC_OK, exito(tipo, stock, esCritico(idMaterial, stock)));
        } catch (StockInsuficienteException | MaterialNoDisponibleException e) {
            responder(resp, HttpServletResponse.SC_OK, falla(e.getMessage()));
        } catch (DaoException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e); // detalle solo al log
            responder(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, falla(MSG_GENERICO));
        }
    }

    /** Devuelve el primer error de validación, o null si todo está bien. */
    private String validar(HttpServletRequest req) {
        String tipo = texto(req, "tipo");
        if (!MovimientoStock.ENTRADA.equals(tipo) && !MovimientoStock.SALIDA.equals(tipo)) {
            return "El tipo de movimiento no es válido.";
        }
        if (entero(req.getParameter("idMaterial")) <= 0) {
            return "El material no es válido.";
        }
        int cantidad = entero(req.getParameter("cantidad"));
        if (cantidad < 1 || cantidad > MAX_CANTIDAD) {
            return "La cantidad debe ser un número entero entre 1 y " + MAX_CANTIDAD + ".";
        }
        if (MovimientoStock.SALIDA.equals(tipo) && entero(req.getParameter("idArea")) <= 0) {
            return "Seleccione el área solicitante.";
        }
        if (texto(req, "observacion").length() > MAX_OBSERVACION) {
            return "La observación no puede pasar de " + MAX_OBSERVACION + " caracteres.";
        }
        return null;
    }

    // ---------- GET: historial ----------

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!"historial".equals(texto(req, "accion"))) {
            resp.sendRedirect(req.getContextPath() + "/almacen");
            return;
        }
        try {
            Optional<Material> m = materialDao.buscarPorId(entero(req.getParameter("id")));
            if (m.isPresent()) {
                String orden = ListaVista.orden(req);
                String dir = ListaVista.dir(req);
                Pagina<MovimientoStock> pag = movimientoDao.listarPorMaterialPagina(m.get().getIdMaterial(),
                        ListaVista.pagina(req), ListaVista.tamano(req), orden, dir);
                Map<String, String> filtros = new LinkedHashMap<>();
                filtros.put("accion", "historial");
                filtros.put("id", String.valueOf(m.get().getIdMaterial()));
                ListaVista.preparar(req, pag, "/almacen/movimiento", filtros, MovimientoDAOImpl.resolverOrden(orden, dir));
                req.setAttribute("material", m.get());
                req.setAttribute("movimientos", pag.getItems());
                req.getRequestDispatcher("/WEB-INF/views/almacen/historial.jsp").forward(req, resp);
                return;
            }
            avisarYVolver(req, resp, "El material no existe.");
        } catch (DaoException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            avisarYVolver(req, resp, MSG_GENERICO);
        }
    }

    // ---------- auxiliares ----------

    /** Guarda un mensaje flash (lo muestra el listado de materiales) y redirige al listado. */
    private void avisarYVolver(HttpServletRequest req, HttpServletResponse resp, String mensaje)
            throws IOException {
        req.getSession().setAttribute("flashTipo", "danger");
        req.getSession().setAttribute("flashTexto", mensaje);
        resp.sendRedirect(req.getContextPath() + "/almacen");
    }

    /** true si tras el movimiento el stock quedó en el mínimo o por debajo. */
    private boolean esCritico(int idMaterial, int stock) {
        try {
            return materialDao.buscarPorId(idMaterial).map(m -> stock <= m.getStockMinimo()).orElse(false);
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "No se pudo calcular si el material es crítico", e);
            return false; // el movimiento ya se guardó; solo falla la insignia
        }
    }

    private Map<String, Object> exito(String tipo, int stock, boolean critico) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        r.put("mensaje", (MovimientoStock.ENTRADA.equals(tipo) ? "Entrada" : "Salida")
                + " registrada correctamente. Stock actual: " + stock + ".");
        r.put("stockActual", stock);
        r.put("critico", critico);
        return r;
    }

    private Map<String, Object> falla(String mensaje) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", false);
        r.put("mensaje", mensaje);
        return r;
    }

    private void responder(HttpServletResponse resp, int estado, Map<String, Object> cuerpo)
            throws IOException {
        resp.setStatus(estado);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(GSON.toJson(cuerpo));
    }

    private String texto(HttpServletRequest req, String nombre) {
        String v = req.getParameter(nombre);
        return v == null ? "" : v.trim();
    }

    /** Convierte a entero; devuelve -1 si no es un número entero sin signo válido. */
    private int entero(String s) {
        return s != null && ENTERO.matcher(s.trim()).matches() ? Integer.parseInt(s.trim()) : -1;
    }
}

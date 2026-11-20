package pe.gob.munquinua.sigmud.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import pe.gob.munquinua.sigmud.dao.AreaDAO;
import pe.gob.munquinua.sigmud.dao.MaterialDAO;
import pe.gob.munquinua.sigmud.dao.impl.AreaDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.MaterialDAOImpl;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.exception.RegistroDuplicadoException;
import pe.gob.munquinua.sigmud.model.Area;
import pe.gob.munquinua.sigmud.model.Material;
import pe.gob.munquinua.sigmud.model.Pagina;
import pe.gob.munquinua.sigmud.util.Auditoria;
import pe.gob.munquinua.sigmud.util.ListaVista;
import pe.gob.munquinua.sigmud.util.MaterialValidador;

/**
 * Módulo Almacén, parte 1: CRUD de materiales. GET: listar, nuevo, editar. POST: guardar, baja.
 * El acceso por rol (ADMIN y ALMACEN) lo controla AuthFilter.
 * El stock NO se edita aquí: solo cambiará con movimientos de entrada y salida.
 */
@WebServlet("/almacen")
public class MaterialServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(MaterialServlet.class.getName());
    private static final String VISTA_LISTADO = "/WEB-INF/views/almacen/listado.jsp";
    private static final String VISTA_FORM = "/WEB-INF/views/almacen/formulario.jsp";
    private static final String FLASH_TEXTO = "flashTexto";
    private static final String FLASH_TIPO = "flashTipo";
    private static final String MSG_GENERICO = "No se pudo completar la operación. Intente nuevamente.";

    private final MaterialDAO dao = new MaterialDAOImpl();
    private final AreaDAO areaDao = new AreaDAOImpl();

    // ---------- GET ----------

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        switch (texto(req, "accion")) {
            case "nuevo" -> mostrarFormulario(req, resp, new Material(), null, new ArrayList<>());
            case "editar" -> editar(req, resp);
            default -> listar(req, resp);
        }
    }

    private void listar(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        mostrarFlash(req);
        String q = texto(req, "q");
        boolean critico = "1".equals(texto(req, "critico"));
        String orden = ListaVista.orden(req);
        String dir = ListaVista.dir(req);
        List<Material> lista = List.of();
        List<Area> areas = List.of();
        try {
            Pagina<Material> pag = dao.listarPagina(q, critico, ListaVista.pagina(req), ListaVista.tamano(req), orden, dir);
            lista = pag.getItems();
            Map<String, String> filtros = new LinkedHashMap<>();
            filtros.put("q", q);
            filtros.put("critico", critico ? "1" : "");
            ListaVista.preparar(req, pag, "/almacen", filtros, MaterialDAOImpl.resolverOrden(orden, dir));
            areas = areaDao.listarActivas(); // para la lista de áreas del modal de salida
        } catch (DaoException e) {
            registrar(e);
            req.setAttribute("mensaje", MSG_GENERICO);
            req.setAttribute("mensajeTipo", "danger");
        }
        req.setAttribute("lista", lista);
        req.setAttribute("areas", areas);
        req.setAttribute("q", q);
        req.setAttribute("critico", critico);
        req.getRequestDispatcher(VISTA_LISTADO).forward(req, resp);
    }

    private void editar(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            Optional<Material> m = dao.buscarPorId(leerId(req.getParameter("id")));
            if (m.isPresent() && m.get().isEstado()) {
                mostrarFormulario(req, resp, m.get(), null, new ArrayList<>());
                return;
            }
            guardarFlash(req, "danger", "El material no existe o está dado de baja.");
        } catch (DaoException e) {
            registrar(e);
            guardarFlash(req, "danger", MSG_GENERICO);
        }
        resp.sendRedirect(req.getContextPath() + "/almacen");
    }

    // ---------- POST ----------

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        switch (texto(req, "accion")) {
            case "guardar" -> guardar(req, resp);
            case "baja" -> darDeBaja(req, resp);
            default -> resp.sendRedirect(req.getContextPath() + "/almacen");
        }
    }

    private void guardar(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<String> errores = new ArrayList<>();
        Material m = leerFormulario(req, errores);
        boolean esNuevo = m.getIdMaterial() == 0;
        Optional<Material> antes = Optional.empty();   // cómo estaba antes de editar (para la auditoría)
        try {
            if (!esNuevo) {
                antes = restaurarDatosFijos(m);
                if (antes.isEmpty()) {
                    guardarFlash(req, "danger", "El material no existe.");
                    resp.sendRedirect(req.getContextPath() + "/almacen");
                    return;
                }
            }
            errores.addAll(MaterialValidador.validar(m));
            if (errores.isEmpty()) {
                persistir(m, esNuevo);
                auditarGuardado(req, antes.orElse(null), m);
                guardarFlash(req, "success", esNuevo
                        ? "Material registrado correctamente. Su stock inicial es 0."
                        : "Material actualizado correctamente.");
                resp.sendRedirect(req.getContextPath() + "/almacen"); // Post-Redirect-Get
                return;
            }
        } catch (RegistroDuplicadoException e) {
            errores.add("Ya existe un material con ese código.");
        } catch (DaoException e) {
            registrar(e);
            errores.add(MSG_GENERICO);
        }
        mostrarFormulario(req, resp, m, req.getParameter("stockMinimo"), errores);
    }

    private void persistir(Material m, boolean esNuevo) throws RegistroDuplicadoException {
        if (esNuevo) {
            dao.insertar(m);
        } else {
            dao.actualizar(m);
        }
    }

    private void darDeBaja(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = leerId(req.getParameter("id"));
        try {
            Optional<Material> existente = id > 0 ? dao.buscarPorId(id) : Optional.empty();
            if (existente.isPresent()) {
                dao.darDeBaja(id);
                Auditoria.registrar(req, Auditoria.BAJA, Auditoria.MATERIAL, id,
                        "Material dado de baja: " + existente.get().getCodigo() + " - " + existente.get().getNombre(), null);
                guardarFlash(req, "success", "Material dado de baja correctamente.");
            } else {
                guardarFlash(req, "danger", "El material no existe.");
            }
        } catch (DaoException e) {
            registrar(e);
            guardarFlash(req, "danger", MSG_GENERICO);
        }
        resp.sendRedirect(req.getContextPath() + "/almacen");
    }

    // ---------- auxiliares ----------

    /** Lee el formulario. El stock nunca se lee: no viene del navegador. */
    private Material leerFormulario(HttpServletRequest req, List<String> errores) {
        Material m = new Material();
        m.setIdMaterial(leerId(req.getParameter("id")));
        m.setCodigo(texto(req, "codigo"));
        m.setNombre(texto(req, "nombre"));
        m.setUnidad(texto(req, "unidad"));
        String minimo = texto(req, "stockMinimo");
        try {
            m.setStockMinimo(Integer.parseInt(minimo));
        } catch (NumberFormatException e) {
            errores.add("El stock mínimo debe ser un número entero mayor o igual a 0.");
        }
        return m;
    }

    /** Al editar, el código y el stock se toman de la BD, no del formulario. */
    /**
     * Devuelve cómo está hoy el material. Código y stock se toman de la BD (no del formulario) y el estado se copia,
     * para que la comparación de la auditoría solo muestre lo que el usuario realmente cambió.
     */
    private Optional<Material> restaurarDatosFijos(Material m) {
        Optional<Material> actual = dao.buscarPorId(m.getIdMaterial());
        actual.ifPresent(a -> {
            m.setCodigo(a.getCodigo());
            m.setStock(a.getStock());
            m.setEstado(a.isEstado());
        });
        return actual;
    }

    /** Auditoría de CREAR (antes == null) o ACTUALIZAR (con las diferencias campo por campo). */
    private void auditarGuardado(HttpServletRequest req, Material antes, Material m) {
        String quien = m.getCodigo() + " - " + m.getNombre();
        if (antes == null) {
            Auditoria.registrar(req, Auditoria.CREAR, Auditoria.MATERIAL, m.getIdMaterial(),
                    "Material registrado: " + quien, null);
        } else {
            Auditoria.registrar(req, Auditoria.ACTUALIZAR, Auditoria.MATERIAL, m.getIdMaterial(),
                    "Material actualizado: " + quien, Auditoria.diferencias(antes, m));
        }
    }

    /** minimoEscrito conserva lo que tecleó el usuario si no era un número válido. */
    private void mostrarFormulario(HttpServletRequest req, HttpServletResponse resp,
                                   Material m, String minimoEscrito, List<String> errores)
            throws ServletException, IOException {
        req.setAttribute("material", m);
        req.setAttribute("esNuevo", m.getIdMaterial() == 0);
        req.setAttribute("errores", errores);
        req.setAttribute("unidades", MaterialValidador.UNIDADES);
        req.setAttribute("stockMinimoTexto", minimoEscrito != null ? minimoEscrito.trim() : String.valueOf(m.getStockMinimo()));
        req.getRequestDispatcher(VISTA_FORM).forward(req, resp);
    }

    /** Guarda un mensaje en la sesión para mostrarlo UNA vez después de redirigir. */
    private void guardarFlash(HttpServletRequest req, String tipo, String texto) {
        HttpSession s = req.getSession();
        s.setAttribute(FLASH_TIPO, tipo);
        s.setAttribute(FLASH_TEXTO, texto);
    }

    /** Pasa el mensaje de la sesión a la petición y lo borra de la sesión. */
    private void mostrarFlash(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute(FLASH_TEXTO) == null) {
            return;
        }
        req.setAttribute("mensaje", s.getAttribute(FLASH_TEXTO));
        req.setAttribute("mensajeTipo", s.getAttribute(FLASH_TIPO));
        s.removeAttribute(FLASH_TEXTO);
        s.removeAttribute(FLASH_TIPO);
    }

    private String texto(HttpServletRequest req, String nombre) {
        String v = req.getParameter(nombre);
        return v == null ? "" : v.trim();
    }

    private int leerId(String s) {
        try {
            return s == null || s.isBlank() ? 0 : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** El detalle técnico va al log del servidor, nunca a la pantalla. */
    private void registrar(DaoException e) {
        LOG.log(Level.SEVERE, e.getMessage(), e);
    }
}

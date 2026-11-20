package pe.gob.munquinua.sigmud.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import pe.gob.munquinua.sigmud.dao.AuditoriaDAO;
import pe.gob.munquinua.sigmud.dao.CitaDAO;
import pe.gob.munquinua.sigmud.dao.ContribuyenteDAO;
import pe.gob.munquinua.sigmud.dao.impl.AuditoriaDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.CitaDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.ContribuyenteDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.ContribuyenteDAOImpl;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.exception.RegistroDuplicadoException;
import pe.gob.munquinua.sigmud.model.Cita;
import pe.gob.munquinua.sigmud.model.Contribuyente;
import pe.gob.munquinua.sigmud.model.Usuario;
import pe.gob.munquinua.sigmud.util.Auditoria;
import pe.gob.munquinua.sigmud.util.ContribuyenteValidador;
import pe.gob.munquinua.sigmud.util.ListaVista;
import pe.gob.munquinua.sigmud.model.Pagina;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Módulo Contribuyentes. GET: listar, nuevo, editar, ficha. POST: guardar, baja, reactivar.
 * El acceso por rol (ADMIN y ATENCION) lo controla AuthFilter.
 */
@WebServlet("/contribuyentes")
public class ContribuyenteServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(ContribuyenteServlet.class.getName());
    private static final String VISTA_LISTADO = "/WEB-INF/views/contribuyentes/listado.jsp";
    private static final String VISTA_FORM = "/WEB-INF/views/contribuyentes/formulario.jsp";
    private static final String VISTA_FICHA = "/WEB-INF/views/contribuyentes/ficha.jsp";
    private static final int EVENTOS_ACTIVIDAD = 10;
    private static final List<String> ESTADOS_CITA = List.of(Cita.PROGRAMADA, Cita.ATENDIDA, Cita.CANCELADA);
    private static final String FLASH_TEXTO = "flashTexto";
    private static final String FLASH_TIPO = "flashTipo";
    private static final String MSG_GENERICO = "No se pudo completar la operación. Intente nuevamente.";

    private final ContribuyenteDAO dao = new ContribuyenteDAOImpl();
    private final CitaDAO citaDao = new CitaDAOImpl();
    private final AuditoriaDAO auditoriaDao = new AuditoriaDAOImpl();

    // ---------- GET ----------

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        switch (texto(req, "accion")) {
            case "nuevo" -> {
                Contribuyente c = new Contribuyente();
                c.setTipoPersona(Contribuyente.NATURAL);
                mostrarFormulario(req, resp, c, new ArrayList<>());
            }
            case "editar" -> editar(req, resp);
            case "ficha" -> ficha(req, resp);
            default -> listar(req, resp);
        }
    }

    private void listar(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        mostrarFlash(req);
        String q = texto(req, "q");
        boolean verInactivos = "1".equals(texto(req, "inactivos"));   // por defecto solo activos
        String orden = ListaVista.orden(req);
        String dir = ListaVista.dir(req);
        List<Contribuyente> lista = List.of();
        try {
            Pagina<Contribuyente> pag = dao.listarPagina(q, verInactivos, ListaVista.pagina(req), ListaVista.tamano(req),
                    orden, dir);
            lista = pag.getItems();
            Map<String, String> filtros = new LinkedHashMap<>();
            filtros.put("q", q);
            filtros.put("inactivos", verInactivos ? "1" : "");
            ListaVista.preparar(req, pag, "/contribuyentes", filtros, ContribuyenteDAOImpl.resolverOrden(orden, dir));
        } catch (DaoException e) {
            registrar(e);
            req.setAttribute("mensaje", MSG_GENERICO);
            req.setAttribute("mensajeTipo", "danger");
        }
        req.setAttribute("lista", lista);
        req.setAttribute("q", q);
        req.setAttribute("verInactivos", verInactivos);
        req.getRequestDispatcher(VISTA_LISTADO).forward(req, resp);
    }

    private void editar(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            Optional<Contribuyente> c = dao.buscarPorId(leerId(req.getParameter("id")));
            if (c.isPresent() && c.get().isEstado()) {
                mostrarFormulario(req, resp, c.get(), new ArrayList<>());
                return;
            }
            guardarFlash(req, "danger", "El contribuyente no existe o está dado de baja.");
        } catch (DaoException e) {
            registrar(e);
            guardarFlash(req, "danger", MSG_GENERICO);
        }
        resp.sendRedirect(req.getContextPath() + "/contribuyentes");
    }

    // ---------- POST ----------

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        switch (texto(req, "accion")) {
            case "guardar" -> guardar(req, resp);
            case "baja" -> darDeBaja(req, resp);
            case "reactivar" -> reactivar(req, resp);
            default -> resp.sendRedirect(req.getContextPath() + "/contribuyentes");
        }
    }

    private void guardar(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<String> errores = new ArrayList<>();
        Contribuyente c = leerFormulario(req, errores);
        boolean esNuevo = c.getIdContribuyente() == 0;
        Optional<Contribuyente> antes = Optional.empty();   // cómo estaba antes de editar (para la auditoría)
        try {
            if (!esNuevo) {
                antes = restaurarDocumento(c);
                if (antes.isEmpty()) {
                    guardarFlash(req, "danger", "El contribuyente no existe.");
                    resp.sendRedirect(req.getContextPath() + "/contribuyentes");
                    return;
                }
            }
            errores.addAll(ContribuyenteValidador.validar(c));
            if (errores.isEmpty()) {
                persistir(c, esNuevo);
                auditarGuardado(req, antes.orElse(null), c);
                guardarFlash(req, "success", esNuevo
                        ? "Contribuyente registrado correctamente."
                        : "Contribuyente actualizado correctamente.");
                resp.sendRedirect(req.getContextPath() + "/contribuyentes"); // Post-Redirect-Get
                return;
            }
        } catch (RegistroDuplicadoException e) {
            errores.add("Ya existe un contribuyente con ese DNI/RUC");
        } catch (DaoException e) {
            registrar(e);
            errores.add(MSG_GENERICO);
        }
        mostrarFormulario(req, resp, c, errores);
    }

    private void persistir(Contribuyente c, boolean esNuevo) throws RegistroDuplicadoException {
        if (esNuevo) {
            dao.insertar(c);
        } else {
            dao.actualizar(c);
        }
    }

    private void darDeBaja(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = leerId(req.getParameter("id"));
        try {
            Optional<Contribuyente> existente = id > 0 ? dao.buscarPorId(id) : Optional.empty();
            if (existente.isPresent()) {
                dao.darDeBaja(id);
                Auditoria.registrar(req, Auditoria.BAJA, Auditoria.CONTRIBUYENTE, id,
                        "Contribuyente dado de baja: DNI/RUC " + existente.get().getDniRuc() + " - "
                        + existente.get().getNombresRazonSocial(), null);
                guardarFlash(req, "success", "Contribuyente dado de baja correctamente.");
            } else {
                guardarFlash(req, "danger", "El contribuyente no existe.");
            }
        } catch (DaoException e) {
            registrar(e);
            guardarFlash(req, "danger", MSG_GENERICO);
        }
        resp.sendRedirect(destinoTrasCambio(req, id));
    }

    /** Reactiva un contribuyente dado de baja (usa el DAO que ya existía) y lo deja en la auditoría. */
    private void reactivar(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = leerId(req.getParameter("id"));
        try {
            Optional<Contribuyente> existente = id > 0 ? dao.buscarPorId(id) : Optional.empty();
            if (existente.isEmpty()) {
                guardarFlash(req, "danger", "El contribuyente no existe.");
            } else if (existente.get().isEstado()) {
                guardarFlash(req, "warning", "El contribuyente ya está activo.");
            } else {
                dao.reactivar(id);
                Auditoria.registrar(req, Auditoria.REACTIVAR, Auditoria.CONTRIBUYENTE, id,
                        "Contribuyente reactivado: DNI/RUC " + existente.get().getDniRuc() + " - "
                        + existente.get().getNombresRazonSocial(), "estado: baja -> activo");
                guardarFlash(req, "success", "Contribuyente reactivado correctamente.");
            }
        } catch (DaoException e) {
            registrar(e);
            guardarFlash(req, "danger", MSG_GENERICO);
        }
        resp.sendRedirect(destinoTrasCambio(req, id));
    }

    /** Si el cambio se pidió desde la ficha se vuelve a la ficha; si no, al listado (Post-Redirect-Get). */
    private String destinoTrasCambio(HttpServletRequest req, int id) {
        String base = req.getContextPath() + "/contribuyentes";
        return "ficha".equals(texto(req, "volver")) && id > 0 ? base + "?accion=ficha&id=" + id : base;
    }

    // ---------- ficha ----------

    /** Ficha del contribuyente: datos, indicadores de citas, historial y (solo ADMIN) actividad de auditoría. */
    private void ficha(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String destino = req.getContextPath() + "/contribuyentes";
        int id = leerId(req.getParameter("id"));
        if (id <= 0) {
            guardarFlash(req, "danger", "El identificador del contribuyente no es válido.");
            resp.sendRedirect(destino);
            return;
        }
        try {
            Optional<Contribuyente> c = dao.buscarPorId(id);   // también los dados de baja
            if (c.isEmpty()) {
                guardarFlash(req, "danger", "El contribuyente no existe.");
                resp.sendRedirect(destino);
                return;
            }
            // Datos personales (Ley N.º 29733): cada consulta queda registrada. Mejor esfuerzo: nunca rompe la pantalla.
            Auditoria.registrar(req, Auditoria.CONSULTA_FICHA, Auditoria.CONTRIBUYENTE, id,
                    "Consulta de ficha: DNI/RUC " + c.get().getDniRuc() + " - " + c.get().getNombresRazonSocial(), null);
            mostrarFlash(req);
            cargarFicha(req, c.get());
        } catch (DaoException e) {
            registrar(e);
            guardarFlash(req, "danger", MSG_GENERICO);
            resp.sendRedirect(destino);
            return;
        }
        req.getRequestDispatcher(VISTA_FICHA).forward(req, resp);
    }

    private void cargarFicha(HttpServletRequest req, Contribuyente c) {
        String estado = texto(req, "estado");
        if (!ESTADOS_CITA.contains(estado)) {
            estado = "";   // un valor desconocido se ignora
        }
        int id = c.getIdContribuyente();
        req.setAttribute("c", c);
        req.setAttribute("resumen", citaDao.resumenPorContribuyente(id));
        req.setAttribute("proxima", citaDao.proximaCita(id));
        String orden = ListaVista.orden(req);
        String dir = ListaVista.dir(req);
        Pagina<Cita> pagCitas = citaDao.listarPorContribuyentePagina(id, estado, ListaVista.pagina(req),
                ListaVista.tamano(req), orden, dir);
        req.setAttribute("citas", pagCitas.getItems());
        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("accion", "ficha");
        filtros.put("id", String.valueOf(id));
        filtros.put("estado", estado);
        ListaVista.preparar(req, pagCitas, "/contribuyentes", filtros, CitaDAOImpl.resolverOrden(orden, dir));
        req.setAttribute("estadoFiltro", estado);
        if (Contribuyente.NATURAL.equals(c.getTipoPersona()) && c.getFechaNacimiento() != null) {
            req.setAttribute("edad", Period.between(c.getFechaNacimiento(), LocalDate.now()).getYears());
        }
        // El rol sale de la SESIÓN: la pestaña "Actividad" solo se calcula y se envía al ADMIN.
        Usuario u = (Usuario) req.getSession().getAttribute(LoginServlet.SESION_USUARIO);
        boolean admin = u != null && "ADMIN".equals(u.getRol());
        req.setAttribute("verActividad", admin);
        if (admin) {
            req.setAttribute("actividad", auditoriaDao.ultimosPorEntidad(Auditoria.CONTRIBUYENTE, id, EVENTOS_ACTIVIDAD));
        }
    }

    // ---------- auxiliares ----------

    /** Lee el formulario. Una fecha con formato inválido se agrega a los errores. */
    private Contribuyente leerFormulario(HttpServletRequest req, List<String> errores) {
        Contribuyente c = new Contribuyente();
        c.setIdContribuyente(leerId(req.getParameter("id")));
        c.setTipoPersona(texto(req, "tipoPersona"));
        c.setDniRuc(texto(req, "dniRuc"));
        c.setNombresRazonSocial(texto(req, "nombresRazonSocial"));
        c.setTelefono(texto(req, "telefono"));
        c.setEmail(texto(req, "email"));
        c.setDireccion(texto(req, "direccion"));
        String fecha = texto(req, "fechaNacimiento");
        try {
            c.setFechaNacimiento(fecha.isEmpty() ? null : LocalDate.parse(fecha));
        } catch (DateTimeParseException e) {
            errores.add("La fecha de nacimiento no es válida.");
        }
        return c;
    }

    /** Al editar, tipo y DNI/RUC no se pueden cambiar: se toman de la BD, no del formulario. */
    /**
     * Devuelve cómo está hoy el contribuyente. Tipo y DNI/RUC se toman de la BD (no del formulario), y estado y fecha de
     * registro se copian para que la comparación de la auditoría solo muestre lo que el usuario realmente cambió.
     */
    private Optional<Contribuyente> restaurarDocumento(Contribuyente c) {
        Optional<Contribuyente> actual = dao.buscarPorId(c.getIdContribuyente());
        actual.ifPresent(a -> {
            c.setTipoPersona(a.getTipoPersona());
            c.setDniRuc(a.getDniRuc());
            c.setEstado(a.isEstado());
            c.setFechaRegistro(a.getFechaRegistro());
        });
        return actual;
    }

    /** Auditoría de CREAR (antes == null) o ACTUALIZAR (con las diferencias campo por campo). */
    private void auditarGuardado(HttpServletRequest req, Contribuyente antes, Contribuyente c) {
        String quien = "DNI/RUC " + c.getDniRuc() + " - " + c.getNombresRazonSocial();
        if (antes == null) {
            Auditoria.registrar(req, Auditoria.CREAR, Auditoria.CONTRIBUYENTE, c.getIdContribuyente(),
                    "Contribuyente registrado: " + quien, null);
        } else {
            Auditoria.registrar(req, Auditoria.ACTUALIZAR, Auditoria.CONTRIBUYENTE, c.getIdContribuyente(),
                    "Contribuyente actualizado: " + quien, Auditoria.diferencias(antes, c));
        }
    }

    private void mostrarFormulario(HttpServletRequest req, HttpServletResponse resp,
                                   Contribuyente c, List<String> errores)
            throws ServletException, IOException {
        req.setAttribute("contribuyente", c);
        req.setAttribute("esNuevo", c.getIdContribuyente() == 0);
        req.setAttribute("errores", errores);
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

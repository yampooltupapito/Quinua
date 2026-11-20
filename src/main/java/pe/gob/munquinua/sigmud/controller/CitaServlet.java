package pe.gob.munquinua.sigmud.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import pe.gob.munquinua.sigmud.dao.AreaDAO;
import pe.gob.munquinua.sigmud.dao.CitaDAO;
import pe.gob.munquinua.sigmud.dao.ContribuyenteDAO;
import pe.gob.munquinua.sigmud.dao.UsuarioDAO;
import pe.gob.munquinua.sigmud.dao.impl.AreaDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.CitaDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.ContribuyenteDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.UsuarioDAOImpl;
import pe.gob.munquinua.sigmud.exception.CitaCruzadaException;
import pe.gob.munquinua.sigmud.exception.CitaEstadoException;
import pe.gob.munquinua.sigmud.exception.CitaNoEncontradaException;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.model.Area;
import pe.gob.munquinua.sigmud.model.Cita;
import pe.gob.munquinua.sigmud.model.Contribuyente;
import pe.gob.munquinua.sigmud.model.Usuario;
import pe.gob.munquinua.sigmud.util.Auditoria;
import pe.gob.munquinua.sigmud.util.CitaValidador;

/**
 * Módulo Citas. GET: listar, nuevo, editar (reprogramar). POST: guardar, atender, cancelar.
 * El acceso por rol (ADMIN y ATENCION) lo controla AuthFilter.
 */
@WebServlet("/citas")
public class CitaServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(CitaServlet.class.getName());
    private static final String VISTA_LISTADO = "/WEB-INF/views/citas/listado.jsp";
    private static final String VISTA_FORM = "/WEB-INF/views/citas/formulario.jsp";
    private static final String FLASH_TEXTO = "flashTexto";
    private static final String FLASH_TIPO = "flashTipo";
    private static final String MSG_GENERICO = "No se pudo completar la operación. Intente nuevamente.";

    private final CitaDAO citaDao = new CitaDAOImpl();
    private final AreaDAO areaDao = new AreaDAOImpl();
    private final UsuarioDAO usuarioDao = new UsuarioDAOImpl();
    private final ContribuyenteDAO contribuyenteDao = new ContribuyenteDAOImpl();
    private final CitaValidador validador = new CitaValidador();

    // ---------- GET ----------

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        switch (texto(req, "accion")) {
            case "nuevo" -> nuevo(req, resp);
            case "editar" -> editar(req, resp);
            default -> listar(req, resp);
        }
    }

    private void listar(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        mostrarFlash(req);
        LocalDate fecha = leerFecha(texto(req, "fecha"), LocalDate.now());
        Integer area = areaSeleccionada(req);
        List<Cita> lista = List.of();
        List<Area> areas = List.of();
        try {
            lista = citaDao.listarPorFecha(fecha, area);
            areas = areaDao.listarActivas();
        } catch (DaoException e) {
            registrar(e);
            req.setAttribute("mensaje", MSG_GENERICO);
            req.setAttribute("mensajeTipo", "danger");
        }
        req.setAttribute("lista", lista);
        req.setAttribute("areas", areas);
        req.setAttribute("fecha", fecha);
        req.setAttribute("areaSel", area);
        req.setAttribute("fechaAnterior", fecha.minusDays(1).toString());
        req.setAttribute("fechaSiguiente", fecha.plusDays(1).toString());
        req.setAttribute("esFutura", fecha.isAfter(LocalDate.now()));
        req.getRequestDispatcher(VISTA_LISTADO).forward(req, resp);
    }

    private void nuevo(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Cita c = new Cita();
        Usuario u = usuarioActual(req);
        if ("ATENCION".equals(u.getRol()) && u.getIdArea() != null) {
            c.setIdArea(u.getIdArea());          // un funcionario de atención empieza con su propia área
        }
        preseleccionarContribuyente(req, c);
        mostrarFormulario(req, resp, c, new ArrayList<>());
    }

    /**
     * Si llega ?contribuyente=ID (por ejemplo desde la ficha) y existe y está activo, el formulario ya trae a ese
     * contribuyente elegido y bloqueado. Si no es válido se ignora y se muestra el formulario normal.
     */
    private void preseleccionarContribuyente(HttpServletRequest req, Cita c) {
        int id = leerId(req.getParameter("contribuyente"));
        if (id <= 0) {
            return;
        }
        try {
            if (contribuyenteDao.buscarPorId(id).filter(Contribuyente::isEstado).isPresent()) {
                c.setIdContribuyente(id);
                req.setAttribute("contribuyenteFijo", true);
            }
        } catch (DaoException e) {
            registrar(e);   // si falla la consulta, se muestra el formulario normal
        }
    }

    private void editar(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String destino = req.getContextPath() + "/citas";
        try {
            Optional<Cita> c = citaDao.buscarPorId(leerId(req.getParameter("id")));
            if (c.isPresent() && Cita.PROGRAMADA.equals(c.get().getEstado())) {
                mostrarFormulario(req, resp, c.get(), new ArrayList<>());
                return;
            }
            guardarFlash(req, "danger", c.isPresent()
                    ? "Solo se puede reprogramar una cita PROGRAMADA."
                    : "La cita no existe.");
            destino = urlListado(req, c.map(Cita::getFecha).orElse(LocalDate.now()), c.map(Cita::getIdArea).orElse(null));
        } catch (DaoException e) {
            registrar(e);
            guardarFlash(req, "danger", MSG_GENERICO);
        }
        resp.sendRedirect(destino);
    }

    // ---------- POST ----------

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        switch (texto(req, "accion")) {
            case "guardar" -> guardar(req, resp);
            case "atender" -> cambiarEstado(req, resp, Cita.ATENDIDA, "Cita marcada como atendida.", Auditoria.CITA_ATENDER);
            case "cancelar" -> cambiarEstado(req, resp, Cita.CANCELADA, "Cita cancelada. El horario quedó libre.", Auditoria.CITA_CANCELAR);
            default -> resp.sendRedirect(req.getContextPath() + "/citas");
        }
    }

    private void guardar(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<String> errores = new ArrayList<>();
        Cita c = leerFormulario(req, errores);
        boolean esNuevo = c.getIdCita() == 0;
        Optional<Cita> antes = Optional.empty();   // cómo estaba antes de reprogramar (para la auditoría)
        try {
            if (!esNuevo) {
                antes = restaurarDatosFijos(c);
                if (antes.isEmpty()) {
                    guardarFlash(req, "danger", "La cita no existe o ya no está PROGRAMADA.");
                    resp.sendRedirect(req.getContextPath() + "/citas");
                    return;
                }
            }
            errores.addAll(validador.validar(c));
            if (errores.isEmpty()) {
                persistir(c, esNuevo);
                auditarGuardado(req, antes.orElse(null), c);
                guardarFlash(req, "success", esNuevo ? "Cita registrada correctamente."
                        : "Cita reprogramada correctamente.");
                resp.sendRedirect(urlListado(req, c.getFecha(), c.getIdArea())); // Post-Redirect-Get
                return;
            }
        } catch (CitaCruzadaException | CitaEstadoException | CitaNoEncontradaException e) {
            errores.add(e.getMessage());
        } catch (DaoException e) {
            registrar(e);
            errores.add(MSG_GENERICO);
        }
        mostrarFormulario(req, resp, c, errores);
    }

    private void persistir(Cita c, boolean esNuevo)
            throws CitaCruzadaException, CitaEstadoException, CitaNoEncontradaException {
        if (esNuevo) {
            citaDao.insertar(c);
        } else {
            citaDao.reprogramar(c.getIdCita(), c.getFecha(), c.getHora(), c.getVentanilla());
        }
    }

    private void cambiarEstado(HttpServletRequest req, HttpServletResponse resp, String nuevoEstado,
                               String mensajeOk, String accionAuditoria) throws IOException {
        LocalDate fecha = leerFecha(texto(req, "fecha"), LocalDate.now());
        Integer area = leerEnteroOpcional(texto(req, "area"));
        try {
            int id = leerId(req.getParameter("id"));
            Optional<Cita> cita = citaDao.buscarPorId(id);          // para describir la cita en la auditoría
            citaDao.cambiarEstado(id, nuevoEstado);
            Auditoria.registrar(req, accionAuditoria, Auditoria.CITA, id,
                    "Cita " + (Cita.ATENDIDA.equals(nuevoEstado) ? "atendida" : "cancelada") + ": "
                    + cita.map(this::resumen).orElse("id " + id), "estado: PROGRAMADA -> " + nuevoEstado);
            guardarFlash(req, "success", mensajeOk);
        } catch (CitaEstadoException | CitaNoEncontradaException e) {
            guardarFlash(req, "danger", e.getMessage());
        } catch (DaoException e) {
            registrar(e);
            guardarFlash(req, "danger", MSG_GENERICO);
        }
        resp.sendRedirect(urlListado(req, fecha, area));
    }

    // ---------- auxiliares ----------

    /** Lee el formulario. Fecha, hora o ventanilla con formato inválido se agregan a los errores. */
    private Cita leerFormulario(HttpServletRequest req, List<String> errores) {
        Cita c = new Cita();
        c.setIdCita(leerId(req.getParameter("id")));
        c.setIdContribuyente(leerId(req.getParameter("idContribuyente")));
        c.setIdArea(leerId(req.getParameter("idArea")));
        c.setIdFuncionario(leerId(req.getParameter("idFuncionario")));
        c.setVentanilla(leerId(req.getParameter("ventanilla")));
        c.setMotivo(texto(req, "motivo"));
        try {
            String f = texto(req, "fecha");
            c.setFecha(f.isEmpty() ? null : LocalDate.parse(f));
        } catch (DateTimeParseException e) {
            errores.add("La fecha no es válida.");
        }
        try {
            String h = texto(req, "hora");
            c.setHora(h.isEmpty() ? null : LocalTime.parse(h));
        } catch (DateTimeParseException e) {
            errores.add("La hora no es válida.");
        }
        return c;
    }

    /** Al reprogramar solo cambian fecha, hora y ventanilla: lo demás se toma de la BD. */
    /**
     * Devuelve cómo está hoy la cita (solo si sigue PROGRAMADA; si no, vacío). Se copia todo lo que no cambia al
     * reprogramar, para que la comparación de la auditoría solo muestre fecha, hora y ventanilla.
     */
    private Optional<Cita> restaurarDatosFijos(Cita c) {
        Optional<Cita> actual = citaDao.buscarPorId(c.getIdCita());
        if (actual.isEmpty() || !Cita.PROGRAMADA.equals(actual.get().getEstado())) {
            return Optional.empty();
        }
        Cita a = actual.get();
        c.setIdContribuyente(a.getIdContribuyente());
        c.setIdArea(a.getIdArea());
        c.setIdFuncionario(a.getIdFuncionario());
        c.setMotivo(a.getMotivo());
        c.setEstado(a.getEstado());
        c.setNombreContribuyente(a.getNombreContribuyente());
        c.setDniRuc(a.getDniRuc());
        c.setNombreArea(a.getNombreArea());
        c.setNombreFuncionario(a.getNombreFuncionario());
        return actual;
    }

    /** Auditoría de CITA_CREAR (antes == null) o CITA_REPROGRAMAR (con las diferencias campo por campo). */
    private void auditarGuardado(HttpServletRequest req, Cita antes, Cita c) {
        if (antes == null) {
            Auditoria.registrar(req, Auditoria.CITA_CREAR, Auditoria.CITA, c.getIdCita(),
                    "Cita registrada: " + resumen(c),
                    "contribuyente: " + c.getIdContribuyente() + "; area: " + c.getIdArea() + "; funcionario: "
                    + c.getIdFuncionario() + "; ventanilla: " + c.getVentanilla() + "; motivo: " + c.getMotivo());
        } else {
            Auditoria.registrar(req, Auditoria.CITA_REPROGRAMAR, Auditoria.CITA, c.getIdCita(),
                    "Cita reprogramada: " + c.getNombreContribuyente(), Auditoria.diferencias(antes, c));
        }
    }

    /** Texto corto de una cita: contribuyente, fecha y hora. */
    private String resumen(Cita c) {
        String quien = c.getNombreContribuyente() == null ? "contribuyente " + c.getIdContribuyente()
                : c.getNombreContribuyente();
        return quien + " el " + c.getFecha() + " a las " + c.getHora();
    }

    private void mostrarFormulario(HttpServletRequest req, HttpServletResponse resp, Cita c, List<String> errores)
            throws ServletException, IOException {
        try {
            if (c.getIdContribuyente() > 0) {   // para mostrar el nombre del contribuyente elegido
                contribuyenteDao.buscarPorId(c.getIdContribuyente()).ifPresent(ct -> {
                    c.setNombreContribuyente(ct.getNombresRazonSocial());
                    c.setDniRuc(ct.getDniRuc());
                });
            }
            req.setAttribute("areas", areaDao.listarActivas());
            req.setAttribute("funcionarios", c.getIdArea() > 0
                    ? usuarioDao.listarFuncionariosPorArea(c.getIdArea()) : List.of());
        } catch (DaoException e) {
            registrar(e);
            errores.add(MSG_GENERICO);
        }
        req.setAttribute("cita", c);
        req.setAttribute("esNuevo", c.getIdCita() == 0);
        req.setAttribute("errores", errores);
        req.getRequestDispatcher(VISTA_FORM).forward(req, resp);
    }

    /** Área del filtro: si no viene el parámetro, ATENCION ve su área y ADMIN ve todas; "area=" vacío = todas. */
    private Integer areaSeleccionada(HttpServletRequest req) {
        String p = req.getParameter("area");
        if (p == null) {
            Usuario u = usuarioActual(req);
            return "ATENCION".equals(u.getRol()) ? u.getIdArea() : null;
        }
        return leerEnteroOpcional(p.trim());
    }

    /** Dirección del listado con fecha y área (valores ya validados, no texto del usuario). */
    private String urlListado(HttpServletRequest req, LocalDate fecha, Integer area) {
        return req.getContextPath() + "/citas?fecha=" + fecha + "&area=" + (area == null ? "" : area);
    }

    private Usuario usuarioActual(HttpServletRequest req) {
        return (Usuario) req.getSession().getAttribute(LoginServlet.SESION_USUARIO);
    }

    private void guardarFlash(HttpServletRequest req, String tipo, String texto) {
        HttpSession s = req.getSession();
        s.setAttribute(FLASH_TIPO, tipo);
        s.setAttribute(FLASH_TEXTO, texto);
    }

    /** Pasa el mensaje de la sesión a la petición y lo borra: se muestra una sola vez. */
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

    private LocalDate leerFecha(String texto, LocalDate porDefecto) {
        try {
            return texto.isEmpty() ? porDefecto : LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            return porDefecto;
        }
    }

    private Integer leerEnteroOpcional(String s) {
        int n = leerId(s);
        return n > 0 ? n : null;
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

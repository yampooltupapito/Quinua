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
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import pe.gob.munquinua.sigmud.dao.AreaDAO;
import pe.gob.munquinua.sigmud.dao.UsuarioDAO;
import pe.gob.munquinua.sigmud.dao.impl.AreaDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.UsuarioDAOImpl;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.exception.RegistroDuplicadoException;
import pe.gob.munquinua.sigmud.exception.UltimoAdminException;
import pe.gob.munquinua.sigmud.model.Pagina;
import pe.gob.munquinua.sigmud.model.Usuario;
import pe.gob.munquinua.sigmud.util.Auditoria;
import pe.gob.munquinua.sigmud.util.ListaVista;
import pe.gob.munquinua.sigmud.util.Seguridad;
import pe.gob.munquinua.sigmud.util.UsuarioValidador;
import pe.gob.munquinua.sigmud.util.Validador;

/**
 * Gestión de usuarios (solo ADMIN: AuthFilter lo exige por ruta). GET: listar (filtros por texto, rol y estado),
 * nuevo, editar. POST: guardar y estado (activar/desactivar). Un usuario NUNCA se borra, solo se desactiva.
 *
 * Reglas verificadas aquí en el servidor (no solo en pantalla): un ADMIN no puede desactivarse a sí mismo ni cambiar
 * su propio rol; siempre queda al menos un ADMIN activo (lo garantiza también el DAO, con una transacción);
 * ATENCION exige un área; el nombre de usuario es único.
 */
@WebServlet("/usuarios")
public class UsuarioServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(UsuarioServlet.class.getName());
    private static final String VISTA_LISTADO = "/WEB-INF/views/usuarios/listado.jsp";
    private static final String VISTA_FORM = "/WEB-INF/views/usuarios/formulario.jsp";
    private static final String FLASH_TEXTO = "flashTexto";
    private static final String FLASH_TIPO = "flashTipo";
    /** Contraseña temporal recién generada: se guarda en la sesión solo hasta que el listado la muestre (una vez). */
    private static final String CRED_USUARIO = "credencialUsuario";
    private static final String CRED_PASSWORD = "credencialPassword";
    private static final String MSG_GENERICO = "No se pudo completar la operación. Intente nuevamente.";

    private final UsuarioDAO dao = new UsuarioDAOImpl();
    private final AreaDAO areaDao = new AreaDAOImpl();

    // ---------- GET ----------

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        switch (texto(req, "accion")) {
            case "nuevo" -> {
                Usuario u = new Usuario();
                u.setRol("ATENCION");
                mostrarFormulario(req, resp, u, new ArrayList<>());
            }
            case "editar" -> editar(req, resp);
            default -> listar(req, resp);
        }
    }

    private void listar(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        mostrarFlash(req);
        String q = texto(req, "q");
        String rol = UsuarioValidador.ROLES.contains(texto(req, "rol")) ? texto(req, "rol") : "";
        String estado = texto(req, "estado");
        Boolean activo = "1".equals(estado) ? Boolean.TRUE : "0".equals(estado) ? Boolean.FALSE : null;
        String orden = ListaVista.orden(req);
        String dir = ListaVista.dir(req);
        List<Usuario> lista = List.of();
        try {
            Pagina<Usuario> pag = dao.listarPagina(q, rol, activo, ListaVista.pagina(req), ListaVista.tamano(req), orden, dir);
            lista = pag.getItems();
            Map<String, String> filtros = new LinkedHashMap<>();
            filtros.put("q", q);
            filtros.put("rol", rol);
            filtros.put("estado", "1".equals(estado) || "0".equals(estado) ? estado : "");
            ListaVista.preparar(req, pag, "/usuarios", filtros, UsuarioDAOImpl.resolverOrden(orden, dir));
        } catch (DaoException e) {
            registrar(e);
            req.setAttribute("mensaje", MSG_GENERICO);
            req.setAttribute("mensajeTipo", "danger");
        }
        req.setAttribute("lista", lista);
        req.setAttribute("q", q);
        req.setAttribute("rolSel", rol);
        req.setAttribute("estadoSel", "1".equals(estado) || "0".equals(estado) ? estado : "");
        req.setAttribute("roles", UsuarioValidador.ROLES);
        req.setAttribute("miId", usuarioActual(req).getIdUsuario());
        req.getRequestDispatcher(VISTA_LISTADO).forward(req, resp);
    }

    private void editar(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Optional<Usuario> u = dao.buscarPorId(leerId(req.getParameter("id")));
            if (u.isPresent()) {
                mostrarFormulario(req, resp, u.get(), new ArrayList<>());
                return;
            }
            guardarFlash(req, "danger", "El usuario no existe.");
        } catch (DaoException e) {
            registrar(e);
            guardarFlash(req, "danger", MSG_GENERICO);
        }
        resp.sendRedirect(req.getContextPath() + "/usuarios");
    }

    // ---------- POST ----------

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        switch (texto(req, "accion")) {
            case "guardar" -> guardar(req, resp);
            case "estado" -> cambiarEstado(req, resp);
            default -> resp.sendRedirect(req.getContextPath() + "/usuarios");
        }
    }

    private void guardar(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Usuario sesion = usuarioActual(req);
        Usuario u = leerFormulario(req);
        boolean esNuevo = u.getIdUsuario() == 0;
        List<String> errores = new ArrayList<>();
        Optional<Usuario> antes = Optional.empty();   // cómo estaba antes de editar (para la auditoría)
        try {
            if (!esNuevo) {
                antes = aplicarReglasDeEdicion(u, sesion, errores);
                if (antes.isEmpty()) {
                    guardarFlash(req, "danger", "El usuario no existe.");
                    resp.sendRedirect(req.getContextPath() + "/usuarios");
                    return;
                }
            }
            errores.addAll(UsuarioValidador.validar(u, esNuevo));
            validarAreaYUnicidad(u, esNuevo, errores);
            if (errores.isEmpty()) {
                persistir(req, u, esNuevo);
                auditarGuardado(req, antes.orElse(null), u);
                resp.sendRedirect(req.getContextPath() + "/usuarios"); // Post-Redirect-Get
                return;
            }
        } catch (RegistroDuplicadoException e) {
            errores.add("Ya existe un usuario con ese nombre de usuario.");
        } catch (UltimoAdminException e) {
            errores.add(e.getMessage());
        } catch (DaoException e) {
            registrar(e);
            errores.add(MSG_GENERICO);
        }
        mostrarFormulario(req, resp, u, errores);
    }

    /**
     * En una edición el nombre de usuario no cambia y un ADMIN no puede cambiar su propio rol.
     * @return false si el usuario no existe
     */
    private Optional<Usuario> aplicarReglasDeEdicion(Usuario u, Usuario sesion, List<String> errores) {
        Optional<Usuario> actual = dao.buscarPorId(u.getIdUsuario());
        if (actual.isEmpty()) {
            return actual;
        }
        Usuario a = actual.get();
        u.setUsername(a.getUsername());
        // Lo que el formulario no edita se copia, para que la auditoría solo muestre lo que realmente cambió
        u.setEstado(a.isEstado());
        u.setIntentosFallidos(a.getIntentosFallidos());
        u.setUltimoAcceso(a.getUltimoAcceso());
        u.setDebeCambiarPassword(a.isDebeCambiarPassword());
        u.setNombreArea(a.getNombreArea());
        if (a.getIdUsuario() == sesion.getIdUsuario() && !a.getRol().equals(u.getRol())) {
            errores.add("No puede cambiar su propio rol.");
            u.setRol(a.getRol());
        }
        return actual;
    }

    /** Auditoría de USUARIO_CREAR (antes == null) o USUARIO_ACTUALIZAR (diferencias; nunca contraseñas ni hashes). */
    private void auditarGuardado(HttpServletRequest req, Usuario antes, Usuario u) {
        if (antes == null) {
            Auditoria.registrar(req, Auditoria.USUARIO_CREAR, Auditoria.USUARIO, u.getIdUsuario(),
                    "Usuario creado: " + u.getUsername() + " (" + u.getRol() + ")",
                    "nombres: " + u.getNombres() + "; rol: " + u.getRol() + "; idArea: " + u.getIdArea()
                    + "; contraseña temporal asignada (no se guarda)");
        } else {
            Auditoria.registrar(req, Auditoria.USUARIO_ACTUALIZAR, Auditoria.USUARIO, u.getIdUsuario(),
                    "Usuario actualizado: " + u.getUsername(), Auditoria.diferencias(antes, u));
        }
    }

    private void validarAreaYUnicidad(Usuario u, boolean esNuevo, List<String> errores) {
        if (u.getIdArea() != null && areaDao.listarActivas().stream().noneMatch(a -> a.getIdArea() == u.getIdArea())) {
            errores.add("El área seleccionada no es válida.");
        }
        if (esNuevo && Validador.usuarioValido(u.getUsername()) && dao.existeUsername(u.getUsername(), 0)) {
            errores.add("Ya existe un usuario con ese nombre de usuario.");
        }
    }

    /** Inserta (con una contraseña temporal) o actualiza, y deja el mensaje de éxito para mostrarlo una sola vez. */
    private void persistir(HttpServletRequest req, Usuario u, boolean esNuevo)
            throws RegistroDuplicadoException, UltimoAdminException {
        if (!esNuevo) {
            dao.actualizar(u);
            guardarFlash(req, "success", "Usuario actualizado correctamente.");
            return;
        }
        String temporal = Seguridad.generarPasswordTemporal();      // nunca se escribe en el log ni se guarda en texto plano
        dao.insertar(u, Seguridad.hashear(temporal));
        HttpSession s = req.getSession();
        s.setAttribute(CRED_USUARIO, u.getUsername());
        s.setAttribute(CRED_PASSWORD, temporal);
        guardarFlash(req, "success", "Usuario creado. Entregue la contraseña temporal; deberá cambiarla al ingresar.");
        LOG.log(Level.INFO, "Usuario creado: usuario={0} por administrador={1}",
                new Object[] {u.getUsername(), usuarioActual(req).getUsername()});
    }

    private void cambiarEstado(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Usuario sesion = usuarioActual(req);
        int id = leerId(req.getParameter("id"));
        boolean activo = "1".equals(texto(req, "activo"));
        try {
            if (id == sesion.getIdUsuario() && !activo) {
                guardarFlash(req, "danger", "No puede desactivar su propia cuenta.");
            } else if (dao.buscarPorId(id).isEmpty()) {
                guardarFlash(req, "danger", "El usuario no existe.");
            } else {
                Usuario objetivo = dao.buscarPorId(id).orElseThrow();
                dao.cambiarEstado(id, activo);
                Auditoria.registrar(req, activo ? Auditoria.USUARIO_ACTIVAR : Auditoria.USUARIO_DESACTIVAR,
                        Auditoria.USUARIO, id, (activo ? "Usuario activado: " : "Usuario desactivado: ") + objetivo.getUsername(),
                        "estado: " + (activo ? "inactivo -> activo" : "activo -> inactivo"));
                guardarFlash(req, "success", activo ? "Usuario activado." : "Usuario desactivado.");
                LOG.log(Level.INFO, "Usuario {0}: id={1} por administrador={2}",
                        new Object[] {activo ? "activado" : "desactivado", id, sesion.getUsername()});
            }
        } catch (UltimoAdminException e) {
            guardarFlash(req, "danger", e.getMessage());
        } catch (DaoException e) {
            registrar(e);
            guardarFlash(req, "danger", MSG_GENERICO);
        }
        resp.sendRedirect(req.getContextPath() + "/usuarios");
    }

    // ---------- auxiliares ----------

    private Usuario leerFormulario(HttpServletRequest req) {
        Usuario u = new Usuario();
        u.setIdUsuario(leerId(req.getParameter("id")));
        u.setUsername(texto(req, "username"));
        u.setNombres(texto(req, "nombres"));
        u.setRol(texto(req, "rol"));
        int area = leerId(req.getParameter("idArea"));
        u.setIdArea(area > 0 ? area : null);
        return u;
    }

    private void mostrarFormulario(HttpServletRequest req, HttpServletResponse resp, Usuario u, List<String> errores)
            throws ServletException, IOException {
        try {
            req.setAttribute("areas", areaDao.listarActivas());
        } catch (DaoException e) {
            registrar(e);
            errores.add(MSG_GENERICO);
        }
        req.setAttribute("usuario", u);
        req.setAttribute("esNuevo", u.getIdUsuario() == 0);
        req.setAttribute("esMiCuenta", u.getIdUsuario() != 0 && u.getIdUsuario() == usuarioActual(req).getIdUsuario());
        req.setAttribute("errores", errores);
        req.setAttribute("roles", UsuarioValidador.ROLES);
        req.getRequestDispatcher(VISTA_FORM).forward(req, resp);
    }

    private Usuario usuarioActual(HttpServletRequest req) {
        return (Usuario) req.getSession().getAttribute(LoginServlet.SESION_USUARIO);
    }

    private void guardarFlash(HttpServletRequest req, String tipo, String texto) {
        HttpSession s = req.getSession();
        s.setAttribute(FLASH_TIPO, tipo);
        s.setAttribute(FLASH_TEXTO, texto);
    }

    /** Pasa a la petición el mensaje y, si existe, la contraseña temporal, y los borra de la sesión: se muestran UNA vez. */
    private void mostrarFlash(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) {
            return;
        }
        if (s.getAttribute(FLASH_TEXTO) != null) {
            req.setAttribute("mensaje", s.getAttribute(FLASH_TEXTO));
            req.setAttribute("mensajeTipo", s.getAttribute(FLASH_TIPO));
            s.removeAttribute(FLASH_TEXTO);
            s.removeAttribute(FLASH_TIPO);
        }
        if (s.getAttribute(CRED_PASSWORD) != null) {
            req.setAttribute("credencialUsuario", s.getAttribute(CRED_USUARIO));
            req.setAttribute("credencialPassword", s.getAttribute(CRED_PASSWORD));
            s.removeAttribute(CRED_USUARIO);
            s.removeAttribute(CRED_PASSWORD);
        }
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

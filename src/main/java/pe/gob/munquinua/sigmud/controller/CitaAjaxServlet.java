package pe.gob.munquinua.sigmud.controller;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import pe.gob.munquinua.sigmud.dao.CitaDAO;
import pe.gob.munquinua.sigmud.dao.ContribuyenteDAO;
import pe.gob.munquinua.sigmud.dao.UsuarioDAO;
import pe.gob.munquinua.sigmud.dao.impl.CitaDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.ContribuyenteDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.UsuarioDAOImpl;
import pe.gob.munquinua.sigmud.exception.DaoException;
import pe.gob.munquinua.sigmud.model.Cita;
import pe.gob.munquinua.sigmud.util.CitaValidador;

/**
 * Consultas Ajax del formulario de citas (todas GET, respuesta JSON UTF-8):
 * contribuyentes (autocompletado), funcionarios (por área) y horas (turnos del día).
 * El acceso (ADMIN y ATENCION) lo controla AuthFilter para toda la ruta /citas/*.
 */
@WebServlet("/citas/ajax")
public class CitaAjaxServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(CitaAjaxServlet.class.getName());
    private static final Gson GSON = new Gson();
    private static final Pattern ENTERO = Pattern.compile("^\\d{1,9}$");
    private static final int MIN_BUSQUEDA = 3;
    private static final int MAX_RESULTADOS = 10;
    private static final String MSG_GENERICO = "No se pudo completar la operación. Intente nuevamente.";

    private final ContribuyenteDAO contribuyenteDao = new ContribuyenteDAOImpl();
    private final UsuarioDAO usuarioDao = new UsuarioDAOImpl();
    private final CitaDAO citaDao = new CitaDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            switch (texto(req, "accion")) {
                case "contribuyentes" -> responder(resp, 200, contribuyentes(req));
                case "funcionarios" -> responder(resp, 200, funcionarios(req));
                case "horas" -> responder(resp, 200, horas(req));
                default -> responder(resp, 200, falla("Acción no válida."));
            }
        } catch (DaoException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e); // detalle solo al log
            responder(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, falla(MSG_GENERICO));
        }
    }

    // ---------- consultas ----------

    /** Hasta 10 contribuyentes ACTIVOS por nombre o DNI/RUC; mínimo 3 caracteres. */
    private Map<String, Object> contribuyentes(HttpServletRequest req) {
        String q = texto(req, "q");
        if (q.length() < MIN_BUSQUEDA) {
            return falla("Escriba al menos " + MIN_BUSQUEDA + " caracteres para buscar.");
        }
        if (q.length() > 100) {
            return falla("El texto de búsqueda es demasiado largo.");
        }
        List<Map<String, Object>> datos = contribuyenteDao.buscar(q).stream().limit(MAX_RESULTADOS)
                .map(c -> fila("id", c.getIdContribuyente(), "dniRuc", c.getDniRuc(),
                        "nombre", c.getNombresRazonSocial()))
                .collect(Collectors.toList());
        return exito(datos, null);
    }

    private Map<String, Object> funcionarios(HttpServletRequest req) {
        int area = entero(req.getParameter("area"));
        if (area <= 0) {
            return falla("El área no es válida.");
        }
        List<Map<String, Object>> datos = usuarioDao.listarFuncionariosPorArea(area).stream()
                .map(u -> fila("id", u.getIdUsuario(), "nombre", u.getNombres()))
                .collect(Collectors.toList());
        return exito(datos, datos.isEmpty() ? "Esta área no tiene funcionarios activos." : null);
    }

    /** Todos los turnos del día (08:00 a 16:30 cada 30 min) marcados como libres u ocupados. */
    private Map<String, Object> horas(HttpServletRequest req) {
        int funcionario = entero(req.getParameter("funcionario"));
        String textoFecha = texto(req, "fecha");
        String textoExcluir = texto(req, "excluir");
        int excluir = textoExcluir.isEmpty() ? 0 : entero(textoExcluir);
        if (funcionario <= 0 || excluir < 0) {
            return falla("El funcionario o la cita indicados no son válidos.");
        }
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(textoFecha);
        } catch (DateTimeParseException e) {
            return falla("La fecha no es válida (use AAAA-MM-DD).");
        }
        if (fecha.isBefore(LocalDate.now())) {
            return exito(new ArrayList<>(), "No se pueden agendar citas en fechas pasadas.");
        }
        if (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return exito(new ArrayList<>(), "Solo se atiende de lunes a viernes.");
        }
        return exito(turnos(funcionario, fecha, excluir), null);
    }

    private List<Map<String, Object>> turnos(int funcionario, LocalDate fecha, int excluir) {
        Set<LocalTime> ocupadas = new java.util.HashSet<>(citaDao.horasOcupadas(funcionario, fecha));
        if (excluir > 0) {   // la cita que se reprograma no debe marcar su propio turno como ocupado
            citaDao.buscarPorId(excluir)
                    .filter(c -> Cita.PROGRAMADA.equals(c.getEstado()) && c.getIdFuncionario() == funcionario
                            && c.getFecha().equals(fecha))
                    .ifPresent(c -> ocupadas.remove(c.getHora()));
        }
        LocalTime ahora = LocalDateTime.now().toLocalTime();
        boolean esHoy = fecha.equals(LocalDate.now());
        List<Map<String, Object>> lista = new ArrayList<>();
        for (LocalTime h = CitaValidador.HORA_INICIO; !h.isAfter(CitaValidador.HORA_FIN);
             h = h.plusMinutes(CitaValidador.INTERVALO_MINUTOS)) {
            boolean libre = !ocupadas.contains(h) && !(esHoy && h.isBefore(ahora));  // hora pasada: no disponible
            lista.add(fila("hora", h.toString(), "libre", libre));
        }
        return lista;
    }

    // ---------- respuesta ----------

    private Map<String, Object> exito(List<Map<String, Object>> datos, String mensaje) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        r.put("datos", datos);
        if (mensaje != null) {
            r.put("mensaje", mensaje);
        }
        return r;
    }

    private Map<String, Object> falla(String mensaje) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", false);
        r.put("mensaje", mensaje);
        return r;
    }

    /** Arma una fila JSON con pares clave, valor, clave, valor... */
    private Map<String, Object> fila(Object... paresClaveValor) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < paresClaveValor.length; i += 2) {
            m.put((String) paresClaveValor[i], paresClaveValor[i + 1]);
        }
        return m;
    }

    private void responder(HttpServletResponse resp, int estado, Map<String, Object> cuerpo) throws IOException {
        resp.setStatus(estado);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(GSON.toJson(cuerpo));
    }

    private String texto(HttpServletRequest req, String nombre) {
        String v = req.getParameter(nombre);
        return v == null ? "" : v.trim();
    }

    /** Entero sin signo; -1 si no es válido. */
    private int entero(String s) {
        return s != null && ENTERO.matcher(s.trim()).matches() ? Integer.parseInt(s.trim()) : -1;
    }
}

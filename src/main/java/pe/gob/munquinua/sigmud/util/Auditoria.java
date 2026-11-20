package pe.gob.munquinua.sigmud.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import pe.gob.munquinua.sigmud.controller.LoginServlet;
import pe.gob.munquinua.sigmud.dao.AuditoriaDAO;
import pe.gob.munquinua.sigmud.dao.impl.AuditoriaDAOImpl;
import pe.gob.munquinua.sigmud.model.RegistroAuditoria;
import pe.gob.munquinua.sigmud.model.Usuario;

/**
 * Auditoría de cambios: registra quién hizo qué y cuándo. Los servlets llaman a registrar(...) justo después de que
 * la operación tenga éxito. Es de "mejor esfuerzo": si guardar el registro falla, el error solo se escribe en el
 * log del servidor y la operación del usuario NO se interrumpe (la única excepción es MOVIMIENTO_STOCK, que se
 * registra dentro de la transacción del movimiento: ver MovimientoDAOImpl).
 *
 * Todo texto se limpia antes de guardarse (sin caracteres de control ni saltos de línea, con la longitud máxima de
 * cada columna). NUNCA se guardan contraseñas, hashes ni contraseñas temporales.
 */
public final class Auditoria {

    // ---- Acciones (columna accion, máximo 30 caracteres) ----
    public static final String LOGIN_OK = "LOGIN_OK";
    public static final String LOGIN_FALLIDO = "LOGIN_FALLIDO";
    public static final String CUENTA_BLOQUEADA = "CUENTA_BLOQUEADA";
    public static final String LOGOUT = "LOGOUT";
    public static final String CREAR = "CREAR";
    public static final String ACTUALIZAR = "ACTUALIZAR";
    public static final String BAJA = "BAJA";
    public static final String REACTIVAR = "REACTIVAR";
    public static final String MOVIMIENTO_STOCK = "MOVIMIENTO_STOCK";
    public static final String CITA_CREAR = "CITA_CREAR";
    public static final String CITA_REPROGRAMAR = "CITA_REPROGRAMAR";
    public static final String CITA_ATENDER = "CITA_ATENDER";
    public static final String CITA_CANCELAR = "CITA_CANCELAR";
    public static final String USUARIO_CREAR = "USUARIO_CREAR";
    public static final String USUARIO_ACTUALIZAR = "USUARIO_ACTUALIZAR";
    public static final String USUARIO_ACTIVAR = "USUARIO_ACTIVAR";
    public static final String USUARIO_DESACTIVAR = "USUARIO_DESACTIVAR";
    public static final String PASSWORD_RESTABLECIDA = "PASSWORD_RESTABLECIDA";
    public static final String PASSWORD_CAMBIADA = "PASSWORD_CAMBIADA";
    public static final String USUARIO_DESBLOQUEADO = "USUARIO_DESBLOQUEADO";
    /** Consulta de la ficha de un contribuyente (datos personales protegidos por la Ley N.º 29733). */
    public static final String CONSULTA_FICHA = "CONSULTA_FICHA";

    /** Todas las acciones (para el filtro de la pantalla de auditoría). */
    public static final List<String> ACCIONES = List.of(LOGIN_OK, LOGIN_FALLIDO, CUENTA_BLOQUEADA, LOGOUT, CREAR,
            ACTUALIZAR, BAJA, REACTIVAR, MOVIMIENTO_STOCK, CITA_CREAR, CITA_REPROGRAMAR, CITA_ATENDER, CITA_CANCELAR,
            USUARIO_CREAR, USUARIO_ACTUALIZAR, USUARIO_ACTIVAR, USUARIO_DESACTIVAR, PASSWORD_RESTABLECIDA,
            PASSWORD_CAMBIADA, USUARIO_DESBLOQUEADO, CONSULTA_FICHA);

    // ---- Entidades (columna entidad, máximo 30 caracteres) ----
    public static final String SESION = "SESION";
    public static final String CONTRIBUYENTE = "CONTRIBUYENTE";
    public static final String MATERIAL = "MATERIAL";
    public static final String CITA = "CITA";
    public static final String USUARIO = "USUARIO";
    public static final List<String> ENTIDADES = List.of(SESION, CONTRIBUYENTE, MATERIAL, CITA, USUARIO);

    // ---- Longitudes máximas de cada columna ----
    public static final int MAX_USERNAME = 40;
    public static final int MAX_ACCION = 30;
    public static final int MAX_ENTIDAD = 30;
    public static final int MAX_DESCRIPCION = 255;
    public static final int MAX_DETALLE = 4000;
    public static final int MAX_IP = 45;
    private static final int MAX_VALOR_DIFERENCIA = 80;

    private static final Logger LOG = Logger.getLogger(Auditoria.class.getName());
    /** Caracteres de control, saltos de línea y caracteres de formato invisibles (por ejemplo, los de inversión de texto). */
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}\\p{Cf}\\u2028\\u2029]+");
    /** Campos que NUNCA se comparan ni se guardan. */
    private static final Pattern SENSIBLE =
            Pattern.compile("(?i).*(password|passwd|hash|clave|contrasena|contraseña|token|secret).*");

    private static AuditoriaDAO dao = new AuditoriaDAOImpl();

    private Auditoria() { }

    /** Solo para pruebas: reemplaza el DAO. */
    public static void usarDao(AuditoriaDAO nuevo) {
        dao = nuevo;
    }

    // ================================================================= registrar

    /**
     * Registra un evento tomando el usuario de la SESIÓN (null si no hay sesión) y la IP de la petición.
     * Nunca lanza excepción: si falla, lo escribe en el log del servidor.
     */
    public static void registrar(HttpServletRequest req, String accion, String entidad, Integer idEntidad,
                                 String descripcion, String detalle) {
        Usuario u = usuarioDeSesion(req);
        registrarComo(req, u == null ? null : u.getIdUsuario(), u == null ? null : u.getUsername(),
                accion, entidad, idEntidad, descripcion, detalle);
    }

    /** Igual que registrar, pero con el usuario indicado (por ejemplo, el usuario escrito en un login fallido). */
    public static void registrarComo(HttpServletRequest req, Integer idUsuario, String username, String accion,
                                     String entidad, Integer idEntidad, String descripcion, String detalle) {
        try {
            dao.registrar(construir(idUsuario, username, accion, entidad, idEntidad, descripcion, detalle,
                    req == null ? null : req.getRemoteAddr()));
        } catch (RuntimeException e) {
            // La auditoría nunca debe romper la operación del usuario: solo se deja constancia en el log del servidor.
            LOG.log(Level.SEVERE, "No se pudo registrar la auditoría (acción " + limpiar(accion, MAX_ACCION) + ")", e);
        }
    }

    /** Arma un registro ya limpio (sin caracteres de control y recortado a cada columna). */
    public static RegistroAuditoria construir(Integer idUsuario, String username, String accion, String entidad,
                                              Integer idEntidad, String descripcion, String detalle, String ip) {
        RegistroAuditoria r = new RegistroAuditoria();
        r.setIdUsuario(idUsuario);
        r.setUsername(limpiar(username, MAX_USERNAME));
        r.setAccion(limpiar(accion, MAX_ACCION));
        r.setEntidad(limpiar(entidad, MAX_ENTIDAD));
        r.setIdEntidad(idEntidad);
        r.setDescripcion(limpiar(descripcion, MAX_DESCRIPCION));
        String d = limpiar(detalle, MAX_DETALLE);
        r.setDetalle(d.isEmpty() ? null : d);
        r.setIp(limpiar(ip, MAX_IP));
        return r;
    }

    private static Usuario usuarioDeSesion(HttpServletRequest req) {
        if (req == null) {
            return null;
        }
        HttpSession s = req.getSession(false);
        return s == null ? null : (Usuario) s.getAttribute(LoginServlet.SESION_USUARIO);
    }

    // ================================================================= limpieza de texto

    /**
     * Quita caracteres de control y saltos de línea (evita inyectar líneas falsas en los registros), recorta los
     * espacios y limita la longitud. Un texto nulo se convierte en "".
     */
    public static String limpiar(String texto, int maximo) {
        if (texto == null) {
            return "";
        }
        String t = CONTROL.matcher(texto).replaceAll(" ").trim();
        return t.length() > maximo ? t.substring(0, maximo) : t;
    }

    // ================================================================= diferencias

    /**
     * Compara campo por campo dos objetos del mismo tipo y devuelve solo lo que cambió, por ejemplo
     * "telefono: 966000001 -> 966000099; direccion: A -> B". Omite campos sensibles (contraseñas, hashes, tokens).
     * Un texto vacío y un nulo se consideran iguales. Si no hay diferencias (o los objetos no son comparables)
     * devuelve "".
     */
    public static String diferencias(Object antes, Object despues) {
        if (antes == null || despues == null || antes.getClass() != despues.getClass()) {
            return "";
        }
        List<String> cambios = new ArrayList<>();
        for (Field f : antes.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers()) || f.isSynthetic()
                    || SENSIBLE.matcher(f.getName()).matches()) {
                continue;
            }
            try {
                f.setAccessible(true);
                Object a = normalizar(f.get(antes));
                Object b = normalizar(f.get(despues));
                if (!Objects.equals(a, b)) {
                    cambios.add(f.getName() + ": " + texto(a) + " -> " + texto(b));
                }
            } catch (IllegalAccessException | RuntimeException e) {
                LOG.log(Level.WARNING, "No se pudo comparar el campo " + f.getName(), e);
            }
        }
        return String.join("; ", cambios);
    }

    /** Texto vacío o solo espacios = null; el resto de textos, sin espacios sobrantes. */
    private static Object normalizar(Object valor) {
        if (valor instanceof String s) {
            String t = s.trim();
            return t.isEmpty() ? null : t;
        }
        return valor;
    }

    private static String texto(Object valor) {
        return valor == null ? "(vacío)" : limpiar(String.valueOf(valor), MAX_VALOR_DIFERENCIA);
    }
}

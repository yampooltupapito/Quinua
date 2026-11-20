package pe.gob.munquinua.sigmud.util;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pe.gob.munquinua.sigmud.dao.AreaDAO;
import pe.gob.munquinua.sigmud.dao.ContribuyenteDAO;
import pe.gob.munquinua.sigmud.dao.UsuarioDAO;
import pe.gob.munquinua.sigmud.dao.impl.AreaDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.ContribuyenteDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.UsuarioDAOImpl;
import pe.gob.munquinua.sigmud.model.Cita;
import pe.gob.munquinua.sigmud.model.Contribuyente;
import pe.gob.munquinua.sigmud.model.Usuario;

/**
 * Valida una Cita y devuelve los errores en español (lista vacía si todo está bien).
 * Consulta la BD para comprobar que el contribuyente esté activo y que el funcionario
 * pertenezca al área. El cruce de horarios NO se valida aquí: lo controla CitaDAO.
 */
public class CitaValidador {

    public static final LocalTime HORA_INICIO = LocalTime.of(8, 0);
    public static final LocalTime HORA_FIN = LocalTime.of(16, 30);
    public static final int INTERVALO_MINUTOS = 30;
    public static final int MAX_DIAS_ANTICIPACION = 90;
    public static final int MIN_VENTANILLA = 1;
    public static final int MAX_VENTANILLA = 10;
    public static final int MAX_MOTIVO = 200;

    private final ContribuyenteDAO contribuyenteDao;
    private final AreaDAO areaDao;
    private final UsuarioDAO usuarioDao;
    private final Clock reloj;

    public CitaValidador() {
        this(new ContribuyenteDAOImpl(), new AreaDAOImpl(), new UsuarioDAOImpl(), Clock.systemDefaultZone());
    }

    public CitaValidador(ContribuyenteDAO contribuyenteDao, AreaDAO areaDao, UsuarioDAO usuarioDao, Clock reloj) {
        this.contribuyenteDao = contribuyenteDao;
        this.areaDao = areaDao;
        this.usuarioDao = usuarioDao;
        this.reloj = reloj;
    }

    public List<String> validar(Cita c) {
        List<String> errores = new ArrayList<>();
        if (c == null) {
            errores.add("No se recibieron los datos de la cita.");
            return errores;
        }
        validarContribuyente(c, errores);
        validarAreaYFuncionario(c, errores);
        validarFecha(c.getFecha(), errores);
        validarHora(c.getFecha(), c.getHora(), errores);
        validarVentanilla(c.getVentanilla(), errores);
        validarMotivo(c.getMotivo(), errores);
        return errores;
    }

    private void validarContribuyente(Cita c, List<String> errores) {
        if (c.getIdContribuyente() <= 0) {
            errores.add("Seleccione el contribuyente.");
            return;
        }
        Optional<Contribuyente> ct = contribuyenteDao.buscarPorId(c.getIdContribuyente());
        if (ct.isEmpty()) {
            errores.add("El contribuyente no existe.");
        } else if (!ct.get().isEstado()) {
            errores.add("El contribuyente está dado de baja.");
        }
    }

    private void validarAreaYFuncionario(Cita c, List<String> errores) {
        boolean areaValida = false;
        if (c.getIdArea() <= 0) {
            errores.add("Seleccione el área.");
        } else if (areaDao.listarActivas().stream().noneMatch(a -> a.getIdArea() == c.getIdArea())) {
            errores.add("El área seleccionada no es válida.");
        } else {
            areaValida = true;
        }
        if (c.getIdFuncionario() <= 0) {
            errores.add("Seleccione el funcionario.");
        } else if (areaValida && usuarioDao.listarFuncionariosPorArea(c.getIdArea()).stream()
                .map(Usuario::getIdUsuario).noneMatch(id -> id == c.getIdFuncionario())) {
            errores.add("El funcionario no pertenece al área seleccionada.");
        }
    }

    /** No pasada, no mayor a 90 días desde hoy, de lunes a viernes. */
    private void validarFecha(LocalDate fecha, List<String> errores) {
        if (fecha == null) {
            errores.add("Seleccione la fecha.");
            return;
        }
        LocalDate hoy = LocalDate.now(reloj);
        if (fecha.isBefore(hoy)) {
            errores.add("La fecha no puede ser anterior a hoy.");
        } else if (fecha.isAfter(hoy.plusDays(MAX_DIAS_ANTICIPACION))) {
            errores.add("No se pueden agendar citas con más de " + MAX_DIAS_ANTICIPACION + " días de anticipación.");
        }
        if (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
            errores.add("Solo se atiende de lunes a viernes.");
        }
    }

    /** Entre 08:00 y 16:30, en intervalos de 30 minutos; si es hoy, no puede haber pasado. */
    private void validarHora(LocalDate fecha, LocalTime hora, List<String> errores) {
        if (hora == null) {
            errores.add("Seleccione la hora.");
            return;
        }
        if (hora.isBefore(HORA_INICIO) || hora.isAfter(HORA_FIN)) {
            errores.add("La hora debe estar entre 08:00 y 16:30.");
        } else if (hora.getMinute() % INTERVALO_MINUTOS != 0 || hora.getSecond() != 0 || hora.getNano() != 0) {
            errores.add("La hora debe ser en intervalos de 30 minutos (08:00, 08:30, 09:00...).");
        } else if (fecha != null && fecha.equals(LocalDate.now(reloj))
                && hora.isBefore(LocalDateTime.now(reloj).toLocalTime())) {
            errores.add("La hora ya pasó.");
        }
    }

    private void validarVentanilla(int ventanilla, List<String> errores) {
        if (ventanilla < MIN_VENTANILLA || ventanilla > MAX_VENTANILLA) {
            errores.add("La ventanilla debe estar entre 1 y 10.");
        }
    }

    private void validarMotivo(String motivo, List<String> errores) {
        if (Validador.vacio(motivo)) {
            errores.add("El motivo es obligatorio.");
        } else if (motivo.trim().length() > MAX_MOTIVO) {
            errores.add("El motivo no puede pasar de " + MAX_MOTIVO + " caracteres.");
        }
    }
}

package pe.gob.munquinua.sigmud.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pe.gob.munquinua.sigmud.dao.AuditoriaDAO;
import pe.gob.munquinua.sigmud.dao.DashboardDAO;
import pe.gob.munquinua.sigmud.dao.impl.AuditoriaDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.DashboardDAOImpl;
import pe.gob.munquinua.sigmud.model.Cita;
import pe.gob.munquinua.sigmud.model.EtiquetaValor;
import pe.gob.munquinua.sigmud.model.FilaStockCritico;
import pe.gob.munquinua.sigmud.model.FiltrosAuditoria;
import pe.gob.munquinua.sigmud.model.MovimientoStock;
import pe.gob.munquinua.sigmud.model.PuntoDiario;
import pe.gob.munquinua.sigmud.model.RegistroAuditoria;
import pe.gob.munquinua.sigmud.model.Usuario;

/**
 * Arma los datos del panel de cada rol (indicadores, gráficos y listas) como mapas de texto y números.
 * Los usa DashboardServlet (para dibujar la página) y DashboardDatosServlet (para el JSON), así ambos coinciden.
 * Cada rol recibe SOLO sus propios datos: no hay forma de pedir los de otro rol desde aquí.
 *
 * Estructura devuelta: { "kpis": {...}, "graficos": {...}, "listas": {...} }.
 */
public class PanelDatos {

    private static final String[] MESES = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"};
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static final int DIAS_CITAS_POR_ESTADO = 30;
    public static final int DIAS_MOVIMIENTOS = 14;
    public static final int MESES_CONTRIBUYENTES = 6;
    public static final int DIAS_ATENDIDAS = 7;
    public static final int ULTIMOS_MOVIMIENTOS = 5;
    public static final int ACTIVIDAD_RECIENTE = 8;

    private final DashboardDAO dao;
    private final AuditoriaDAO auditoriaDao;

    public PanelDatos() {
        this(new DashboardDAOImpl());
    }

    public PanelDatos(DashboardDAO dao) {
        this(dao, new AuditoriaDAOImpl());
    }

    public PanelDatos(DashboardDAO dao, AuditoriaDAO auditoriaDao) {
        this.dao = dao;
        this.auditoriaDao = auditoriaDao;
    }

    /**
     * @param rol       rol de la SESIÓN (ADMIN, ATENCION o ALMACEN)
     * @param idUsuario id del usuario de la SESIÓN (para ATENCION: el funcionario)
     * @throws IllegalArgumentException si el rol no tiene panel
     */
    public Map<String, Object> construir(String rol, int idUsuario) {
        return switch (rol == null ? "" : rol) {
            case "ADMIN" -> admin();
            case "ATENCION" -> atencion(idUsuario);
            case "ALMACEN" -> almacen();
            default -> throw new IllegalArgumentException("Rol sin panel: " + rol);
        };
    }

    // ---------- ADMIN ----------

    private Map<String, Object> admin() {
        Map<String, Long> t = dao.totalesAdmin();
        List<Usuario> bloqueadas = dao.cuentasBloqueadas();
        Map<String, Long> kpis = new LinkedHashMap<>();
        kpis.put("usuarios", t.get("usuariosActivos"));
        kpis.put("contribuyentes", t.get("contribuyentesActivos"));
        kpis.put("materiales", t.get("materialesActivos"));
        kpis.put("criticos", t.get("materialesCriticos"));
        kpis.put("citasHoy", t.get("citasHoy"));
        kpis.put("movimientosHoy", t.get("movimientosHoy"));
        kpis.put("bloqueadas", (long) bloqueadas.size());
        kpis.put("loginFallidos", loginFallidos24h());

        Map<String, Object> graficos = new LinkedHashMap<>();
        graficos.put("citasPorEstado", serieEstados(dao.citasPorEstado(DIAS_CITAS_POR_ESTADO)));
        graficos.put("movimientosPorDia", serieMovimientos(dao.movimientosPorDia(DIAS_MOVIMIENTOS)));
        graficos.put("contribuyentesPorMes", serieMeses(dao.contribuyentesPorMes(MESES_CONTRIBUYENTES)));

        Map<String, Object> listas = new LinkedHashMap<>();
        listas.put("bloqueadas", filasUsuarios(bloqueadas, false));
        listas.put("accesos", filasUsuarios(dao.ultimosAccesos(), true));
        listas.put("actividad", filasActividad(auditoriaDao.recientes(ACTIVIDAD_RECIENTE)));
        return panel(kpis, graficos, listas);
    }

    /** Intentos de acceso fallidos en las últimas 24 horas (según la auditoría). */
    private long loginFallidos24h() {
        FiltrosAuditoria f = new FiltrosAuditoria();
        f.setAccion(Auditoria.LOGIN_FALLIDO);
        f.setDesdeMomento(LocalDateTime.now().minusHours(24));
        return auditoriaDao.contar(f);
    }

    private List<Map<String, Object>> filasActividad(List<RegistroAuditoria> registros) {
        List<Map<String, Object>> filas = new ArrayList<>();
        for (RegistroAuditoria r : registros) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("fecha", r.getFechaTexto());
            f.put("usuario", r.getUsername() == null ? "" : r.getUsername());
            f.put("accion", r.getAccion());
            f.put("descripcion", r.getDescripcion());
            f.put("clase", r.getClaseInsignia());
            filas.add(f);
        }
        return filas;
    }

    // ---------- ATENCION (todo filtrado por el funcionario de la sesión) ----------

    private Map<String, Object> atencion(int idFuncionario) {
        Map<String, Long> t = dao.misCitasHoy(idFuncionario);
        Map<String, Long> kpis = new LinkedHashMap<>(t);

        Map<String, Object> graficos = new LinkedHashMap<>();
        graficos.put("atendidasPorDia", serieDias(dao.misCitasAtendidasPorDia(idFuncionario, DIAS_ATENDIDAS)));

        List<Map<String, Object>> proximas = new ArrayList<>();
        for (Cita c : dao.misProximasCitasHoy(idFuncionario, LocalTime.now())) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("id", c.getIdCita());
            f.put("hora", c.getHora().toString());
            f.put("contribuyente", c.getNombreContribuyente());
            f.put("dniRuc", c.getDniRuc());
            f.put("motivo", c.getMotivo() == null ? "" : c.getMotivo());
            f.put("ventanilla", c.getVentanilla());
            f.put("fecha", c.getFecha().toString());
            f.put("idArea", c.getIdArea());
            proximas.add(f);
        }
        Map<String, Object> listas = new LinkedHashMap<>();
        listas.put("proximas", proximas);
        return panel(kpis, graficos, listas);
    }

    // ---------- ALMACEN ----------

    private Map<String, Object> almacen() {
        Map<String, Long> t = dao.totalesAlmacen();
        Map<String, Long> kpis = new LinkedHashMap<>();
        kpis.put("materiales", t.get("materialesActivos"));
        kpis.put("criticos", t.get("materialesCriticos"));
        kpis.put("movimientosHoy", t.get("movimientosHoy"));
        kpis.put("entradasMes", t.get("entradasMes"));
        kpis.put("salidasMes", t.get("salidasMes"));

        Map<String, Object> graficos = new LinkedHashMap<>();
        graficos.put("salidasPorArea", serieSimple(dao.salidasPorAreaMes()));

        List<Map<String, Object>> reponer = new ArrayList<>();
        for (FilaStockCritico m : dao.materialesParaReponer()) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("codigo", m.getCodigo());
            f.put("nombre", m.getNombre());
            f.put("unidad", m.getUnidad());
            f.put("stock", m.getStock());
            f.put("minimo", m.getStockMinimo());
            f.put("faltante", m.getFaltante());
            reponer.add(f);
        }
        List<Map<String, Object>> movimientos = new ArrayList<>();
        for (MovimientoStock mv : dao.ultimosMovimientos(ULTIMOS_MOVIMIENTOS)) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("fecha", mv.getFecha() == null ? "" : mv.getFecha().format(FECHA_HORA));
            f.put("tipo", mv.getTipo());
            f.put("material", mv.getNombreMaterial());
            f.put("cantidad", mv.getCantidad());
            f.put("area", mv.getNombreArea() == null ? "" : mv.getNombreArea());
            f.put("usuario", mv.getNombreUsuario());
            movimientos.add(f);
        }
        Map<String, Object> listas = new LinkedHashMap<>();
        listas.put("reponer", reponer);
        listas.put("movimientos", movimientos);
        return panel(kpis, graficos, listas);
    }

    // ---------- armado de series y filas ----------

    private Map<String, Object> panel(Map<String, Long> kpis, Map<String, Object> graficos, Map<String, Object> listas) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("kpis", kpis);
        p.put("graficos", graficos);
        p.put("listas", listas);
        return p;
    }

    private Map<String, Object> serieEstados(List<EtiquetaValor> datos) {
        List<String> etiquetas = new ArrayList<>();
        List<Long> valores = new ArrayList<>();
        for (EtiquetaValor d : datos) {
            etiquetas.add(switch (d.getEtiqueta()) {
                case Cita.PROGRAMADA -> "Programadas";
                case Cita.ATENDIDA -> "Atendidas";
                default -> "Canceladas";
            });
            valores.add(d.getValor());
        }
        return serie(etiquetas, "valores", valores);
    }

    private Map<String, Object> serieMovimientos(List<PuntoDiario> datos) {
        List<String> etiquetas = new ArrayList<>();
        List<Long> entradas = new ArrayList<>();
        List<Long> salidas = new ArrayList<>();
        for (PuntoDiario p : datos) {
            etiquetas.add(p.getFecha().format(DIA_MES));
            entradas.add(p.getEntradas());
            salidas.add(p.getSalidas());
        }
        Map<String, Object> s = serie(etiquetas, "entradas", entradas);
        s.put("salidas", salidas);
        return s;
    }

    /** Etiquetas AAAA-MM -> "nov 2026". */
    private Map<String, Object> serieMeses(List<EtiquetaValor> datos) {
        List<String> etiquetas = new ArrayList<>();
        List<Long> valores = new ArrayList<>();
        for (EtiquetaValor d : datos) {
            YearMonth m = YearMonth.parse(d.getEtiqueta());
            etiquetas.add(MESES[m.getMonthValue() - 1] + " " + m.getYear());
            valores.add(d.getValor());
        }
        return serie(etiquetas, "valores", valores);
    }

    /** Etiquetas AAAA-MM-DD -> "14/11". */
    private Map<String, Object> serieDias(List<EtiquetaValor> datos) {
        List<String> etiquetas = new ArrayList<>();
        List<Long> valores = new ArrayList<>();
        for (EtiquetaValor d : datos) {
            etiquetas.add(LocalDate.parse(d.getEtiqueta()).format(DIA_MES));
            valores.add(d.getValor());
        }
        return serie(etiquetas, "valores", valores);
    }

    private Map<String, Object> serieSimple(List<EtiquetaValor> datos) {
        List<String> etiquetas = new ArrayList<>();
        List<Long> valores = new ArrayList<>();
        for (EtiquetaValor d : datos) {
            etiquetas.add(d.getEtiqueta());
            valores.add(d.getValor());
        }
        return serie(etiquetas, "valores", valores);
    }

    private Map<String, Object> serie(List<String> etiquetas, String clave, List<Long> valores) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("etiquetas", etiquetas);
        s.put(clave, valores);
        return s;
    }

    /** Filas de usuarios para las listas del ADMIN (cuentas bloqueadas y últimos accesos). */
    private List<Map<String, Object>> filasUsuarios(List<Usuario> usuarios, boolean conAcceso) {
        List<Map<String, Object>> filas = new ArrayList<>();
        for (Usuario u : usuarios) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("id", u.getIdUsuario());
            f.put("username", u.getUsername());
            f.put("nombres", u.getNombres());
            if (conAcceso) {
                LocalDateTime a = u.getUltimoAcceso();
                f.put("rol", u.getRol());
                f.put("fecha", a == null ? "" : a.format(FECHA_HORA));
            } else {
                f.put("intentos", u.getIntentosFallidos());
            }
            filas.add(f);
        }
        return filas;
    }
}

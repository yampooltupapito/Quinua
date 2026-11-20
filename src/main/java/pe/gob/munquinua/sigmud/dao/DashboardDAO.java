package pe.gob.munquinua.sigmud.dao;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import pe.gob.munquinua.sigmud.model.Cita;
import pe.gob.munquinua.sigmud.model.EtiquetaValor;
import pe.gob.munquinua.sigmud.model.FilaStockCritico;
import pe.gob.munquinua.sigmud.model.MovimientoStock;
import pe.gob.munquinua.sigmud.model.PuntoDiario;
import pe.gob.munquinua.sigmud.model.Usuario;

/**
 * Consultas de agregación para los paneles (dashboards). Solo lectura.
 * "Hoy" y "mes actual" salen del reloj de la aplicación (no del servidor MySQL).
 * Las consultas del actor ATENCION reciben el id del funcionario, que el servlet toma SIEMPRE de la sesión.
 */
public interface DashboardDAO {

    // ---------- ADMIN ----------

    /** Claves: usuariosActivos, contribuyentesActivos, materialesActivos, materialesCriticos, citasHoy, movimientosHoy. */
    Map<String, Long> totalesAdmin();

    /** Citas por estado de los últimos N días (siempre devuelve PROGRAMADA, ATENDIDA y CANCELADA, con 0 si no hay). */
    List<EtiquetaValor> citasPorEstado(int dias);

    /** Entradas y salidas por día de los últimos N días, un punto por día (0 en los días sin movimientos). */
    List<PuntoDiario> movimientosPorDia(int dias);

    /** Contribuyentes registrados por mes en los últimos N meses (etiqueta AAAA-MM; 0 en meses sin registros). */
    List<EtiquetaValor> contribuyentesPorMes(int meses);

    /** Usuarios con 5 o más intentos fallidos (sin el hash de la contraseña). */
    List<Usuario> cuentasBloqueadas();

    /** Los 5 usuarios con el último acceso más reciente (sin el hash). */
    List<Usuario> ultimosAccesos();

    // ---------- ATENCION (idFuncionario viene de la sesión) ----------

    /** Claves: total, programadas, atendidas, canceladas (citas de hoy de ese funcionario). */
    Map<String, Long> misCitasHoy(int idFuncionario);

    /** Citas PROGRAMADAS de hoy de ese funcionario con hora desde "desde", por hora, máximo 5. */
    List<Cita> misProximasCitasHoy(int idFuncionario, LocalTime desde);

    /** Citas ATENDIDAS por día de ese funcionario en los últimos N días (etiqueta AAAA-MM-DD; 0 en días sin citas). */
    List<EtiquetaValor> misCitasAtendidasPorDia(int idFuncionario, int dias);

    // ---------- ALMACEN ----------

    /** Claves: materialesActivos, materialesCriticos, movimientosHoy, entradasMes, salidasMes. */
    Map<String, Long> totalesAlmacen();

    /** Los 5 materiales críticos con mayor faltante (stock mínimo - stock). */
    List<FilaStockCritico> materialesParaReponer();

    /** Unidades entregadas por área en el mes actual, de mayor a menor. */
    List<EtiquetaValor> salidasPorAreaMes();

    /** Los últimos N movimientos de stock (más recientes primero), con nombres de material, área y usuario. */
    List<MovimientoStock> ultimosMovimientos(int limite);
}

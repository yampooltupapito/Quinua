package pe.gob.munquinua.sigmud.dao;

import java.time.LocalDate;
import java.util.List;
import pe.gob.munquinua.sigmud.model.FilaResumenMaterial;
import pe.gob.munquinua.sigmud.model.FilaSalidaArea;
import pe.gob.munquinua.sigmud.model.FilaStockCritico;
import pe.gob.munquinua.sigmud.model.MovimientoStock;

/** Consultas de reportes del almacén (solo lectura). Los rangos de fechas incluyen "desde" y "hasta". */
public interface ReporteAlmacenDAO {

    /** Materiales activos con stock <= stock mínimo, con el faltante (mínimo - stock). */
    List<FilaStockCritico> stockCritico();

    /**
     * Detalle de movimientos del periodo. tipo ("ENTRADA"/"SALIDA"), idArea e idMaterial
     * son opcionales: null o vacío significa "sin filtro".
     */
    List<MovimientoStock> movimientosPorFecha(LocalDate desde, LocalDate hasta,
                                              String tipo, Integer idArea, Integer idMaterial);

    /** Por material: suma de entradas, suma de salidas, saldo y cantidad de movimientos del periodo. */
    List<FilaResumenMaterial> resumenPorMaterial(LocalDate desde, LocalDate hasta);

    /** Unidades entregadas a cada área en el periodo, de mayor a menor. */
    List<FilaSalidaArea> salidasPorArea(LocalDate desde, LocalDate hasta);
}

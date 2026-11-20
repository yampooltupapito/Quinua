package pe.gob.munquinua.sigmud.dao;

import java.time.LocalDate;
import java.util.List;
import pe.gob.munquinua.sigmud.model.FilaCitasResumen;

/** Reportes de citas (solo lectura). El rango incluye los días "desde" y "hasta". */
public interface ReporteCitasDAO {

    /** Por área: total de citas y cuántas están programadas, atendidas y canceladas. */
    List<FilaCitasResumen> citasPorArea(LocalDate desde, LocalDate hasta);

    /** Por funcionario: total de citas y cuántas están programadas, atendidas y canceladas. */
    List<FilaCitasResumen> citasPorFuncionario(LocalDate desde, LocalDate hasta);
}

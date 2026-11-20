package pe.gob.munquinua.sigmud.dao;

import java.util.List;
import pe.gob.munquinua.sigmud.model.Area;

/** Acceso a datos de áreas (unidades orgánicas de la municipalidad). */
public interface AreaDAO {

    /** Áreas activas ordenadas por nombre (para la lista de áreas solicitantes). */
    List<Area> listarActivas();
}

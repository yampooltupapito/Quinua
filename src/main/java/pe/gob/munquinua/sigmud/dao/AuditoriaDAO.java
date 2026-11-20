package pe.gob.munquinua.sigmud.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import pe.gob.munquinua.sigmud.model.FiltrosAuditoria;
import pe.gob.munquinua.sigmud.model.RegistroAuditoria;

/**
 * Acceso a datos de la auditoría. El registro es INMUTABLE: solo se inserta y se consulta (la base de datos impide
 * UPDATE y DELETE con triggers).
 */
public interface AuditoriaDAO {

    /** Guarda un registro con su propia conexión (si falla, lanza DaoException). */
    void registrar(RegistroAuditoria r);

    /**
     * Guarda un registro usando una conexión que ya está dentro de una transacción: se confirma o se deshace junto
     * con ella. No cierra la conexión.
     */
    void registrar(Connection cn, RegistroAuditoria r) throws SQLException;

    /** Una página de resultados (más recientes primero). pagina empieza en 1. */
    List<RegistroAuditoria> buscar(FiltrosAuditoria filtros, int pagina, int tamano);

    /** Total de registros que cumplen los filtros (para la paginación). */
    long contar(FiltrosAuditoria filtros);

    /** Los n registros más recientes. */
    List<RegistroAuditoria> recientes(int n);

    /** Los n eventos más recientes de una entidad concreta (por ejemplo CONTRIBUYENTE con id 5). */
    List<RegistroAuditoria> ultimosPorEntidad(String entidad, int idEntidad, int n);
}

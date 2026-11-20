package pe.gob.munquinua.sigmud.dao;

import java.util.List;
import java.util.Optional;
import pe.gob.munquinua.sigmud.exception.RegistroDuplicadoException;
import pe.gob.munquinua.sigmud.model.Material;
import pe.gob.munquinua.sigmud.model.Pagina;

/** Acceso a datos de materiales del almacén (baja lógica; el stock solo cambia con movimientos). */
public interface MaterialDAO {

    /** Materiales activos, ordenados por nombre. */
    List<Material> listar();

    /** Activos cuyo código o nombre contiene el texto. */
    List<Material> buscar(String texto);

    /** Activos con stock menor o igual al stock mínimo. */
    List<Material> listarStockCritico();

    /**
     * Una página de materiales activos, con búsqueda por código o nombre, filtro opcional de stock crítico y orden por
     * columna (codigo, nombre, unidad, stock o minimo; otro valor = nombre). LIMIT y OFFSET van como parámetros.
     */
    Pagina<Material> listarPagina(String texto, boolean soloCriticos, int pagina, int tamano, String orden, String dir);

    /** Busca por id (activo o no). */
    Optional<Material> buscarPorId(int id);

    /** Busca por código exacto (activo o no). */
    Optional<Material> buscarPorCodigo(String codigo);

    /** Inserta con stock 0 (el stock enviado en el objeto se ignora) y deja el id generado. */
    void insertar(Material m) throws RegistroDuplicadoException;

    /** Actualiza código, nombre, unidad y stock mínimo. NUNCA toca el stock. */
    void actualizar(Material m) throws RegistroDuplicadoException;

    /** Baja lógica: estado = 0. No borra la fila. */
    void darDeBaja(int id);

    /** Vuelve a poner estado = 1. */
    void reactivar(int id);
}

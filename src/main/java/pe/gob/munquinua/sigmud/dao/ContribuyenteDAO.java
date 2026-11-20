package pe.gob.munquinua.sigmud.dao;

import java.util.List;
import java.util.Optional;
import pe.gob.munquinua.sigmud.exception.RegistroDuplicadoException;
import pe.gob.munquinua.sigmud.model.Contribuyente;
import pe.gob.munquinua.sigmud.model.Pagina;

/** Acceso a datos de contribuyentes: búsquedas, baja lógica y control de DNI/RUC duplicado. */
public interface ContribuyenteDAO {

    /** Contribuyentes activos, ordenados por nombre. */
    List<Contribuyente> listar();

    /**
     * Una página del listado, con búsqueda opcional, opción de incluir dados de baja y orden por columna.
     * orden: dniRuc, nombre, tipo, telefono o email (otro valor = nombre); dir: asc o desc. LIMIT y OFFSET van como parámetros.
     */
    Pagina<Contribuyente> listarPagina(String texto, boolean incluirInactivos, int pagina, int tamano,
                                       String orden, String dir);

    /** Todos los contribuyentes, activos y dados de baja, ordenados por nombre. */
    List<Contribuyente> listarConInactivos();

    /** Como buscar(texto), pero incluyendo los dados de baja. */
    List<Contribuyente> buscarConInactivos(String texto);

    /** Busca por id (activo o no). */
    Optional<Contribuyente> buscarPorId(int id);

    /** Busca por DNI/RUC exacto (activo o no, para detectar duplicados dados de baja). */
    Optional<Contribuyente> buscarPorDniRuc(String dniRuc);

    /** Activos cuyo nombre/razón social contiene el texto, o cuyo DNI/RUC lo contiene. */
    List<Contribuyente> buscar(String texto);

    /** Inserta y deja el id generado en el objeto. */
    void insertar(Contribuyente c) throws RegistroDuplicadoException;

    /** Actualiza los datos (no cambia estado ni fecha de registro). */
    void actualizar(Contribuyente c) throws RegistroDuplicadoException;

    /** Baja lógica: estado = 0. No borra la fila. */
    void darDeBaja(int id);

    /** Vuelve a poner estado = 1. */
    void reactivar(int id);

    /** true si otro contribuyente (distinto de idExcluido) ya usa ese DNI/RUC. Use 0 al registrar uno nuevo. */
    boolean existeDniRuc(String dniRuc, int idExcluido);
}

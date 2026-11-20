package pe.gob.munquinua.sigmud.dao;

import java.util.List;
import java.util.Optional;
import pe.gob.munquinua.sigmud.exception.RegistroDuplicadoException;
import pe.gob.munquinua.sigmud.exception.UltimoAdminException;
import pe.gob.munquinua.sigmud.model.Pagina;
import pe.gob.munquinua.sigmud.model.Usuario;

/**
 * Acceso a datos de usuarios: login, intentos fallidos, gestión de usuarios (solo ADMIN) y contraseñas.
 * Un usuario NUNCA se borra: solo se desactiva, porque tiene movimientos y citas asociados.
 */
public interface UsuarioDAO {

    /** Busca por username (incluye el hash para poder verificar la contraseña). */
    Optional<Usuario> buscarPorUsername(String username);

    /** Busca por id (sin el hash de la contraseña). */
    Optional<Usuario> buscarPorId(int idUsuario);

    /**
     * Lista usuarios (sin el hash) con filtros opcionales: texto (en usuario o nombre), rol y estado.
     * Cada filtro en null o vacío significa "sin filtro". Ordenados por nombre.
     */
    List<Usuario> listar(String texto, String rol, Boolean activo);

    /**
     * Una página del listado de usuarios con los mismos filtros de listar(...) y orden por columna (username, nombre,
     * rol, area, estado o acceso; otro valor = nombre). LIMIT y OFFSET van como parámetros.
     */
    Pagina<Usuario> listarPagina(String texto, String rol, Boolean activo, int pagina, int tamano,
                                 String orden, String dir);

    /** true si otro usuario (distinto de idExcluido; use 0 al crear) ya usa ese nombre de usuario. */
    boolean existeUsername(String username, int idExcluido);

    /** Cuántos usuarios ADMIN activos hay. */
    int contarAdminsActivos();

    /**
     * Inserta un usuario con su hash de contraseña (ya calculado) y debe_cambiar_password = 1.
     * Deja el id generado en el objeto.
     */
    void insertar(Usuario u, String passwordHash) throws RegistroDuplicadoException;

    /**
     * Actualiza nombres, rol y área (no el nombre de usuario ni la contraseña).
     * @throws UltimoAdminException si se le quita el rol ADMIN al último ADMIN activo
     */
    void actualizar(Usuario u) throws UltimoAdminException;

    /**
     * Activa o desactiva una cuenta. Al activar, también reinicia los intentos fallidos.
     * @throws UltimoAdminException si se desactiva al último ADMIN activo
     */
    void cambiarEstado(int idUsuario, boolean activo) throws UltimoAdminException;

    /** Guarda un hash nuevo y marca si debe cambiarla en el próximo inicio de sesión. */
    void actualizarPassword(int idUsuario, String passwordHash, boolean debeCambiar);

    /**
     * Desbloquea una cuenta bloqueada por intentos fallidos: pone intentos_fallidos en 0 y la reactiva.
     * @return true si la cuenta estaba bloqueada (5 o más intentos) y se desbloqueó; false si no
     */
    boolean desbloquear(int idUsuario);

    /** Usuarios activos con rol ATENCION de esa área (sin el hash de la contraseña), ordenados por nombre. */
    List<Usuario> listarFuncionariosPorArea(int idArea);

    /** Pone los intentos fallidos en 0 y guarda la fecha del último acceso. */
    void registrarAccesoExitoso(int idUsuario);

    /** Suma un intento fallido; al llegar a 5 desactiva la cuenta (estado = 0). */
    void registrarIntentoFallido(int idUsuario);
}

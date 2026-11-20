package pe.gob.munquinua.sigmud.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.gob.munquinua.sigmud.dao.impl.UsuarioDAOImpl;
import pe.gob.munquinua.sigmud.exception.RegistroDuplicadoException;
import pe.gob.munquinua.sigmud.exception.UltimoAdminException;
import pe.gob.munquinua.sigmud.util.Seguridad;
import pe.gob.munquinua.sigmud.model.Usuario;

/** Pruebas de UsuarioDAO contra la base mun_quinua_test. Restauran el usuario que modifican. */
class UsuarioDAOTest extends BaseDaoTest {

    private final UsuarioDAO dao = new UsuarioDAOImpl();
    private int idAlmacen;

    @BeforeEach
    void guardarEstadoOriginal() {
        idAlmacen = dao.buscarPorUsername("almacen1").orElseThrow().getIdUsuario();
    }

    @AfterEach
    void restaurarUsuario() {
        ejecutar("UPDATE usuario SET intentos_fallidos = 0, estado = 1, ultimo_acceso = NULL WHERE id_usuario = ?", idAlmacen);
        ejecutar("DELETE FROM usuario WHERE username LIKE 'tst_usr%'");
        ejecutar("UPDATE usuario SET estado = 1, rol = 'ADMIN', id_area = 6, debe_cambiar_password = 0 WHERE username = 'admin'");
    }

    @Test
    void buscaUnUsuarioExistenteConSusDatos() {
        Optional<Usuario> u = dao.buscarPorUsername("admin");
        assertTrue(u.isPresent());
        assertEquals("ADMIN", u.get().getRol());
        assertTrue(u.get().isEstado());
        assertNotNull(u.get().getPasswordHash());
        assertEquals(64, u.get().getPasswordHash().length());
    }

    @Test
    void usuarioInexistenteDevuelveVacio() {
        assertTrue(dao.buscarPorUsername("no_existe").isEmpty());
    }

    @Test
    void usuarioConComillasNoRompeLaConsultaNiDevuelveDatos() {
        assertTrue(dao.buscarPorUsername("' OR '1'='1").isEmpty());
    }

    @Test
    void cadaIntentoFallidoSumaUno() {
        dao.registrarIntentoFallido(idAlmacen);
        dao.registrarIntentoFallido(idAlmacen);
        Usuario u = dao.buscarPorUsername("almacen1").orElseThrow();
        assertEquals(2, u.getIntentosFallidos());
        assertTrue(u.isEstado(), "con 2 intentos la cuenta sigue activa");
    }

    @Test
    void alQuintoIntentoFallidoLaCuentaSeBloquea() {
        for (int i = 1; i <= 4; i++) {
            dao.registrarIntentoFallido(idAlmacen);
            assertTrue(dao.buscarPorUsername("almacen1").orElseThrow().isEstado(), "intento " + i);
        }
        dao.registrarIntentoFallido(idAlmacen);
        Usuario u = dao.buscarPorUsername("almacen1").orElseThrow();
        assertEquals(5, u.getIntentosFallidos());
        assertFalse(u.isEstado(), "al 5.º intento la cuenta queda bloqueada");
    }

    @Test
    void elAccesoExitosoReiniciaElContadorYGuardaLaFecha() {
        dao.registrarIntentoFallido(idAlmacen);
        dao.registrarIntentoFallido(idAlmacen);
        dao.registrarAccesoExitoso(idAlmacen);
        Usuario u = dao.buscarPorUsername("almacen1").orElseThrow();
        assertEquals(0, u.getIntentosFallidos());
        assertNotNull(u.getUltimoAcceso());
    }

    @Test
    void listaFuncionariosActivosDeAtencionPorArea() {
        List<Usuario> area1 = dao.listarFuncionariosPorArea(1);
        assertEquals(1, area1.size());
        assertEquals("atencion1", area1.get(0).getUsername());
        assertNull(area1.get(0).getPasswordHash(), "el hash no debe salir del DAO en este listado");
        assertTrue(dao.listarFuncionariosPorArea(5).isEmpty(), "el área 5 solo tiene un usuario de ALMACEN");
    }

    @Test
    void buscarPorIdDevuelveElUsuarioSinSuHash() {
        Usuario u = dao.buscarPorId(idAlmacen).orElseThrow();
        assertEquals("almacen1", u.getUsername());
        assertNull(u.getPasswordHash());
        assertTrue(dao.buscarPorId(999999).isEmpty());
    }

    @Test
    void desbloquearReiniciaLosIntentosYReactivaUnaCuentaBloqueada() {
        for (int i = 0; i < 5; i++) {
            dao.registrarIntentoFallido(idAlmacen);
        }
        assertFalse(dao.buscarPorId(idAlmacen).orElseThrow().isEstado(), "quedó bloqueada");
        assertTrue(dao.desbloquear(idAlmacen));
        Usuario u = dao.buscarPorId(idAlmacen).orElseThrow();
        assertEquals(0, u.getIntentosFallidos());
        assertTrue(u.isEstado());
    }

    @Test
    void desbloquearNoHaceNadaSiLaCuentaNoEstaBloqueada() {
        dao.registrarIntentoFallido(idAlmacen);                 // 1 intento: no está bloqueada
        assertFalse(dao.desbloquear(idAlmacen));
        assertEquals(1, dao.buscarPorId(idAlmacen).orElseThrow().getIntentosFallidos(), "no se tocó");
        assertFalse(dao.desbloquear(999999));
    }

    @Test
    void desbloquearNoReactivaUnaCuentaDesactivadaAMano() {
        ejecutar("UPDATE usuario SET estado = 0 WHERE id_usuario = ?", idAlmacen);   // desactivada por un administrador (0 intentos)
        assertFalse(dao.desbloquear(idAlmacen));
        assertFalse(dao.buscarPorId(idAlmacen).orElseThrow().isEstado());
    }

    // ================= gestión de usuarios (nuevos métodos) =================

    private Usuario nuevo(String username, String rol, Integer idArea) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setNombres("Usuario de prueba " + username);
        u.setRol(rol);
        u.setIdArea(idArea);
        return u;
    }

    private int idDe(String username) {
        return dao.buscarPorUsername(username).orElseThrow().getIdUsuario();
    }

    @Test
    void insertarGuardaElUsuarioActivoConClaveTemporalYHashPbkdf2() throws Exception {
        String hash = Seguridad.hashear("Temporal123");
        Usuario u = nuevo("tst_usr_1", "ATENCION", 1);
        dao.insertar(u, hash);
        assertTrue(u.getIdUsuario() > 0);
        Usuario guardado = dao.buscarPorUsername("tst_usr_1").orElseThrow();
        assertTrue(guardado.isEstado(), "nace activo");
        assertTrue(guardado.isDebeCambiarPassword(), "nace con la contraseña temporal");
        assertEquals(0, guardado.getIntentosFallidos());
        assertEquals("ATENCION", guardado.getRol());
        assertEquals("Mesa de Partes", guardado.getNombreArea());
        assertEquals(hash, guardado.getPasswordHash(), "el hash PBKDF2 (más de 64 caracteres) se guardó completo");
        assertTrue(Seguridad.verificar("Temporal123", guardado.getPasswordHash()));
        assertNull(dao.buscarPorId(guardado.getIdUsuario()).orElseThrow().getPasswordHash(), "buscarPorId no devuelve el hash");
    }

    @Test
    void unUsernameDuplicadoLanzaRegistroDuplicadoException() throws Exception {
        dao.insertar(nuevo("tst_usr_2", "ALMACEN", null), Seguridad.hashear("Temporal123"));
        assertThrows(RegistroDuplicadoException.class,
                () -> dao.insertar(nuevo("tst_usr_2", "ATENCION", 1), Seguridad.hashear("Otra12345")));
        assertThrows(RegistroDuplicadoException.class,
                () -> dao.insertar(nuevo("admin", "ADMIN", null), Seguridad.hashear("Otra12345")), "también contra los de ejemplo");
        assertEquals(1, contar("SELECT COUNT(*) FROM usuario WHERE username = 'tst_usr_2'"));
    }

    @Test
    void existeUsernameExcluyeElIdIndicado() throws Exception {
        Usuario u = nuevo("tst_usr_3", "ALMACEN", null);
        dao.insertar(u, Seguridad.hashear("Temporal123"));
        assertTrue(dao.existeUsername("tst_usr_3", 0));
        assertFalse(dao.existeUsername("tst_usr_3", u.getIdUsuario()), "al editarse a sí mismo no cuenta");
        assertFalse(dao.existeUsername("tst_usr_no_existe", 0));
    }

    @Test
    void actualizarCambiaNombreRolYAreaPeroNoElUsernameNiLaClave() throws Exception {
        Usuario u = nuevo("tst_usr_4", "ATENCION", 1);
        String hash = Seguridad.hashear("Temporal123");
        dao.insertar(u, hash);
        u.setNombres("Nombre Nuevo");
        u.setRol("ALMACEN");
        u.setIdArea(5);
        u.setUsername("otro_intento");
        dao.actualizar(u);
        Usuario g = dao.buscarPorId(u.getIdUsuario()).orElseThrow();
        assertEquals("Nombre Nuevo", g.getNombres());
        assertEquals("ALMACEN", g.getRol());
        assertEquals(5, g.getIdArea());
        assertEquals("tst_usr_4", g.getUsername(), "el nombre de usuario no se actualiza");
        assertEquals(hash, dao.buscarPorUsername("tst_usr_4").orElseThrow().getPasswordHash(), "la clave no cambia");
    }

    @Test
    void cambiarEstadoDesactivaYActivaSinBorrarLaFila() throws Exception {
        Usuario u = nuevo("tst_usr_5", "ATENCION", 1);
        dao.insertar(u, Seguridad.hashear("Temporal123"));
        dao.cambiarEstado(u.getIdUsuario(), false);
        assertFalse(dao.buscarPorId(u.getIdUsuario()).orElseThrow().isEstado());
        assertEquals(1, contar("SELECT COUNT(*) FROM usuario WHERE id_usuario = ?", u.getIdUsuario()), "la fila sigue existiendo");
        assertTrue(dao.listarFuncionariosPorArea(1).stream().noneMatch(x -> x.getUsername().equals("tst_usr_5")),
                "un usuario inactivo no aparece como funcionario");
        dao.cambiarEstado(u.getIdUsuario(), true);
        assertTrue(dao.buscarPorId(u.getIdUsuario()).orElseThrow().isEstado());
    }

    @Test
    void activarUnaCuentaBloqueadaReiniciaLosIntentos() throws Exception {
        for (int i = 0; i < 5; i++) {
            dao.registrarIntentoFallido(idAlmacen);
        }
        assertFalse(dao.buscarPorId(idAlmacen).orElseThrow().isEstado());
        dao.cambiarEstado(idAlmacen, true);
        Usuario u = dao.buscarPorId(idAlmacen).orElseThrow();
        assertTrue(u.isEstado());
        assertEquals(0, u.getIntentosFallidos(), "no debe quedar a un fallo de bloquearse otra vez");
    }

    @Test
    void desbloquearPoneLosIntentosEnCeroYReactiva() {
        for (int i = 0; i < 5; i++) {
            dao.registrarIntentoFallido(idAlmacen);
        }
        assertTrue(dao.desbloquear(idAlmacen));
        assertEquals(0, dao.buscarPorId(idAlmacen).orElseThrow().getIntentosFallidos());
        assertFalse(dao.desbloquear(idAlmacen), "ya no está bloqueada");
    }

    @Test
    void actualizarPasswordGuardaElHashYElIndicadorDeCambioObligatorio() throws Exception {
        Usuario u = nuevo("tst_usr_6", "ALMACEN", null);
        dao.insertar(u, Seguridad.hashear("Temporal123"));
        String nuevoHash = Seguridad.hashear("Definitiva456");
        dao.actualizarPassword(u.getIdUsuario(), nuevoHash, false);
        Usuario g = dao.buscarPorUsername("tst_usr_6").orElseThrow();
        assertFalse(g.isDebeCambiarPassword());
        assertTrue(Seguridad.verificar("Definitiva456", g.getPasswordHash()));
        assertFalse(Seguridad.verificar("Temporal123", g.getPasswordHash()));
        dao.actualizarPassword(u.getIdUsuario(), Seguridad.hashear("Otra789xyz"), true);
        assertTrue(dao.buscarPorUsername("tst_usr_6").orElseThrow().isDebeCambiarPassword());
    }

    @Test
    void laMigracionDeUnHashAntiguoGuardaElFormatoNuevoYSigueVerificando() {
        int id = idDe("atencion1");
        String antiguo = dao.buscarPorUsername("atencion1").orElseThrow().getPasswordHash();
        try {
            assertTrue(Seguridad.esLegado(antiguo), "los usuarios de ejemplo tienen el hash antiguo");
            assertTrue(Seguridad.verificar("Atencion123", antiguo));
            dao.actualizarPassword(id, Seguridad.hashear("Atencion123"), false);
            String migrado = dao.buscarPorUsername("atencion1").orElseThrow().getPasswordHash();
            assertFalse(Seguridad.esLegado(migrado));
            assertTrue(Seguridad.verificar("Atencion123", migrado), "las mismas contraseñas siguen entrando");
        } finally {
            dao.actualizarPassword(id, antiguo, false);   // se deja la base como estaba
        }
    }

    // ---------- siempre debe quedar al menos un ADMIN activo ----------

    @Test
    void noSePuedeDesactivarAlUltimoAdminActivo() {
        int idAdmin = idDe("admin");
        assertEquals(1, dao.contarAdminsActivos(), "los datos de ejemplo tienen un solo ADMIN");
        assertThrows(UltimoAdminException.class, () -> dao.cambiarEstado(idAdmin, false));
        assertTrue(dao.buscarPorId(idAdmin).orElseThrow().isEstado(), "sigue activo");
    }

    @Test
    void noSePuedeQuitarElRolAlUltimoAdminActivo() {
        Usuario admin = dao.buscarPorId(idDe("admin")).orElseThrow();
        admin.setRol("ATENCION");
        admin.setIdArea(1);
        assertThrows(UltimoAdminException.class, () -> dao.actualizar(admin));
        assertEquals("ADMIN", dao.buscarPorId(admin.getIdUsuario()).orElseThrow().getRol(), "sigue siendo ADMIN");
    }

    @Test
    void conUnSegundoAdminActivoSiSePuedeDesactivarAlPrimero() throws Exception {
        Usuario segundo = nuevo("tst_usr_admin2", "ADMIN", null);
        dao.insertar(segundo, Seguridad.hashear("Temporal123"));
        assertEquals(2, dao.contarAdminsActivos());
        dao.cambiarEstado(idDe("admin"), false);
        assertEquals(1, dao.contarAdminsActivos());
        assertThrows(UltimoAdminException.class, () -> dao.cambiarEstado(segundo.getIdUsuario(), false),
                "ahora el segundo es el último");
    }

    @Test
    void unAdminInactivoNoCuentaComoAdminActivo() throws Exception {
        Usuario segundo = nuevo("tst_usr_admin3", "ADMIN", null);
        dao.insertar(segundo, Seguridad.hashear("Temporal123"));
        dao.cambiarEstado(segundo.getIdUsuario(), false);            // desactivar a uno que no es el último: permitido
        assertEquals(1, dao.contarAdminsActivos());
        dao.cambiarEstado(segundo.getIdUsuario(), false);            // ya inactivo: no cambia nada ni lanza excepción
    }

    @Test
    void desactivarOCambiarElRolDeUsuariosQueNoSonAdminNoSeVeAfectadoPorLaRegla() throws Exception {
        Usuario u = nuevo("tst_usr_7", "ATENCION", 1);
        dao.insertar(u, Seguridad.hashear("Temporal123"));
        dao.cambiarEstado(u.getIdUsuario(), false);
        u.setRol("ALMACEN");
        dao.actualizar(u);
        assertEquals("ALMACEN", dao.buscarPorId(u.getIdUsuario()).orElseThrow().getRol());
    }

    // ---------- listar con filtros ----------

    @Test
    void listarSinFiltrosTraeTodosOrdenadosPorNombreSinElHash() throws Exception {
        List<Usuario> todos = dao.listar(null, null, null);
        assertEquals(contar("SELECT COUNT(*) FROM usuario"), todos.size());
        assertTrue(todos.stream().allMatch(u -> u.getPasswordHash() == null), "nunca se devuelve el hash");
        for (int i = 1; i < todos.size(); i++) {
            assertTrue(todos.get(i - 1).getNombres().compareToIgnoreCase(todos.get(i).getNombres()) <= 0, "orden por nombre");
        }
    }

    @Test
    void listarFiltraPorTextoRolYEstado() throws Exception {
        assertEquals(List.of("admin"), dao.listar("Administrador", null, null).stream().map(Usuario::getUsername).toList());
        assertEquals(List.of("almacen1"), dao.listar("almacen", "ALMACEN", true).stream().map(Usuario::getUsername).toList());
        assertEquals(2, dao.listar(null, "ATENCION", null).size());
        assertTrue(dao.listar("almacen", "ADMIN", null).isEmpty(), "los filtros se combinan");
        Usuario u = nuevo("tst_usr_8", "ATENCION", 1);
        dao.insertar(u, Seguridad.hashear("Temporal123"));
        dao.cambiarEstado(u.getIdUsuario(), false);
        List<Usuario> inactivos = dao.listar(null, null, false);
        assertEquals(List.of("tst_usr_8"), inactivos.stream().map(Usuario::getUsername).toList());
        assertTrue(dao.listar(null, null, true).stream().noneMatch(x -> x.getUsername().equals("tst_usr_8")));
    }

    @Test
    void listarTrataElTextoComoTextoNoComoSql() {
        assertTrue(dao.listar("' OR '1'='1", null, null).isEmpty());
        assertTrue(dao.listar("%", null, null).isEmpty(), "el % escrito por el usuario no es un comodín");
        assertTrue(dao.listar("x'; DROP TABLE usuario; --", null, null).isEmpty());
        assertEquals(contar("SELECT COUNT(*) FROM usuario"), dao.listar("", "", null).size(), "vacío = sin filtro");
    }

    @Test
    void unFuncionarioDesactivadoNoAparece() {
        int idAtencion1 = dao.buscarPorUsername("atencion1").orElseThrow().getIdUsuario();
        try {
            ejecutar("UPDATE usuario SET estado = 0 WHERE id_usuario = ?", idAtencion1);
            assertTrue(dao.listarFuncionariosPorArea(1).isEmpty());
        } finally {
            ejecutar("UPDATE usuario SET estado = 1 WHERE id_usuario = ?", idAtencion1);
        }
    }
}

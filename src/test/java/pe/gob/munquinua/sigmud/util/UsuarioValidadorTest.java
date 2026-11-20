package pe.gob.munquinua.sigmud.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import pe.gob.munquinua.sigmud.model.Usuario;

/** Pruebas de UsuarioValidador (datos que escribe el ADMIN), sin base de datos. */
class UsuarioValidadorTest {

    private Usuario valido() {
        Usuario u = new Usuario();
        u.setUsername("juan.perez");
        u.setNombres("Juan Pérez Rojas");
        u.setRol("ATENCION");
        u.setIdArea(1);
        return u;
    }

    private boolean hayError(List<String> errores, String fragmento) {
        return errores.stream().anyMatch(e -> e.contains(fragmento));
    }

    @Test
    void unUsuarioValidoNoTieneErrores() {
        assertEquals(List.of(), UsuarioValidador.validar(valido(), true));
    }

    @Test
    void elNombreDeUsuarioDebeTenerDe3A30CaracteresPermitidos() {
        Usuario u = valido();
        for (String malo : new String[] {"ab", "a b c", "juan@perez", "x".repeat(31), "", null, "ñandu"}) {
            u.setUsername(malo);
            assertTrue(hayError(UsuarioValidador.validar(u, true), "nombre de usuario"), "usuario " + malo);
        }
        for (String bueno : new String[] {"abc", "a_b.c-d", "x".repeat(30)}) {
            u.setUsername(bueno);
            assertEquals(List.of(), UsuarioValidador.validar(u, true), "usuario " + bueno);
        }
    }

    @Test
    void enUnaEdicionElNombreDeUsuarioNoSeValida() {
        Usuario u = valido();
        u.setUsername("x");
        assertEquals(List.of(), UsuarioValidador.validar(u, false));
    }

    @Test
    void elNombreCompletoEsObligatorioDe3A100Caracteres() {
        Usuario u = valido();
        u.setNombres("ab");
        assertTrue(hayError(UsuarioValidador.validar(u, true), "nombre completo"));
        u.setNombres("   ");
        assertTrue(hayError(UsuarioValidador.validar(u, true), "nombre completo"));
        u.setNombres("a".repeat(101));
        assertTrue(hayError(UsuarioValidador.validar(u, true), "nombre completo"));
        u.setNombres("a".repeat(100));
        assertEquals(List.of(), UsuarioValidador.validar(u, true));
    }

    @Test
    void elRolDebeSerUnoDeLosTresDelSistema() {
        Usuario u = valido();
        for (String malo : new String[] {"", null, "SUPERADMIN", "admin", "Atencion"}) {
            u.setRol(malo);
            assertTrue(hayError(UsuarioValidador.validar(u, true), "rol válido"), "rol " + malo);
        }
        for (String bueno : new String[] {"ADMIN", "ATENCION", "ALMACEN"}) {
            u.setRol(bueno);
            u.setIdArea(1);
            assertEquals(List.of(), UsuarioValidador.validar(u, true), "rol " + bueno);
        }
    }

    @Test
    void atencionExigeUnArea() {
        Usuario u = valido();
        u.setIdArea(null);
        assertTrue(hayError(UsuarioValidador.validar(u, true), "área asignada"));
        u.setIdArea(0);
        assertTrue(hayError(UsuarioValidador.validar(u, true), "área asignada"));
    }

    @Test
    void adminYAlmacenPuedenIrSinArea() {
        Usuario u = valido();
        u.setIdArea(null);
        u.setRol("ADMIN");
        assertEquals(List.of(), UsuarioValidador.validar(u, true));
        u.setRol("ALMACEN");
        assertEquals(List.of(), UsuarioValidador.validar(u, true));
    }

    @Test
    void unUsuarioNuloNoLanzaExcepcion() {
        assertEquals(1, UsuarioValidador.validar(null, true).size());
    }
}

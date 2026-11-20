package pe.gob.munquinua.sigmud.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.gob.munquinua.sigmud.dao.impl.CitaDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.ContribuyenteDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.MaterialDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.MovimientoDAOImpl;
import pe.gob.munquinua.sigmud.dao.impl.UsuarioDAOImpl;
import pe.gob.munquinua.sigmud.model.Cita;
import pe.gob.munquinua.sigmud.model.Contribuyente;
import pe.gob.munquinua.sigmud.model.Material;
import pe.gob.munquinua.sigmud.model.MovimientoStock;
import pe.gob.munquinua.sigmud.model.Pagina;
import pe.gob.munquinua.sigmud.model.Usuario;

/**
 * Pruebas de los listados paginados y ordenables (mun_quinua_test): LIMIT/OFFSET, conteo, corrección de página y
 * tamaño, orden por columna y lista blanca contra inyección SQL. Datos propios con prefijo "TSTPAG" que se borran al final.
 */
class ListadosPaginadosDAOTest extends BaseDaoTest {

    private final ContribuyenteDAO contribuyentes = new ContribuyenteDAOImpl();
    private final MaterialDAO materiales = new MaterialDAOImpl();
    private final UsuarioDAO usuarios = new UsuarioDAOImpl();
    private final MovimientoDAO movimientos = new MovimientoDAOImpl();
    private final CitaDAO citas = new CitaDAOImpl();
    private int idMaterial;
    private int idContribuyente;

    @BeforeEach
    void preparar() {
        limpiar();
        // 25 contribuyentes TSTPAG01..TSTPAG25 (DNI 99100001..99100025)
        for (int i = 1; i <= 25; i++) {
            ejecutar("INSERT INTO contribuyente (tipo_persona, dni_ruc, nombres_razon_social) VALUES ('NATURAL', ?, ?)",
                    String.format("991%05d", i), String.format("TSTPAG%02d", i));
        }
        // 12 materiales TSTPAG-M01..M12
        for (int i = 1; i <= 12; i++) {
            ejecutar("INSERT INTO material (codigo, nombre, unidad, stock, stock_minimo) VALUES (?, ?, 'UNIDAD', ?, 5)",
                    String.format("TSTPAG-M%02d", i), String.format("TSTPAG Material %02d", i), i);   // stock 1..12
        }
        idMaterial = contar("SELECT id_material FROM material WHERE codigo = 'TSTPAG-M01'");
        // 13 movimientos y 13 citas para el primer contribuyente / material
        int idUsuario = contar("SELECT id_usuario FROM usuario WHERE username = 'almacen1'");
        idContribuyente = contar("SELECT id_contribuyente FROM contribuyente WHERE dni_ruc = '99100001'");
        int funcionario = contar("SELECT id_usuario FROM usuario WHERE username = 'atencion1'");
        for (int i = 1; i <= 13; i++) {
            ejecutar("INSERT INTO movimiento_stock (id_material, tipo, cantidad, id_usuario, observacion) VALUES (?, 'ENTRADA', ?, ?, ?)",
                    idMaterial, i, idUsuario, "TSTPAG");
            ejecutar("INSERT INTO cita (id_contribuyente, id_area, id_funcionario, ventanilla, fecha, hora, motivo, estado) "
                    + "VALUES (?, 1, ?, 1, ?, ?, 'TSTPAG', ?)", idContribuyente, funcionario,
                    Date.valueOf(LocalDate.now().plusDays(100 + i)), Time.valueOf(LocalTime.of(5, 0)), i % 3 == 0 ? "CANCELADA" : "PROGRAMADA");
        }
    }

    @AfterEach
    void limpiar() {
        ejecutar("DELETE FROM cita WHERE motivo = 'TSTPAG'");
        ejecutar("DELETE FROM movimiento_stock WHERE observacion = 'TSTPAG'");
        ejecutar("DELETE FROM material WHERE codigo LIKE 'TSTPAG-%'");
        ejecutar("DELETE FROM contribuyente WHERE dni_ruc LIKE '991%'");
        ejecutar("TRUNCATE TABLE auditoria");
    }

    private List<String> nombres(Pagina<Contribuyente> p) {
        return p.getItems().stream().map(Contribuyente::getNombresRazonSocial).toList();
    }

    // ---------- contribuyentes ----------

    @Test
    void paginaYCuentaConLimitYOffset() {
        Pagina<Contribuyente> p1 = contribuyentes.listarPagina("TSTPAG", false, 1, 10, "nombre", "asc");
        assertEquals(25, p1.getTotalRegistros());
        assertEquals(3, p1.getTotalPaginas());
        assertEquals(10, p1.getItems().size());
        assertEquals("TSTPAG01", p1.getItems().get(0).getNombresRazonSocial());
        Pagina<Contribuyente> p3 = contribuyentes.listarPagina("TSTPAG", false, 3, 10, "nombre", "asc");
        assertEquals(5, p3.getItems().size());
        assertEquals("TSTPAG21", p3.getItems().get(0).getNombresRazonSocial());
        assertEquals(21, p3.getDesde());
        assertEquals(25, p3.getHasta());
        assertEquals(20, contribuyentes.listarPagina("TSTPAG", false, 1, 20, "nombre", "asc").getItems().size());
        assertEquals(25, contribuyentes.listarPagina("TSTPAG", false, 1, 50, "nombre", "asc").getItems().size());
    }

    @Test
    void laPaginaFueraDeRangoYElTamanoInvalidoSeCorrigen() {
        assertEquals(1, contribuyentes.listarPagina("TSTPAG", false, -5, 10, "nombre", "asc").getPaginaActual());
        Pagina<Contribuyente> ultima = contribuyentes.listarPagina("TSTPAG", false, 999999, 10, "nombre", "asc");
        assertEquals(3, ultima.getPaginaActual(), "se corrige a la última página");
        assertEquals(5, ultima.getItems().size());
        Pagina<Contribuyente> grande = contribuyentes.listarPagina("TSTPAG", false, 1, 100000, "nombre", "asc");
        assertEquals(10, grande.getTamano(), "tamaño inválido = 10");
        assertEquals(10, grande.getItems().size());
        assertEquals(3, contribuyentes.listarPagina("TSTPAG", false, Integer.MAX_VALUE, 10, "nombre", "asc").getPaginaActual());
    }

    @Test
    void ordenaPorColumnaYDireccion() {
        List<String> asc = nombres(contribuyentes.listarPagina("TSTPAG", false, 1, 10, "nombre", "asc"));
        List<String> desc = nombres(contribuyentes.listarPagina("TSTPAG", false, 1, 10, "nombre", "desc"));
        assertEquals("TSTPAG01", asc.get(0));
        assertEquals("TSTPAG25", desc.get(0));
        List<String> porDni = nombres(contribuyentes.listarPagina("TSTPAG", false, 1, 10, "dniRuc", "desc"));
        assertEquals("TSTPAG25", porDni.get(0), "ordenar por dniRuc descendente");
    }

    @Test
    void unaColumnaConInyeccionUsaElOrdenPorDefectoYNoRompeNada() {
        String[] ataques = {"nombre;DROP TABLE contribuyente", "1 OR 1=1", "nombre--", "' OR '1'='1", "(SELECT 1)", "password_hash"};
        for (String ataque : ataques) {
            Pagina<Contribuyente> p = contribuyentes.listarPagina("TSTPAG", false, 1, 10, ataque, "desc; DROP TABLE contribuyente");
            assertEquals("TSTPAG01", p.getItems().get(0).getNombresRazonSocial(), "orden por defecto (nombre asc) con: " + ataque);
        }
        assertEquals(25, contar("SELECT COUNT(*) FROM contribuyente WHERE dni_ruc LIKE '991%'"), "la tabla sigue intacta");
    }

    @Test
    void incluyeInactivosSoloSiSePide() {
        contribuyentes.darDeBaja(idContribuyente);
        assertEquals(24, contribuyentes.listarPagina("TSTPAG", false, 1, 50, "nombre", "asc").getTotalRegistros());
        assertEquals(25, contribuyentes.listarPagina("TSTPAG", true, 1, 50, "nombre", "asc").getTotalRegistros());
    }

    @Test
    void elTextoConComodinesSeBuscaComoTexto() {
        assertEquals(0, contribuyentes.listarPagina("%", false, 1, 10, "nombre", "asc").getItems().stream()
                .filter(c -> c.getNombresRazonSocial().startsWith("TSTPAG")).count());
    }

    // ---------- materiales ----------

    @Test
    void paginaYOrdenaMateriales() {
        Pagina<Material> p = materiales.listarPagina("TSTPAG", false, 2, 10, "stock", "desc");
        assertEquals(12, p.getTotalRegistros());
        assertEquals(2, p.getItems().size());
        assertEquals(2, p.getItems().get(0).getStock(), "orden por stock descendente: la página 2 trae 2 y 1");
        Pagina<Material> criticos = materiales.listarPagina("TSTPAG", true, 1, 50, "stock", "asc");
        assertEquals(5, criticos.getTotalRegistros(), "stock 1..5 es <= stock mínimo 5");
        assertEquals(12, materiales.listarPagina("TSTPAG", false, 1, 50, "x;DROP TABLE material", "asc").getTotalRegistros());
    }

    // ---------- usuarios ----------

    @Test
    void paginaYOrdenaUsuarios() {
        Pagina<Usuario> todos = usuarios.listarPagina("", "", null, 1, 10, "username", "asc");
        assertTrue(todos.getTotalRegistros() >= 4);
        assertEquals("admin", todos.getItems().get(0).getUsername());
        Pagina<Usuario> desc = usuarios.listarPagina("", "", null, 1, 10, "username", "desc");
        assertNotNull(desc.getItems().get(0));
        Pagina<Usuario> solo = usuarios.listarPagina("", "ADMIN", Boolean.TRUE, 1, 10, "nombre;DROP TABLE usuario", "asc");
        assertTrue(solo.getItems().stream().allMatch(u -> "ADMIN".equals(u.getRol())));
        assertTrue(contar("SELECT COUNT(*) FROM usuario") >= 4, "la tabla usuario sigue intacta");
    }

    // ---------- historial de movimientos ----------

    @Test
    void paginaElHistorialDeMovimientos() {
        Pagina<MovimientoStock> p = movimientos.listarPorMaterialPagina(idMaterial, 2, 10, "cantidad", "asc");
        assertEquals(13, p.getTotalRegistros());
        assertEquals(3, p.getItems().size());
        assertEquals(11, p.getItems().get(0).getCantidad());
        Pagina<MovimientoStock> porDefecto = movimientos.listarPorMaterialPagina(idMaterial, 1, 10, "x' OR '1'='1", "asc");
        assertEquals(10, porDefecto.getItems().size());
        assertEquals(13, movimientos.listarPorMaterialPagina(idMaterial, 99, 10, "", "").getTotalRegistros());
    }

    // ---------- historial de citas (ficha) ----------

    @Test
    void paginaElHistorialDeCitasConFiltroDeEstado() {
        Pagina<Cita> todas = citas.listarPorContribuyentePagina(idContribuyente, null, 1, 10, "fecha", "desc");
        assertEquals(13, todas.getTotalRegistros());
        assertEquals(10, todas.getItems().size());
        assertEquals(LocalDate.now().plusDays(113), todas.getItems().get(0).getFecha(), "la más reciente primero");
        assertNotNull(todas.getItems().get(0).getNombreArea());
        Pagina<Cita> canceladas = citas.listarPorContribuyentePagina(idContribuyente, "CANCELADA", 1, 10, "fecha", "asc");
        assertEquals(4, canceladas.getTotalRegistros(), "las citas 3, 6, 9 y 12");
        assertEquals(LocalDate.now().plusDays(103), canceladas.getItems().get(0).getFecha());
        Pagina<Cita> ataque = citas.listarPorContribuyentePagina(idContribuyente, "PROGRAMADA' OR '1'='1", 1, 10, "area;DROP", "asc");
        assertEquals(0, ataque.getTotalRegistros());
    }
}

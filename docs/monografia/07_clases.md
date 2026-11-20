# 07. Diagramas de clases (Capítulo III, sección 3.8)

Seis diagramas UML (texto PlantUML en `diagramas/`), con un máximo de 15 clases cada uno. Muestran los atributos y métodos principales (no
todos los getters y setters) y las relaciones reales entre POJOs, interfaces DAO, implementaciones, Servlets, validadores y excepciones.

| Diagrama | Contenido | Clases que aparecen |
|---|---|---|
| `diagramas/clases_contribuyentes.puml` | Módulo de contribuyentes y paginación | `Contribuyente`, `Pagina`, `ContribuyenteDAO`, `ContribuyenteDAOImpl`, `ContribuyenteServlet`, `ContribuyenteValidador`, `Validador`, `OrdenSql`, `Paginador`, `RegistroDuplicadoException`, `DaoException` |
| `diagramas/clases_almacen.puml` | Materiales, movimientos de stock y reportes | `Material`, `MovimientoStock`, `MaterialDAO`, `MovimientoDAO`, `ReporteAlmacenDAO` y sus implementaciones, `MaterialServlet`, `MovimientoServlet`, `ReporteAlmacenServlet`, `MaterialValidador`, `StockInsuficienteException`, `MaterialNoDisponibleException`, filas de reporte |
| `diagramas/clases_citas.puml` | Citas, áreas y reporte de citas | `Cita`, `Area`, `CitaDAO`, `AreaDAO`, `ReporteCitasDAO` y sus implementaciones, `CitaServlet`, `CitaAjaxServlet`, `ReporteCitasServlet`, `CitaValidador`, `CitaCruzadaException`, `CitaEstadoException`, `CitaNoEncontradaException` |
| `diagramas/clases_usuarios_seguridad.puml` | Acceso, usuarios, contraseñas | `Usuario`, `UsuarioDAO`, `UsuarioDAOImpl`, `LoginServlet`, `LogoutServlet`, `PerfilServlet`, `UsuarioServlet`, `UsuarioAjaxServlet`, `Seguridad`, `UsuarioValidador`, `Validador`, excepciones de acceso |
| `diagramas/clases_auditoria_paneles.puml` | Auditoría y paneles | `RegistroAuditoria`, `FiltrosAuditoria`, `AuditoriaDAO`, `Auditoria`, `AuditoriaServlet`, `DashboardDAO`, `PanelDatos`, `DashboardServlet`, `DashboardDatosServlet`, `DashboardDesbloquearServlet`, `EtiquetaValor`, `PuntoDiario` |
| `diagramas/clases_infraestructura.puml` | Filtros, listeners, utilidades y jerarquía de excepciones | `SecurityHeadersFilter`, `AuthFilter`, `CsrfFilter`, `SesionListener`, `ContextoListener`, `ConexionBD`, `Csv`, `ListaVista`, `FiltrosReporte`, `TablaReporte`, `DaoException` y las excepciones de negocio |

## Convenciones de lectura
- Estereotipos entre `<< >>`: `model`, `dao`, `dao.impl`, `controller`, `util`, `exception`, `filter`, `listener` (coinciden con los
  paquetes reales).
- `<|..` = una clase implementa una interfaz DAO; `-->` = usa; `..>` = depende o lanza.
- **Excepciones:** `DaoException` extiende `RuntimeException` (error técnico, se registra en el log y el usuario recibe un mensaje
  genérico); el resto extiende `Exception` (errores de negocio con mensaje apto para el usuario).
- Los POJOs son clases con atributos privados, constructor vacío, getters y setters; los de la capa de modelo no dependen de ninguna otra
  capa.

## Clases del paquete `model` (17)
`Area`, `Cita`, `Contribuyente`, `EtiquetaValor`, `FilaCitasResumen`, `FilaResumenMaterial`, `FilaSalidaArea`, `FilaStockCritico`,
`FiltrosAuditoria`, `Material`, `MovimientoStock`, `Pagina`, `PuntoDiario`, `RegistroAuditoria`, `ResumenCitas`, `TablaReporte`, `Usuario`.
Las entidades que corresponden a tablas son `Area`, `Cita`, `Contribuyente`, `Material`, `MovimientoStock`, `RegistroAuditoria` y `Usuario`;
las demás son filas de reportes, filtros o contenedores.

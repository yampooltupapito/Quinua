# 06. Arquitectura del sistema (Capítulo III, sección 3.7)

## 1. Diagramas
- `diagramas/arquitectura_capas.puml` — capas: Vista (JSP) → Controlador (Servlet) → DAO → MySQL, con filtros, listeners, utilidades y modelo.
- `diagramas/paquetes.puml` — paquetes de `pe.gob.munquinua.sigmud` y sus dependencias.

## 2. Descripción de las capas
| Capa | Componentes reales | Responsabilidad |
|---|---|---|
| **Filtros** | `SecurityHeadersFilter`, `AuthFilter`, `CsrfFilter` | Cabeceras de seguridad, sesión y rol por ruta, token CSRF |
| **Listeners** | `SesionListener`, `ContextoListener` | Contar sesiones activas; detectar al arrancar si existe el logo (`logoDisponible`) |
| **Control** | 16 Servlets (`controller`) | Leer parámetros, validar, llamar al DAO, auditar, elegir la vista o redirigir (Post-Redirect-Get) o responder JSON |
| **Vista** | 29 JSP, 3 tag files, JS y CSS en `assets` | Mostrar datos con JSTL y Bootstrap; sin scriptlets ni JavaScript dentro del HTML |
| **Negocio y utilidades** | `util` (validadores, `Seguridad`, `Auditoria`, `OrdenSql`, `Paginador`, `Csv`, `PanelDatos`, ...) | Reglas de validación y servicios comunes |
| **Acceso a datos (DAO)** | 10 interfaces en `dao`, 10 implementaciones en `dao.impl`, `ConexionBD` | SQL con `PreparedStatement`, transacciones, traducción de errores de MySQL |
| **Modelo** | 17 clases en `model` (POJOs y filas de reporte) | Datos que viajan entre capas, incluida la página genérica `Pagina<T>` |
| **Excepciones** | 10 clases en `exception` | Errores de negocio con mensaje apto para el usuario y `DaoException` técnica |
| **Datos** | MySQL 8.4, esquema `mun_quinua` | Persistencia, restricciones y triggers |

## 3. Ciclo de vida de una petición

Los filtros están declarados en `web.xml` y su orden de ejecución es el de los `filter-mapping`:

1. **`SecurityHeadersFilter`** (siempre, también en páginas de error): agrega `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`,
   `Referrer-Policy: same-origin` y la CSP (`default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;
   font-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'`).
2. **`AuthFilter`** (autenticación y autorización): `/login` y `/assets/*` pasan; sin sesión → redirige a `/login` (o responde 401 JSON en
   `/dashboard/*`); con contraseña temporal solo se permite `/perfil/password` y `/logout`; si el rol no está permitido para la ruta → 403;
   agrega cabeceras anti-caché a las páginas protegidas.
3. **`CsrfFilter`**: en todo `POST` (excepto `/login`) compara en tiempo constante el token recibido (parámetro `csrf` o cabecera
   `X-CSRF-Token`) con el de la sesión; si falta o no coincide → 403.
4. **Servlet** de la ruta (`doGet` o `doPost`): valida, usa el DAO, audita y responde.
5. **JSP** (por `forward`) o **redirección** (`sendRedirect`) o **JSON** (Gson).
6. En caso de error no controlado, `web.xml` envía a `error.jsp` (códigos 400, 401, 403, 404, 405, 500 y `Throwable`).

> **Nota:** en el encargo se mencionó el orden "seguridad, CSRF, autenticación", pero el orden real implementado es
> **cabeceras → autenticación y roles → CSRF** (`web.xml`). Este documento describe el orden real. Ver `observaciones.md`.

## 4. Árbol de paquetes real (`src/main/java/pe/gob/munquinua/sigmud`)

```
sigmud
├── controller   AuditoriaServlet, CitaAjaxServlet, CitaServlet, ContribuyenteServlet, DashboardDatosServlet,
│                DashboardDesbloquearServlet, DashboardServlet, LoginServlet, LogoutServlet, MaterialServlet,
│                MovimientoServlet, PerfilServlet, ReporteAlmacenServlet, ReporteCitasServlet, UsuarioAjaxServlet, UsuarioServlet
├── dao          AreaDAO, AuditoriaDAO, CitaDAO, ContribuyenteDAO, DashboardDAO, MaterialDAO, MovimientoDAO,
│                ReporteAlmacenDAO, ReporteCitasDAO, UsuarioDAO
│   └── impl     (una implementación JDBC por cada interfaz: AreaDAOImpl ... UsuarioDAOImpl)
├── exception    CitaCruzadaException, CitaEstadoException, CitaNoEncontradaException, CredencialesInvalidasException,
│                CuentaBloqueadaException, DaoException, MaterialNoDisponibleException, RegistroDuplicadoException,
│                StockInsuficienteException, UltimoAdminException
├── filter       AuthFilter, CsrfFilter, SecurityHeadersFilter
├── listener     ContextoListener, SesionListener
├── model        Area, Cita, Contribuyente, EtiquetaValor, FilaCitasResumen, FilaResumenMaterial, FilaSalidaArea,
│                FilaStockCritico, FiltrosAuditoria, Material, MovimientoStock, Pagina, PuntoDiario, RegistroAuditoria,
│                ResumenCitas, TablaReporte, Usuario
└── util         Auditoria, CitaValidador, ConexionBD, ContribuyenteValidador, Csv, FiltrosReporte, ListaVista,
                 MaterialValidador, OrdenSql, PanelDatos, Paginador, Seguridad, UsuarioValidador, Validador
```

Otros directorios del proyecto:
```
quinua
├── pom.xml, desplegar.ps1, pruebas-seguridad.ps1, CLAUDE.md, nb-configuration.xml
├── sql/        02_tablas_sigmud.sql, 03_bd_pruebas.sql, 04_usuarios.sql, 05_auditoria.sql
├── docs/       avance.md, auditoria.md, monografia/ (esta documentación)
├── tools/      LimpiarPruebaSeguridad.java
└── src
    ├── main/resources/db.properties
    ├── main/webapp/        index.jsp, assets/{css,js,img,fonts}, WEB-INF/{web.xml, views/, tags/}
    └── test/java/..., test/resources/db.properties
```

## 5. Tabla de rutas (URL, método, Servlet y roles)

Roles tomados de `AuthFilter.ROLES_POR_RUTA` (una ruta hereda el rol de su prefijo, por ejemplo `/almacen/reportes` hereda `/almacen`).
"Cualquier rol" = con sesión, sin restricción de rol. Los métodos son los `doGet`/`doPost` que cada Servlet implementa.

| URL | Método | Servlet | Roles permitidos | Qué hace |
|---|---|---|---|---|
| `/` (`index.jsp`) | GET | — (JSP) | pública | Redirige a `/menu` |
| `/login` | GET, POST | `LoginServlet` | pública | Formulario y autenticación (POST sin token CSRF) |
| `/logout` | GET | `LogoutServlet` | cualquier rol | Cierra la sesión |
| `/menu` | GET | `DashboardServlet` | cualquier rol | Panel según el rol (admin, atención o almacén) |
| `/perfil` | GET | `PerfilServlet` | cualquier rol | Mis datos |
| `/perfil/password` | GET, POST | `PerfilServlet` | cualquier rol | Cambiar contraseña |
| `/contribuyentes` | GET (listar, nuevo, editar, ficha); POST (guardar, baja, reactivar) | `ContribuyenteServlet` | ADMIN, ATENCION | Padrón de contribuyentes y ficha |
| `/almacen` | GET (listar, nuevo, editar); POST (guardar, baja) | `MaterialServlet` | ADMIN, ALMACEN | Materiales |
| `/almacen/movimiento` | GET (historial); POST (registrar, responde JSON) | `MovimientoServlet` | ADMIN, ALMACEN | Entradas y salidas de stock |
| `/almacen/reportes` | GET (4 reportes; descarga CSV) | `ReporteAlmacenServlet` | ADMIN, ALMACEN | Reportes del almacén |
| `/citas` | GET (listar, nuevo, editar); POST (guardar, atender, cancelar) | `CitaServlet` | ADMIN, ATENCION | Citas |
| `/citas/ajax` | GET (contribuyentes, funcionarios, horas; JSON) | `CitaAjaxServlet` | ADMIN, ATENCION | Ayudas del formulario de citas |
| `/citas/reportes` | GET (descarga CSV con `accion=csv`) | `ReporteCitasServlet` | ADMIN, ATENCION | Reporte de citas |
| `/dashboard/datos` | GET (JSON) | `DashboardDatosServlet` | ADMIN, ATENCION, ALMACEN | Datos del panel del rol |
| `/dashboard/desbloquear` | POST (JSON) | `DashboardDesbloquearServlet` | ADMIN | Desbloquear una cuenta desde el panel |
| `/usuarios` | GET (listar, nuevo, editar); POST (guardar, estado) | `UsuarioServlet` | ADMIN | Gestión de usuarios |
| `/usuarios/restablecer` | POST (JSON) | `UsuarioAjaxServlet` | ADMIN | Restablecer contraseña |
| `/usuarios/desbloquear` | POST (JSON) | `UsuarioAjaxServlet` | ADMIN | Desbloquear cuenta |
| `/auditoria` | GET (consulta; `formato=csv` exporta) | `AuditoriaServlet` | ADMIN | Consulta de auditoría (solo lectura) |
| `/assets/*` | GET | (archivos estáticos) | pública | CSS, JS, imágenes y fuentes locales |

Total: 16 Servlets, 18 patrones de URL con Servlet (más `/` y `/assets/*`). Toda ruta no listada devuelve 404 con la página de error propia.

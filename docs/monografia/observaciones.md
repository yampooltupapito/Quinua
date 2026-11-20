# Observaciones: lo que no cuadra, lo que falta y mejoras

Lista de hallazgos hechos al preparar esta documentación (2026-11-19). **No se corrigió ninguno** (la consigna fue solo documentar); cada punto
indica dónde está y qué se sugiere. Todo lo escrito aquí se verificó contra el código, los scripts o los resultados de las pruebas.

## A. Estado de la instalación del usuario (importante antes de la sustentación)

| # | Observación | Detalle | Sugerencia |
|---|---|---|---|
| A1 | **La base real `mun_quinua` estaba desactualizada** | Al intentar iniciar sesión el sistema respondía "Ocurrió un error" porque el log decía `Unknown column 'u.debe_cambiar_password'`: faltaban los cambios de `sql/04_usuarios.sql` y `sql/05_auditoria.sql`. **En esta sesión de trabajo se aplicaron** (con el usuario `sigmud`, que tiene permisos sobre esa base): la columna `usuario.debe_cambiar_password`, el ensanche de `password_hash` a `VARCHAR(255)` y la tabla `auditoria`. No se modificó ningún dato de usuarios, contribuyentes, materiales ni citas | Dejar constancia en la monografía de que la base se actualizó con los scripts `04` y `05` |
| A2 | **Los triggers de auditoría no existen en la base real** | La creación de `trg_auditoria_no_update` y `trg_auditoria_no_delete` falló por falta de permisos (error 1419: se requiere SUPER con el registro binario activo). La tabla `auditoria` existe y la aplicación escribe en ella, pero **sin la protección de inmutabilidad** hasta que se ejecuten como root | Ejecutar como root la sección 1 de `sql/05_auditoria.sql` (líneas 21 a 59) y comprobar con `SHOW TRIGGERS` (captura 48) |
| A3 | **No existe `mun_quinua_test` en el MySQL del usuario** | Las pruebas de DAO no pueden correr allí. Las pruebas de este anexo se ejecutaron en una instancia temporal | Ejecutar `sql/03_bd_pruebas.sql` como root (necesita que exista el usuario `sigmud`) |
| A4 | **Tomcat del usuario apagado** | Al preparar esta documentación el puerto 8080 no tenía ningún servidor; un Tomcat levantado desde la sesión de trabajo fue cerrado por el sistema por falta de memoria | Volver a ejecutar `.\desplegar.ps1` antes de tomar las capturas |
| A5 | **No hay logo oficial** | `src/main/webapp/assets/img/` solo contiene `favicon.svg`; el sistema muestra el ícono genérico. Para el logo institucional hay que copiarlo a `assets/img/logo.png` | Obtener el logo oficial de la Municipalidad y reiniciar |
| A6 | **El proyecto no está bajo control de versiones** | La carpeta no es un repositorio Git | Considerar inicializar Git para el historial y la entrega |

## B. Inconsistencias entre la documentación existente y el sistema real

| # | Observación | Dónde |
|---|---|---|
| B1 | `docs/avance.md` (Módulo 1) menciona `controller/MenuServlet` y `views/menu.jsp`; **no existen**. `/menu` lo atiende `DashboardServlet` y muestra `dashboard/admin.jsp`, `atencion.jsp` o `almacen.jsp` | `docs/avance.md` línea 11 en adelante; `controller/DashboardServlet.java` |
| B2 | `docs/avance.md` aún lista como "Pendiente" los módulos de Contribuyentes, Almacén y Citas y la protección CSRF, que ya están hechos | `docs/avance.md`, sección "Pendiente" (final del archivo) |
| B3 | Los totales de pruebas escritos en `docs/avance.md` están desfasados (206 pruebas JUnit y 33 del script en la revisión inicial; 230 y 46 más adelante). Hoy son **376** y **207** | `docs/avance.md` secciones "Revisión de seguridad" y "Paneles" |
| B4 | `docs/auditoria.md` dice que "`REACTIVAR` está definido pero ninguna pantalla reactiva todavía contribuyentes ni materiales"; los **contribuyentes sí se reactivan** desde la ficha (los materiales no) | `docs/auditoria.md` §7, limitaciones |
| B5 | El enunciado de esta documentación describe el orden de filtros como "seguridad, CSRF, autenticación"; el orden real en `web.xml` es **cabeceras → autenticación y roles → CSRF** | `web.xml` líneas 34 a 49 |
| B6 | `docs/avance.md` describe la ruta de pruebas `/prueba-citas` ("abrir `/sigmud/prueba-citas`"); esa pantalla ya no existe (no hay Servlet con esa ruta). Los resultados de esa sección son de una ejecución anterior | `docs/avance.md`, Módulo 4 parte 1 |
| B7 | **No se pudo comparar con el documento del proyecto original del curso**: no fue entregado al generar esta documentación. Cualquier diferencia de alcance (por ejemplo módulos que el documento pida y el sistema no tenga, como trámite documentario, tributos, mercado, registro civil o Defensa Civil, declarados "fuera de alcance" en `CLAUDE.md`) debe revisarse a mano | — |

## C. Pruebas ausentes o pendientes

| # | Observación |
|---|---|
| C1 | **90 casos manuales** de `docs/avance.md` tienen el resultado "(pendiente)" (contribuyentes 10, materiales 10, movimientos 11, reportes de almacén 11, citas 15, usuarios 22, "Cómo probar" del Módulo 1: 11). Ver Anexo F, sección 6 |
| C2 | **No hay pruebas JUnit** de: `MaterialDAOImpl` (insertar, actualizar, baja, buscar por código), `ReporteAlmacenDAOImpl`, `ReporteCitasDAOImpl`, `AreaDAOImpl`, `FiltrosReporte` (límite de 366 días), los Servlets y los filtros (`AuthFilter`, `CsrfFilter`) |
| C3 | No hay pruebas automáticas del JavaScript (`movimientos.js`, `citas.js`, `usuarios.js`, `tema.js`, `toast.js`, validaciones jQuery) |
| C4 | No hay pruebas de carga, de concurrencia real (dos salidas simultáneas del mismo material con `FOR UPDATE`) ni de rendimiento |
| C5 | El script `pruebas-seguridad.ps1` no recorre los flujos de almacén (salida y entrada por la pantalla) ni la creación de citas por la pantalla, ni los reportes: esos flujos solo se prueban a nivel de DAO |
| C6 | **Defecto cosmético del script:** 15 casos ("GET /contribuyentes", "GET /almacen" y "GET /usuarios" con un parámetro `orden` hostil, 5 por listado) salen con la etiqueta vacía, porque en PowerShell `@("orden=" + X, "etiqueta")` concatena el arreglo en una sola cadena. La petición hostil sí se envía y el resultado es válido; solo falta el nombre | 
| C7 | Las pruebas de DAO necesitan una base `mun_quinua_test` y se niegan a correr contra otra (decisión correcta, pero implica un paso previo del instalador) |

## D. Código y diseño

| # | Observación | Dónde | Sugerencia |
|---|---|---|---|
| D1 | **Métodos DAO sin uso en producción** (solo se llaman desde pruebas): `ContribuyenteDAO.listar`, `listarConInactivos`, `buscarConInactivos`; `MaterialDAO.buscar`, `listarStockCritico`; `MovimientoDAO.listarPorMaterial`; `CitaDAO.listarPorContribuyente` (las dos versiones). Quedaron al introducir la paginación | `dao/` | Decidir si se retiran o se documentan como API |
| D2 | `MaterialDAO.reactivar` existe pero **ninguna pantalla reactiva materiales** | `MaterialServlet` solo tiene `guardar` y `baja` | Agregar la acción o quitar el método |
| D3 | **Sin pool de conexiones:** `ConexionBD.obtener()` abre una conexión nueva con `DriverManager` en cada operación | `util/ConexionBD.java` | Para más usuarios simultáneos, usar un `DataSource` con pool |
| D4 | **Reglas solo en la aplicación, no en la base:** RUC que empieza con 10/20, DNI solo con dígitos, teléfono de 9 dígitos que empieza con 9, ventanilla de 1 a 10, horario de 08:00 a 16:30, lista de unidades de material, formato del código `AAA-999`. El `CHECK` de la base solo valida la longitud del documento | `Validador`, `CitaValidador`, `MaterialValidador`, `sql/02_tablas_sigmud.sql` | Aceptable; documentarlo (ver `05_modelo_de_datos.md`) |
| D5 | **Redundancia `cita.id_area` / `usuario.id_area`:** que el funcionario pertenezca al área de la cita lo comprueba `CitaValidador`, no la base | `05_modelo_de_datos.md`, nota 3 | Documentar como decisión de diseño |
| D6 | **La ventanilla no tiene regla de cruce:** la única regla es por funcionario, fecha y hora (`UNIQUE`). Dos funcionarios distintos podrían agendar la misma ventanilla a la misma hora | `CitaDAOImpl`, `uq_cita_funcionario_horario` | Confirmar con la municipalidad si la ventanilla debe ser exclusiva |
| D7 | **Auditoría y paginación:** `AuditoriaServlet` pagina con su propio código (20 filas fijas), mientras los demás listados usan `Pagina<T>`/`Paginador` | `AuditoriaServlet.java` | Unificar si se desea |
| D8 | La cuenta se bloquea **por usuario, no por IP**: un tercero puede bloquear cuentas ajenas escribiendo mal la contraseña 5 veces. El cierre de sesión es por `GET`. Las sesiones abiertas siguen vigentes aunque se desactive al usuario. Sin HTTPS (la cookie no lleva `Secure`) | `docs/auditoria.md` §4 y §6 | Pasar a HTTPS y activar `<secure>true</secure>` en producción |
| D9 | `ON DELETE SET NULL` en `auditoria.id_usuario`: si algún día se borrara un usuario con SQL directo, los registros conservan el nombre pero pierden el id (los triggers no se disparan por acciones de claves foráneas). La aplicación nunca borra usuarios | `sql/05_auditoria.sql` | Documentado y probado (`AuditoriaDAOTest.siSeBorraElUsuarioElRegistroConservaElNombre`) |
| D10 | La edad de la ficha y las fechas de citas usan la hora del servidor | `docs/auditoria.md` §8 | — |

## E. Seguridad de datos de ejemplo

| # | Observación | Sugerencia |
|---|---|---|
| E1 | Los scripts `02` y `03` y el script de pruebas contienen las **contraseñas de ejemplo** de las cuentas de demostración (en comentarios y como valores por defecto) y los **hashes SHA-256 sin sal** correspondientes. En esta documentación esas contraseñas se **enmascararon** (`********`) en los Anexos A y D | Antes de entregar o instalar en producción: cambiar las contraseñas de las cuentas de ejemplo desde "Mi perfil" o restablecerlas |
| E2 | `sql/02_tablas_sigmud.sql` crea el usuario de la base con la contraseña marcador `CAMBIAR_AQUI`. Si se ejecuta sin cambiarla, el usuario queda con esa contraseña | Cambiarla antes de ejecutar (Anexo B, paso 3.2) |
| E3 | Las filas creadas por `pruebas-seguridad.ps1` en la base real (usuarios `segpru…` y `segaud…`) no se pueden borrar de la auditoría (inmutable) | Ejecutar el script contra una base de pruebas, no la real |
| E4 | La contraseña real de la base está en `src/main/resources/db.properties` (enmascarada en el Anexo D) | No subir ese archivo a repositorios públicos |

## F. Entorno

| # | Observación |
|---|---|
| F1 | `nb-configuration.xml` declara `j2eeVersion = 11-web` (Jakarta EE 11) mientras el servidor es Tomcat 10.1 (Jakarta EE 10, Servlet 6.0). Puede causar que NetBeans no ofrezca el servidor (Anexo B, problemas frecuentes). No se verificó dentro del IDE |
| F2 | `desplegar.ps1` tiene rutas fijas del equipo del desarrollador (JDK 25, Maven de NetBeans, Tomcat en `C:\Users\ASUS\Documents\toncat\...`). En otro equipo hay que editar la sección `RUTAS` |
| F3 | El equipo tiene instalados varios JDK (17, 21, 24 y 25); el proyecto se compila con el 25 a nivel Java 17. Un `JAVA_HOME` apuntando a otro JDK es una fuente de errores |
| F4 | Las citas de ejemplo del script `02` están fechadas el 15 y 16 de noviembre de 2026, que ya pasaron: los paneles de atención no las mostrarán como "de hoy" ni "próximas" |

## G. Ideas de mejora para las Recomendaciones de la monografía
1. Desplegar con **HTTPS** y cookie `Secure`; limitar intentos por IP además de por usuario.
2. **Pool de conexiones** (`DataSource`) para soportar más usuarios simultáneos.
3. Pruebas de Servlets y filtros (mocks) y pruebas del JavaScript; pruebas de carga y de concurrencia en las salidas de stock.
4. Tabla de **unidades de medida** y de **ventanillas** en la base de datos, para que las reglas no dependan solo del código.
5. Recordatorios de cita por **correo o mensaje** al contribuyente; recuperación de contraseña por correo en lugar de contraseña temporal mostrada al administrador.
6. Reportes en **PDF/Excel** además de CSV; archivado o purga periódica de la tabla de auditoría.
7. Permisos más finos (por ejemplo, que ATENCION solo vea sus propios contribuyentes) y verificación en dos pasos para el ADMIN.
8. **Respaldo automático** de la base de datos y plan de recuperación.
9. Integrar los módulos hoy fuera de alcance (trámite documentario, tributos, mercado, registro civil, Defensa Civil) como fases futuras.
10. Control de versiones (Git) e integración continua que ejecute `mvn test` y el script de seguridad.

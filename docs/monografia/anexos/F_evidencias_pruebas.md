# Anexo F. Evidencias de pruebas

> **Regla de este anexo:** el "resultado obtenido" y el "estado" solo provienen de resultados **reales**: (1) la ejecución de `mvn test` y del script
> `pruebas-seguridad.ps1` hecha para este documento el **2026-11-19**, o (2) resultados que ya estaban escritos en `docs/avance.md`
> (marcados "DOCUMENTADO EN avance.md"). Todo lo demás figura como **PENDIENTE DE EJECUTAR**. No se inventó ningún resultado.

## 1. Plan de pruebas

| Tipo | Objetivo | Herramienta | Alcance real |
|---|---|---|---|
| **Unitarias** (validadores, seguridad, modelo) | Verificar reglas puras sin base de datos | JUnit 5 | `util` y `model`: Validador, ContribuyenteValidador, MaterialValidador, UsuarioValidador, CitaValidador, Seguridad, PoliticaPassword, Csv, Auditoria, OrdenSql, Pagina |
| **Unitarias de DAO** (integración con base de datos) | Verificar el SQL, las transacciones y las restricciones contra MySQL | JUnit 5 sobre `mun_quinua_test` (se niegan a correr contra otra base) | Contribuyentes, Movimientos, Citas, Usuarios, Dashboard, Auditoría, Ficha, Listados paginados. **No existen** pruebas de `MaterialDAOImpl` (altas/ediciones/bajas), `ReporteAlmacenDAOImpl`, `ReporteCitasDAOImpl` ni `AreaDAOImpl` |
| **Funcionales por requerimiento** | Verificar cada flujo desde la pantalla | Casos manuales de `docs/avance.md` | 138 casos escritos; 90 sin resultado todavía y 48 con resultado documentado |
| **De seguridad (en vivo)** | Accesos, roles, CSRF, cabeceras, XSS, inyección, contraseñas, auditoría, CSP | `pruebas-seguridad.ps1` contra Tomcat encendido | 207 comprobaciones HTTP |
| **De usabilidad** | Tema oscuro, teclado, foco, avisos | Revisión manual en navegador (descrita en `docs/avance.md`) | Sin casos con resultado numerado: **PENDIENTE DE EJECUTAR** como prueba formal |
| **De rendimiento y concurrencia** | Carga y usuarios simultáneos | — | **No existen** (limitación reconocida en `docs/auditoria.md`) |

## 2. Entorno de la ejecución real (2026-11-19)
| Elemento | Dato |
|---|---|
| Código probado | Copia del proyecto (sin `target`) tomada el 2026-11-19 y compilada con Maven 3.9.9 y JDK 25.0.1 (nivel Java 17) |
| Base de datos | Instancia **temporal** de MySQL Server 8.4.9 (puerto 13306, carpeta de datos temporal fuera del proyecto) creada con `sql/02_tablas_sigmud.sql` y `sql/03_bd_pruebas.sql`. **No se usó la base real del usuario** |
| Servidor | Copia aislada de Apache Tomcat 10.1.61 en el puerto 18080 con el WAR de esa copia |
| Archivos modificados | Solo en la **copia** (`db.properties` apuntando a la base temporal); el proyecto original no se tocó |
| Comandos | `mvn -B -q test` → código de salida 0; `.\pruebas-seguridad.ps1 -UrlBase http://localhost:18080/sigmud` → código de salida 0 |

## 3. Resultados de las pruebas JUnit (ejecución real)

**Total: 376 pruebas, 0 fallos o errores, 0 omitidas → APROBADO.**

| ID | Clase de prueba | Requerimientos | Qué verifica | Pruebas | Fallos | Errores | Omitidas | Tiempo | Estado |
|---|---|---|---|---:|---:|---:|---:|---:|---|
| U-01 | `dao.AuditoriaDAOTest` | RF-31, RF-32, RNF-21, RNF-23 | Registro, filtros, paginación, inyección SQL como texto, triggers inmutables, transacción con rollback/commit | 18 | 0 | 0 | 0 | 3.974 s | APROBADO |
| U-02 | `dao.CitaDAOTest` | RF-21 a RF-24, RNF-22 | Cruce de horarios, UNIQUE de la base, cancelar libera el horario, reprogramar, estados | 16 | 0 | 0 | 0 | 1.001 s | APROBADO |
| U-03 | `dao.ContribuyenteDAOTest` | RF-07 a RF-11 | Insertar, DNI duplicado, baja lógica, reactivar, actualizar, búsqueda con comodines | 9 | 0 | 0 | 0 | 0.313 s | APROBADO |
| U-04 | `dao.DashboardDAOTest` | RF-26 a RF-28 | Indicadores y listas de los tres paneles contra consultas directas | 15 | 0 | 0 | 0 | 1.913 s | APROBADO |
| U-05 | `dao.FichaContribuyenteDAOTest` | RF-10, RF-12 | Resumen y próxima cita, historial con filtro, actividad por entidad, inactivos y reactivar | 15 | 0 | 0 | 0 | 2.352 s | APROBADO |
| U-06 | `dao.ListadosPaginadosDAOTest` | RF-11, RF-16, RF-19, RNF-01, RNF-14 | Paginación, orden con lista blanca e inyección, materiales, usuarios, historiales | 10 | 0 | 0 | 0 | 6.128 s | APROBADO |
| U-07 | `dao.MovimientoDAOTest` | RF-17 a RF-19, RNF-21 | Entrada, salida, stock insuficiente, ROLLBACK forzado, auditoría en la transacción | 14 | 0 | 0 | 0 | 1.759 s | APROBADO |
| U-08 | `dao.PanelDatosTest` | RF-26 a RF-28 | Contenido de cada panel y aislamiento entre roles | 7 | 0 | 0 | 0 | 0.750 s | APROBADO |
| U-09 | `dao.UsuarioDAOTest` | RF-01, RF-02, RF-29, RF-30 | Login/intentos/bloqueo, alta, edición, estado, último ADMIN, desbloqueo, migración de hash | 29 | 0 | 0 | 0 | 3.288 s | APROBADO |
| U-10 | `model.PaginaTest` | RNF-14 | Cálculo de páginas, límites y normalización | 8 | 0 | 0 | 0 | 0.008 s | APROBADO |
| U-11 | `util.AuditoriaTest` | RF-31 | Saneamiento de texto (saltos de línea), longitudes y diferencias campo por campo | 17 | 0 | 0 | 0 | 0.030 s | APROBADO |
| U-12 | `util.CitaValidadorTest` | RF-21 | Fecha, hora, 90 días, funcionario/área, ventanilla, motivo | 44 | 0 | 0 | 0 | 0.246 s | APROBADO |
| U-13 | `util.ContribuyenteValidadorTest` | RF-07 | DNI, RUC, nombre, contacto, fecha de nacimiento | 20 | 0 | 0 | 0 | 0.030 s | APROBADO |
| U-14 | `util.CsvTest` | RF-20, RF-25, RF-32 | Escape de CSV (punto y coma, comillas, fórmulas) | 6 | 0 | 0 | 0 | 0.016 s | APROBADO |
| U-15 | `util.MaterialValidadorTest` | RF-13, RF-14 | Código, nombre, unidad y stock mínimo | 28 | 0 | 0 | 0 | 0.084 s | APROBADO |
| U-16 | `util.OrdenSqlTest` | RNF-01 | Lista blanca de orden: 18 entradas no permitidas (inyecciones y variantes) usan el orden por defecto | 8 | 0 | 0 | 0 | 0.016 s | APROBADO |
| U-17 | `util.PoliticaPasswordTest` | RF-06 | Política de contraseñas | 17 | 0 | 0 | 0 | 0.040 s | APROBADO |
| U-18 | `util.SeguridadTest` | RNF-02, RF-30 | PBKDF2, hash antiguo, token, contraseña temporal | 30 | 0 | 0 | 0 | 1.321 s | APROBADO |
| U-19 | `util.UsuarioValidadorTest` | RF-29 | Validación de usuarios y roles | 8 | 0 | 0 | 0 | 0.021 s | APROBADO |
| U-20 | `util.ValidadorTest` | RF-01, RF-07 | Usuario, DNI, RUC, teléfono, correo | 57 | 0 | 0 | 0 | 0.128 s | APROBADO |

(Los tiempos son los de la ejecución; pueden variar entre equipos.)

## 4. Resumen del script de seguridad (ejecución real)
**207 comprobaciones: 207 APROBADAS, 0 FALLIDAS.** Agrupación de los casos (ver el detalle en la sección 5):

| Grupo | Casos | Requerimientos |
|---|---|---|
| Sin sesión | S-001 a S-004 | RF-04 |
| Inyección SQL en el login y mensajes del login | S-005 a S-008 | RF-01, RNF-01 |
| Cabeceras de seguridad | S-009 a S-012 | RNF-04 |
| Sesión, cookie y CSRF | S-013 a S-020 | RF-01, RNF-03, RNF-05 |
| XSS y páginas de error | S-021 a S-025 | RNF-06, RNF-07 |
| Roles (almacén, atención) | S-026 a S-032 | RF-04 |
| Paneles por rol | S-033 a S-045 | RF-26 a RF-28 |
| Usuarios y contraseñas temporales | S-046 a S-086 | RF-05, RF-06, RF-29, RF-30 |
| Auditoría | S-087 a S-104 | RF-31, RF-32 |
| Ficha, reactivar y citas desde la ficha | S-105 a S-139 | RF-09, RF-10, RF-12, RNF-08 |
| Paginación y orden con valores hostiles | S-140 a S-182 | RF-11, RF-16, RNF-01, RNF-14 |
| Avisos, tema, accesibilidad, favicon | S-183 a S-192 | RF-33, RNF-10, RNF-11, RNF-19 |
| CSP y revisión del código de las vistas | S-193 a S-205 | RNF-04, RNF-19 |
| Cierre de sesión | S-206, S-207 | RF-03 |

## 5. Casos de prueba del script de seguridad (resultado real)

Columnas: ID (numeración en el orden en que el script los ejecuta), prueba, **resultado esperado** y **resultado obtenido** tal como los imprimió el
script, y estado. El script abrevia los textos largos a 90 caracteres. Observaciones: los casos "GET /contribuyentes (o /almacen, /usuarios) con parámetro orden hostil" son
los 15 casos (5 por listado) cuya etiqueta salió vacía por un defecto cosmético del script (ver `observaciones.md`); la petición hostil sí se envió.

| ID | Prueba | Esperado | Obtenido | Estado |
|---|---|---|---|---|
| S-001 | Sin sesion: GET /menu | 302 hacia /login | 302 -> /sigmud/login | APROBADO |
| S-002 | Sin sesion: GET /contribuyentes | 302 hacia /login | 302 -> /sigmud/login | APROBADO |
| S-003 | Sin sesion: GET /almacen | 302 hacia /login | 302 -> /sigmud/login | APROBADO |
| S-004 | Sin sesion: GET /citas | 302 hacia /login | 302 -> /sigmud/login | APROBADO |
| S-005 | Login con ' OR '1'='1 como usuario | Falla (401), sin sesion ni redireccion a /menu | 401 location='' | APROBADO |
| S-006 | Login con admin' -- como usuario | Falla (401) | 401 | APROBADO |
| S-007 | Login con contrasena incorrecta | 401 y mensaje generico | 401 | APROBADO |
| S-008 | Usuario inexistente da el MISMO mensaje | 401 y el mismo mensaje (no revela si el usuario existe) | 401 | APROBADO |
| S-009 | Cabecera X-Content-Type-Options | nosniff | nosniff | APROBADO |
| S-010 | Cabecera X-Frame-Options | DENY | DENY | APROBADO |
| S-011 | Cabecera Referrer-Policy | same-origin | same-origin | APROBADO |
| S-012 | Cabecera Content-Security-Policy | script-src 'self' y sin 'unsafe-inline' en scripts img-src 'self' | default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; da... | APROBADO |
| S-013 | Login correcto (admin) | 302 hacia /menu | 302 -> /sigmud/menu | APROBADO |
| S-014 | Cookie de sesion HttpOnly y SameSite=Strict | HttpOnly y SameSite=Strict SameSite=Strict | JSESSIONID=<valor>; Path=/sigmud; HttpOnly; | APROBADO |
| S-015 | El formulario trae el token CSRF | campo oculto csrf de 43 caracteres | fnr5uWyceqJmcDbqaG8iOjGsPDb8uv6Og1pQb6z9N_c | APROBADO |
| S-016 | POST sin token CSRF (sesion valida) | 403 | 403 | APROBADO |
| S-017 | POST con token CSRF falso | 403 | 403 | APROBADO |
| S-018 | POST con token CSRF correcto | 302 (no 403) | 302 | APROBADO |
| S-019 | Ajax POST sin cabecera X-CSRF-Token | 403 | 403 | APROBADO |
| S-020 | Ajax POST con cabecera X-CSRF-Token | 200 JSON ok:false (material inexistente, no 403) | 200 {"ok":false,"mensaje":"El material no existe o está dado de baja."} | APROBADO |
| S-021 | Crear contribuyente con nombre <script>alert(1)</script> | 302 (guardado) | 302 /sigmud/contribuyentes | APROBADO |
| S-022 | XSS: el nombre aparece ESCAPADO en el listado | &lt;script&gt; y nunca <script>alert(1)</script> | escapado=True crudo=False | APROBADO |
| S-023 | XSS reflejado: el texto buscado se escapa | sin <script>alert(2)</script> en la pagina | crudo=False | APROBADO |
| S-024 | Error 404 sin datos tecnicos | pagina propia, sin 'Apache Tomcat' ni 'Exception' | 404 | APROBADO |
| S-025 | Error 405 (metodo no permitido) sin datos tecnicos | pagina propia, sin version del servidor | 405 | APROBADO |
| S-026 | Login almacen1 | 302 hacia /menu | 302 | APROBADO |
| S-027 | almacen1 entra a /contribuyentes | 403 | 403 | APROBADO |
| S-028 | almacen1 entra a /citas | 403 | 403 | APROBADO |
| S-029 | almacen1 entra a /almacen (su modulo) | 200 | 200 | APROBADO |
| S-030 | Login atencion1 | 302 hacia /menu | 302 | APROBADO |
| S-031 | atencion1 entra a /almacen | 403 | 403 | APROBADO |
| S-032 | atencion1 entra a /citas (su modulo) | 200 | 200 | APROBADO |
| S-033 | Sin sesion: GET /dashboard/datos | 401 (JSON, no redireccion) | 401 {"ok":false,"mensaje":"La sesión expiró. Vuelva a iniciar sesión."} | APROBADO |
| S-034 | Sin sesion: POST /dashboard/desbloquear | 401 | 401 | APROBADO |
| S-035 | admin: /menu muestra el panel ADMIN | data-panel="ADMIN" | 200 | APROBADO |
| S-036 | admin: /dashboard/datos trae los datos de ADMIN | rol ADMIN con "usuarios" y "bloqueadas" | 200 | APROBADO |
| S-037 | atencion1: /menu muestra el panel ATENCION | data-panel="ATENCION" y sin panel ADMIN | 200 | APROBADO |
| S-038 | atencion1 NO recibe datos de ADMIN | rol ATENCION y ninguna clave de ADMIN/ALMACEN | 200 rol ATENCION=True | APROBADO |
| S-039 | atencion1 cambiando parametros (rol=ADMIN, id=1, funcionario=3) recibe lo MISMO | misma respuesta; los parametros se ignoran | rol=ATENCION kpis iguales=True | APROBADO |
| S-040 | atencion1 (con token CSRF valido) usa /dashboard/desbloquear | 403 (solo ADMIN) | 403 | APROBADO |
| S-041 | almacen1: /menu muestra el panel ALMACEN | data-panel="ALMACEN" | 200 | APROBADO |
| S-042 | almacen1 NO recibe datos de ADMIN ni de ATENCION | rol ALMACEN, con "entradasMes" y sin "bloqueadas"/"citas | Hoy" 200 | APROBADO |
| S-043 | almacen1 (con token CSRF valido) usa /dashboard/desbloquear | 403 (solo ADMIN) | 403 | APROBADO |
| S-044 | admin: /dashboard/desbloquear SIN token CSRF | 403 | 403 | APROBADO |
| S-045 | admin: /dashboard/desbloquear con token (usuario inexistente) | 200 JSON ok:false (no 403) | 200 {"ok":false,"mensaje":"El usuario no existe."} | APROBADO |
| S-046 | atencion1 entra a /usuarios | 403 (solo ADMIN) | 403 | APROBADO |
| S-047 | atencion1 intenta crear un usuario ADMIN (con token valido) | 403 | 403 | APROBADO |
| S-048 | atencion1 intenta restablecer una clave (con token valido) | 403 | 403 | APROBADO |
| S-049 | almacen1 entra a /usuarios | 403 (solo ADMIN) | 403 | APROBADO |
| S-050 | almacen1 intenta crear un usuario ADMIN (con token valido) | 403 | 403 | APROBADO |
| S-051 | almacen1 intenta restablecer una clave (con token valido) | 403 | 403 | APROBADO |
| S-052 | admin entra a /usuarios | 200 | 200 | APROBADO |
| S-053 | El listado trae el id del admin | numero | 1 | APROBADO |
| S-054 | POST /usuarios sin token CSRF | 403 | 403 | APROBADO |
| S-055 | Crear usuario de prueba (segpru890) | 302 (creado) | 302 | APROBADO |
| S-056 | La contrasena temporal se muestra (12 caracteres, cumple la politica) | 12 caracteres con mayuscula, minuscula y numero | longitud 12 | APROBADO |
| S-057 | La contrasena temporal se muestra UNA sola vez | no aparece al recargar | aparece=False | APROBADO |
| S-058 | Crear un usuario con nombre repetido | 200 con el error (no se crea) | 200 | APROBADO |
| S-059 | Crear un usuario ATENCION sin area | 200 con el error (no se crea) | 200 | APROBADO |
| S-060 | Un ADMIN intenta desactivarse a si mismo | rechazado: 'No puede desactivar su propia cuenta' | 302 mensaje=True | APROBADO |
| S-061 | Un ADMIN intenta cambiarse su propio rol | 200 con el error 'No puede cambiar su propio rol' | 200 | APROBADO |
| S-062 | El ADMIN sigue siendo ADMIN y activo | sigue entrando a /usuarios | 200 | APROBADO |
| S-063 | Restablecer clave sin token CSRF | 403 | 403 | APROBADO |
| S-064 | Restablecer clave (admin, con token) | 200 JSON con la temporal nueva (12 caracteres) | 200 longitud 12 | APROBADO |
| S-065 | El admin no puede restablecer su propia clave desde aqui | 200 JSON ok:false | 200 | APROBADO |
| S-066 | La contrasena temporal anterior ya no sirve | 401 | 401 | APROBADO |
| S-067 | Login del usuario nuevo con su contrasena temporal | 302 hacia /menu | 302 | APROBADO |
| S-068 | Clave temporal: GET /menu | 302 hacia /perfil/password | 302 -> /sigmud/perfil/password | APROBADO |
| S-069 | Clave temporal: GET /almacen | 302 hacia /perfil/password | 302 -> /sigmud/perfil/password | APROBADO |
| S-070 | Clave temporal: GET /perfil | 302 hacia /perfil/password | 302 -> /sigmud/perfil/password | APROBADO |
| S-071 | Clave temporal: GET /dashboard/datos | 302 hacia /perfil/password | 302 -> /sigmud/perfil/password | APROBADO |
| S-072 | Clave temporal: /assets sigue accesible | 200 | 200 | APROBADO |
| S-073 | Clave temporal: /perfil/password accesible con el mensaje explicativo | 200 con aviso de contrasena temporal | 200 | APROBADO |
| S-074 | Contrasena debil rechazada | 200 con los errores de la politica | 200 | APROBADO |
| S-075 | Contrasena igual al nombre de usuario rechazada | 200 con el error | 200 | APROBADO |
| S-076 | Contrasena actual incorrecta rechazada | 200 con el error (cuenta como intento fallido) | 200 | APROBADO |
| S-077 | Confirmacion distinta rechazada | 200 con el error | 200 | APROBADO |
| S-078 | Nueva igual a la actual rechazada | 200 con el error | 200 | APROBADO |
| S-079 | Cambio de clave sin token CSRF | 403 | 403 | APROBADO |
| S-080 | Cambio de clave correcto | 302 hacia /perfil | 302 -> /sigmud/perfil | APROBADO |
| S-081 | El cambio de clave cambia el id de sesion | Set-Cookie con un JSESSIONID nuevo | cookies nuevas: 1 | APROBADO |
| S-082 | Tras cambiar la clave el usuario ya usa el sistema | 200 (panel) | 200 | APROBADO |
| S-083 | Mi perfil muestra los datos del usuario | 200 con usuario, rol e inicio de sesion | 200 | APROBADO |
| S-084 | La contrasena temporal ya no sirve despues del cambio | 401 | 401 | APROBADO |
| S-085 | Entra con la contrasena nueva (sin cambio obligatorio) | 302 hacia /menu y /menu = 200 | 302 | APROBADO |
| S-086 | Un usuario desactivado no puede entrar | 401 (cuenta bloqueada o inactiva) | 401 | APROBADO |
| S-087 | atencion1 entra a /auditoria | 403 (solo ADMIN) | 403 | APROBADO |
| S-088 | atencion1 intenta exportar la auditoria en CSV | 403 | 403 | APROBADO |
| S-089 | almacen1 entra a /auditoria | 403 (solo ADMIN) | 403 | APROBADO |
| S-090 | almacen1 intenta exportar la auditoria en CSV | 403 | 403 | APROBADO |
| S-091 | Sin sesion: GET /auditoria | 302 hacia /login | 302 -> /sigmud/login | APROBADO |
| S-092 | admin entra a /auditoria | 200 | 200 | APROBADO |
| S-093 | admin exporta la auditoria en CSV | 200 text/csv | 200 | APROBADO |
| S-094 | La auditoria es de solo lectura: POST /auditoria (con token) | 405 (no hay nada que modificar) | 405 | APROBADO |
| S-095 | Periodo de mas de 366 dias rechazado | 200 con el error (sin consultar) | 200 | APROBADO |
| S-096 | Inyeccion SQL en los filtros de auditoria | 200 sin error y sin resultados | 200 | APROBADO |
| S-097 | Login fallido con un usuario nuevo (segaud195) | 401 | 401 | APROBADO |
| S-098 | El login fallido aparece en /auditoria | una fila LOGIN_FALLIDO de segaud195 | estado 200 | APROBADO |
| S-099 | El registro no contiene la contrasena escrita | sin 'ClaveIncorrecta999' en la pagina | aparece=False | APROBADO |
| S-100 | Login fallido con saltos de linea y <script> en el usuario | 401 | 401 | APROBADO |
| S-101 | XSS en auditoria: el usuario con <script> aparece ESCAPADO | &lt;script&gt; y nunca <script>alert(7) | escapado=True crudo=False | APROBADO |
| S-102 | Los saltos de linea se eliminan al guardar | una sola linea: 'segaud521 FALSO' dentro del mismo atributo | revisado en el HTML | APROBADO |
| S-103 | Panel ADMIN: indicador de accesos fallidos y actividad reciente | claves "loginFallidos" y "actividad" | 200 | APROBADO |
| S-104 | atencion1 NO recibe la actividad ni los accesos fallidos | sin "loginFallidos" ni "actividad" | 200 | APROBADO |
| S-105 | El listado enlaza el nombre a la ficha | enlace accion=ficha&id=N | id=5 | APROBADO |
| S-106 | Sin sesion: GET ficha | 302 hacia /login | 302 -> /sigmud/login | APROBADO |
| S-107 | almacen1 abre la ficha | 403 | 403 | APROBADO |
| S-108 | almacen1 intenta reactivar (con token valido) | 403 | 403 | APROBADO |
| S-109 | admin abre la ficha | 200 con Datos personales, indicadores e historial | 200 | APROBADO |
| S-110 | XSS: el nombre con <script> se ve ESCAPADO en la ficha | &lt;script&gt; y nunca <script>alert(1) | escapado=True crudo=False | APROBADO |
| S-111 | admin ve la pestana Actividad | panel-actividad presente | True | APROBADO |
| S-112 | atencion1 abre la ficha | 200 | 200 | APROBADO |
| S-113 | atencion1 NO ve la pestana Actividad | sin panel-actividad ni tab-actividad | panel=False | APROBADO |
| S-114 | atencion1 NO puede pedir la Actividad con parametros | 200 sin panel-actividad ni eventos de auditoria | panel=False | APROBADO |
| S-115 | Filtro de estado con texto invalido | 200 (se ignora, sin error 500) | 200 | APROBADO |
| S-116 | Ficha con id 'abc' | 302 al listado con mensaje (no 500, sin datos) | 302 -> /sigmud/contribuyentes | APROBADO |
| S-117 | Ficha con id '1 OR 1=1' | 302 al listado con mensaje (no 500, sin datos) | 302 -> /sigmud/contribuyentes | APROBADO |
| S-118 | Ficha con id '999999' | 302 al listado con mensaje (no 500, sin datos) | 302 -> /sigmud/contribuyentes | APROBADO |
| S-119 | Ficha con id '-5' | 302 al listado con mensaje (no 500, sin datos) | 302 -> /sigmud/contribuyentes | APROBADO |
| S-120 | Ficha con id '' | 302 al listado con mensaje (no 500, sin datos) | 302 -> /sigmud/contribuyentes | APROBADO |
| S-121 | Ficha con id '0' | 302 al listado con mensaje (no 500, sin datos) | 302 -> /sigmud/contribuyentes | APROBADO |
| S-122 | Ficha con id '99999999999999999999' | 302 al listado con mensaje (no 500, sin datos) | 302 -> /sigmud/contribuyentes | APROBADO |
| S-123 | Ficha sin id | 302 al listado | 302 | APROBADO |
| S-124 | CONSULTA_FICHA aparece en /auditoria | al menos un evento CONSULTA_FICHA | 200 | APROBADO |
| S-125 | Baja desde la ficha sin token CSRF | 403 | 403 | APROBADO |
| S-126 | Reactivar sin token CSRF | 403 | 403 | APROBADO |
| S-127 | Baja desde la ficha (con token) | 302 de vuelta a la ficha | 302 -> /sigmud/contribuyentes?accion=ficha&id=5 | APROBADO |
| S-128 | La ficha de un dado de baja se puede abrir | 200 con insignia 'Dado de baja', Editar y Nueva cita deshabilitad | os 200 | APROBADO |
| S-129 | El listado normal NO muestra al dado de baja | sin resultados | True | APROBADO |
| S-130 | Con 'Ver tambien dados de baja' aparece con su insignia | fila con 'Dado de baja' y boton Reactivar | 200 | APROBADO |
| S-131 | Nueva cita para un contribuyente dado de baja | 200 y el contribuyente NO viene preseleccionado | 200 | APROBADO |
| S-132 | Reactivar (con token) | 302 de vuelta a la ficha | 302 -> /sigmud/contribuyentes?accion=ficha&id=5 | APROBADO |
| S-133 | Tras reactivar la ficha ya no dice 'Dado de baja' | sin la insignia y con mensaje de exito | 200 | APROBADO |
| S-134 | REACTIVAR aparece en /auditoria | evento REACTIVAR | 200 | APROBADO |
| S-135 | Nueva cita desde la ficha: contribuyente ya elegido y bloqueado | readonly con el DNI y el aviso 'elegido desde su ficha' | 200 | APROBADO |
| S-136 | Nueva cita con contribuyente='abc' | 200, se ignora y se muestra el formulario normal | 200 | APROBADO |
| S-137 | Nueva cita con contribuyente='999999' | 200, se ignora y se muestra el formulario normal | 200 | APROBADO |
| S-138 | Nueva cita con contribuyente='1 OR 1=1' | 200, se ignora y se muestra el formulario normal | 200 | APROBADO |
| S-139 | impresion.css se sirve y es solo @media print | 200 con @media print | 200 | APROBADO |
| S-140 | GET /contribuyentes con parámetro orden hostil (etiqueta vacía en el script) | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-141 | GET /contribuyentes con parámetro orden hostil (etiqueta vacía en el script) | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-142 | GET /contribuyentes con parámetro orden hostil (etiqueta vacía en el script) | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-143 | GET /contribuyentes con parámetro orden hostil (etiqueta vacía en el script) | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-144 | GET /contribuyentes con parámetro orden hostil (etiqueta vacía en el script) | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-145 | GET /contribuyentes con pagina=-5 | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-146 | GET /contribuyentes con pagina=999999 | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-147 | GET /contribuyentes con pagina=abc | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-148 | GET /contribuyentes con pagina enorme | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-149 | GET /contribuyentes con tamano=100000 | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-150 | GET /contribuyentes con tamano=-1 | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-151 | GET /contribuyentes con tamano=7 | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-152 | GET /almacen con parámetro orden hostil (etiqueta vacía en el script) | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-153 | GET /almacen con parámetro orden hostil (etiqueta vacía en el script) | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-154 | GET /almacen con parámetro orden hostil (etiqueta vacía en el script) | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-155 | GET /almacen con parámetro orden hostil (etiqueta vacía en el script) | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-156 | GET /almacen con parámetro orden hostil (etiqueta vacía en el script) | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-157 | GET /almacen con pagina=-5 | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-158 | GET /almacen con pagina=999999 | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-159 | GET /almacen con pagina=abc | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-160 | GET /almacen con pagina enorme | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-161 | GET /almacen con tamano=100000 | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-162 | GET /almacen con tamano=-1 | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-163 | GET /almacen con tamano=7 | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-164 | GET /usuarios con parámetro orden hostil (etiqueta vacía en el script) | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-165 | GET /usuarios con parámetro orden hostil (etiqueta vacía en el script) | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-166 | GET /usuarios con parámetro orden hostil (etiqueta vacía en el script) | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-167 | GET /usuarios con parámetro orden hostil (etiqueta vacía en el script) | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-168 | GET /usuarios con parámetro orden hostil (etiqueta vacía en el script) | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-169 | GET /usuarios con pagina=-5 | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-170 | GET /usuarios con pagina=999999 | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-171 | GET /usuarios con pagina=abc | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-172 | GET /usuarios con pagina enorme | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-173 | GET /usuarios con tamano=100000 | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-174 | GET /usuarios con tamano=-1 | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-175 | GET /usuarios con tamano=7 | 200 con el listado normal (valores por defecto), sin error 500 | 200 | APROBADO |
| S-176 | tamano=100000 se normaliza a 10 | selector con 10 marcado | True | APROBADO |
| S-177 | tamano=50 es valido | selector con 50 marcado | True | APROBADO |
| S-178 | Orden por columna valida (nombre, descendente) | aria-sort=descending en el encabezado | True | APROBADO |
| S-179 | Orden invalido: el encabezado marca el orden por defecto | aria-sort=ascending en el nombre | True | APROBADO |
| S-180 | La tabla de contribuyentes sigue existiendo y busca | encuentra el contribuyente de prueba | True | APROBADO |
| S-181 | Historial de movimientos con parametros hostiles | 200 o vuelta al listado, nunca 500 | 200 | APROBADO |
| S-182 | Ficha con parametros hostiles de orden y pagina | 200 con el historial de citas | 200 | APROBADO |
| S-183 | El mensaje flash llega como toast (data-*) | id="datosFlash" con data-tipo="error" y data-mensaje | True | APROBADO |
| S-184 | El contenedor de toasts es accesible (aria-live) | contenedorToasts con aria-live | True | APROBADO |
| S-185 | Login: favicon, theme-color y boton de tema | link rel=icon, meta theme-color y btn-tema | 200 | APROBADO |
| S-186 | Login: enlace Saltar al contenido y main con id | saltar-contenido y id=contenido | ok | APROBADO |
| S-187 | Login sin logo: icono generico (no se inventa un escudo) | icono bi-building o logo.png | ok | APROBADO |
| S-188 | Menu: favicon, tema, saltar, toasts | icon, tema.js, btn-tema, saltar-contenido, contenedorToasts | 200 | APROBADO |
| S-189 | Recurso estatico /assets/img/favicon.svg | 200 (publico) | 200 | APROBADO |
| S-190 | Recurso estatico /assets/js/tema.js | 200 (publico) | 200 | APROBADO |
| S-191 | Recurso estatico /assets/js/toast.js | 200 (publico) | 200 | APROBADO |
| S-192 | Recurso estatico /assets/js/paginacion.js | 200 (publico) | 200 | APROBADO |
| S-193 | CSP presente en /login | script-src 'self' sin unsafe-inline ni CDN | 171 caracteres | APROBADO |
| S-194 | CSP presente en /menu | script-src 'self' sin unsafe-inline ni CDN | 171 caracteres | APROBADO |
| S-195 | CSP presente en /contribuyentes | script-src 'self' sin unsafe-inline ni CDN | 171 caracteres | APROBADO |
| S-196 | CSP presente en /almacen | script-src 'self' sin unsafe-inline ni CDN | 171 caracteres | APROBADO |
| S-197 | CSP presente en /usuarios | script-src 'self' sin unsafe-inline ni CDN | 171 caracteres | APROBADO |
| S-198 | CSP presente en /auditoria | script-src 'self' sin unsafe-inline ni CDN | 171 caracteres | APROBADO |
| S-199 | CSP presente en /contribuyentes?accion=ficha&id=5 | script-src 'self' sin unsafe-inline ni CDN | 171 caracteres | APROBADO |
| S-200 | JSP/tags revisados: 32 archivos | se encontraron archivos para revisar | 32 | APROBADO |
| S-201 | Ningun JSP tiene <script> en linea | 0 coincidencias | 0 | APROBADO |
| S-202 | Ningun JSP tiene onclick/onerror/onchange/onload... | 0 coincidencias | 0 | APROBADO |
| S-203 | Ningun JSP usa javascript: en enlaces | 0 coincidencias | 0 | APROBADO |
| S-204 | Ningun JSP tiene <style> en linea | 0 coincidencias | 0 | APROBADO |
| S-205 | Ningun JSP enlaza a CDN ni fuentes externas | 0 coincidencias | 0 | APROBADO |
| S-206 | Despues de cerrar sesion, /menu pide login | 302 hacia /login | 302 -> /sigmud/login | APROBADO |
| S-207 | Despues de cerrar sesion, /auditoria pide login | 302 hacia /login | 302 -> /sigmud/login | APROBADO |

## 6. Casos manuales documentados en `docs/avance.md`

Extraídos tal cual de las tablas de casos de `docs/avance.md`. **90 casos tienen el resultado "(pendiente)"** en ese documento: aquí figuran como
**PENDIENTE DE EJECUTAR** (no se ejecutaron manualmente para este anexo). Los 48 casos con resultado ya estaban escritos en `docs/avance.md`
(ejecuciones de la época de cada módulo, algunas por consulta directa al DAO); se copian como "DOCUMENTADO", **no se repitieron hoy**.

| ID | Sección de avance.md | Entrada / prueba | Esperado | Obtenido (según avance.md) | Estado |
|---|---|---|---|---|---|
| M-001 | Módulo 2, parte 1: capa de datos y validación de Contribuyentes / Casos de valid | NATURAL, DNI `1234567` (7 dígitos) | Error de DNI | El DNI debe tener exactamente 8 dígitos. | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-002 | Módulo 2, parte 1: capa de datos y validación de Contribuyentes / Casos de valid | JURIDICA, RUC `30123456789` | Error de RUC | El RUC debe tener 11 dígitos y empezar con 10 o 20. | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-003 | Módulo 2, parte 1: capa de datos y validación de Contribuyentes / Casos de valid | Teléfono `96600000` (8 dígitos) | Error de teléfono | El teléfono debe tener 9 dígitos y empezar con 9. | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-004 | Módulo 2, parte 1: capa de datos y validación de Contribuyentes / Casos de valid | Email `ana.example.com` | Error de correo | El correo electrónico no es válido. | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-005 | Módulo 2, parte 1: capa de datos y validación de Contribuyentes / Casos de valid | Fecha de nacimiento dentro de 1 año | Error de fecha futura | La fecha de nacimiento no puede ser futura. | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-006 | Módulo 2, parte 1: capa de datos y validación de Contribuyentes / Casos de valid | DNI `45678912`, cel. `966123456`, email `ana@example.com`, nacimiento 1990-05-20 | Sin errores | (sin errores) | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-007 | Módulo 2, parte 2: pantallas de Contribuyentes / Pruebas manuales | Nuevo, NATURAL, DNI `45678912`, "Ana Torres Rojas", cel. `966123456` | Vuelve al listado con "Contribuyente registrado correctamente." (una sola vez) | — | PENDIENTE DE EJECUTAR |
| M-008 | Módulo 2, parte 2: pantallas de Contribuyentes / Pruebas manuales | Nuevo, JURIDICA, RUC `20123456781`, "Comercial Andina SAC" | Registro exitoso; el campo de fecha no aparece | — | PENDIENTE DE EJECUTAR |
| M-009 | Módulo 2, parte 2: pantallas de Contribuyentes / Pruebas manuales | Nuevo con DNI `70000001` (ya existe) | Formulario con "Ya existe un contribuyente con ese DNI/RUC" y datos conservados | — | PENDIENTE DE EJECUTAR |
| M-010 | Módulo 2, parte 2: pantallas de Contribuyentes / Pruebas manuales | Nuevo, NATURAL, DNI de 7 dígitos | Navegador: campo en rojo, no envía. Con JS desactivado: el servidor devuelve el error del DNI | — | PENDIENTE DE EJECUTAR |
| M-011 | Módulo 2, parte 2: pantallas de Contribuyentes / Pruebas manuales | Editar un contribuyente: cambiar el teléfono | Tipo y DNI de solo lectura; guarda y muestra "actualizado correctamente" | — | PENDIENTE DE EJECUTAR |
| M-012 | Módulo 2, parte 2: pantallas de Contribuyentes / Pruebas manuales | Buscar `Quispe` | Solo "Maria Quispe Huaman" | — | PENDIENTE DE EJECUTAR |
| M-013 | Módulo 2, parte 2: pantallas de Contribuyentes / Pruebas manuales | Buscar `70000002` | Solo "Juan Ccasani Flores" | — | PENDIENTE DE EJECUTAR |
| M-014 | Módulo 2, parte 2: pantallas de Contribuyentes / Pruebas manuales | Dar de baja (confirmar en el modal) | Desaparece del listado; en BD `estado = 0` y la fila sigue existiendo | — | PENDIENTE DE EJECUTAR |
| M-015 | Módulo 2, parte 2: pantallas de Contribuyentes / Pruebas manuales | Entrar con `almacen1` y abrir `/sigmud/contribuyentes` | 403 "Acceso denegado"; la tarjeta no aparece en su menú | — | PENDIENTE DE EJECUTAR |
| M-016 | Módulo 2, parte 2: pantallas de Contribuyentes / Pruebas manuales | Sin sesión, abrir `/sigmud/contribuyentes` | Redirige a `/login` | — | PENDIENTE DE EJECUTAR |
| M-017 | Módulo 3, parte 1: CRUD de materiales del almacén / Pruebas manuales | Nuevo: `MAT-006`, "Clavos 2 pulgadas", CAJA, stock mínimo 5 | Listado con "Material registrado correctamente. Su stock inicial es 0."; fila con stock 0 y "Crítico" | — | PENDIENTE DE EJECUTAR |
| M-018 | Módulo 3, parte 1: CRUD de materiales del almacén / Pruebas manuales | Nuevo con código `MAT-001` | Formulario con "Ya existe un material con ese código." y datos conservados | — | PENDIENTE DE EJECUTAR |
| M-019 | Módulo 3, parte 1: CRUD de materiales del almacén / Pruebas manuales | Nuevo con código `mat1` | Navegador: se escribe `MAT1`, campo en rojo y no envía. Con JS desactivado, el servidor devuelve el error del formato | — | PENDIENTE DE EJECUTAR |
| M-020 | Módulo 3, parte 1: CRUD de materiales del almacén / Pruebas manuales | Unidad `DOCENA` (solo posible manipulando el formulario con las herramientas del navegador) | El servidor rechaza: "Seleccione una unidad de la lista…" | — | PENDIENTE DE EJECUTAR |
| M-021 | Módulo 3, parte 1: CRUD de materiales del almacén / Pruebas manuales | Editar MAT-002: cambiar nombre y stock mínimo | Código de solo lectura, stock mostrado como texto (200); al guardar el stock sigue en 200 (verificar en la BD) | — | PENDIENTE DE EJECUTAR |
| M-022 | Módulo 3, parte 1: CRUD de materiales del almacén / Pruebas manuales | Buscar `papel` | Solo "Papel bond A4 (paquete de 500)" | — | PENDIENTE DE EJECUTAR |
| M-023 | Módulo 3, parte 1: CRUD de materiales del almacén / Pruebas manuales | "Ver solo stock crítico" (con MAT-005 en mínimo 5) | Solo los materiales con stock <= mínimo, con insignia roja "Crítico" | — | PENDIENTE DE EJECUTAR |
| M-024 | Módulo 3, parte 1: CRUD de materiales del almacén / Pruebas manuales | Dar de baja MAT-006 (confirmar en el modal) | Desaparece del listado; en BD `estado = 0` y la fila sigue existiendo | — | PENDIENTE DE EJECUTAR |
| M-025 | Módulo 3, parte 1: CRUD de materiales del almacén / Pruebas manuales | Entrar con `atencion1` y abrir `/sigmud/almacen` | 403 "Acceso denegado"; la tarjeta de Almacén no aparece en su menú | — | PENDIENTE DE EJECUTAR |
| M-026 | Módulo 3, parte 1: CRUD de materiales del almacén / Pruebas manuales | Sin sesión, abrir `/sigmud/almacen` | Redirige a `/login` | — | PENDIENTE DE EJECUTAR |
| M-027 | Módulo 3, parte 2: entradas y salidas de stock con transacción, Ajax y JSON / Pr | Entrada de 10 a MAT-002 | Aviso verde "Entrada registrada correctamente. Stock actual: 210."; la fila pasa de 200 a 210 sin recargar | — | PENDIENTE DE EJECUTAR |
| M-028 | Módulo 3, parte 2: entradas y salidas de stock con transacción, Ajax y JSON / Pr | Salida de 15 a MAT-002 con área "Mesa de Partes" | Aviso verde; stock 195 en la fila | — | PENDIENTE DE EJECUTAR |
| M-029 | Módulo 3, parte 2: entradas y salidas de stock con transacción, Ajax y JSON / Pr | Salida de 1000 a MAT-002 | Mensaje rojo dentro del modal (sin cerrarlo): "Stock insuficiente: solo hay 195 unidades disponibles."; stock y movimientos no cambian (verificar en Historial y con la consulta de consistencia) | — | PENDIENTE DE EJECUTAR |
| M-030 | Módulo 3, parte 2: entradas y salidas de stock con transacción, Ajax y JSON / Pr | Cantidad `0` (o vacía) | Navegador: campo en rojo, no envía. Negativo no se puede escribir (solo números). Con `$.post` a mano y `cantidad=-5`: JSON `ok:false` "La cantidad debe ser un número entero entre 1 y 100000." | — | PENDIENTE DE EJECUTAR |
| M-031 | Módulo 3, parte 2: entradas y salidas de stock con transacción, Ajax y JSON / Pr | Salida sin elegir área | Navegador: área en rojo, no envía. Con `$.post` sin `idArea`: `ok:false` "Seleccione el área solicitante." | — | PENDIENTE DE EJECUTAR |
| M-032 | Módulo 3, parte 2: entradas y salidas de stock con transacción, Ajax y JSON / Pr | `$.post('/sigmud/almacen/movimiento', {accion:'registrar', idMaterial:1, tipo:'AJUSTE', cantidad:5})` | `ok:false` "El tipo de movimiento no es válido."; sin cambios | — | PENDIENTE DE EJECUTAR |
| M-033 | Módulo 3, parte 2: entradas y salidas de stock con transacción, Ajax y JSON / Pr | Igual con `idMaterial:99999`, `tipo:'ENTRADA'`, `cantidad:5` | `ok:false` "El material no existe o está dado de baja." | — | PENDIENTE DE EJECUTAR |
| M-034 | Módulo 3, parte 2: entradas y salidas de stock con transacción, Ajax y JSON / Pr | Falla provocada en el segundo paso: `$.post(..., {accion:'registrar', idMaterial:2, tipo:'SALIDA', cantidad:5, idArea:999})` | HTTP 500 con `ok:false` y mensaje genérico; en el log de Tomcat aparece la violación de clave foránea. **Rollback:** el stock de MAT-002 y la cantidad de movimientos en el Historial quedan igual que antes | — | PENDIENTE DE EJECUTAR |
| M-035 | Módulo 3, parte 2: entradas y salidas de stock con transacción, Ajax y JSON / Pr | Entrar con `atencion1` y abrir `/sigmud/almacen/movimiento?accion=historial&id=1` | 403 "Acceso denegado" | — | PENDIENTE DE EJECUTAR |
| M-036 | Módulo 3, parte 2: entradas y salidas de stock con transacción, Ajax y JSON / Pr | Sin sesión, abrir `/sigmud/almacen/movimiento?accion=historial&id=1` | Redirige a `/login` | — | PENDIENTE DE EJECUTAR |
| M-037 | Módulo 3, parte 2: entradas y salidas de stock con transacción, Ajax y JSON / Pr | Abrir Historial de MAT-002 tras las pruebas 1 y 2 | Los movimientos más recientes primero, con área, usuario ("Administrador del Sistema") y badges verde/rojo | — | PENDIENTE DE EJECUTAR |
| M-038 | Módulo 3, parte 3: reportes del almacén / Pruebas manuales | Reporte "Stock crítico" con datos conocidos: editar un material para que su stock mínimo sea mayor que su stock (p. ej. MAT-005: stock 4, mínimo 10) | Aparece MAT-005 con faltante 6; el pie muestra el total de materiales y la suma de faltantes | — | PENDIENTE DE EJECUTAR |
| M-039 | Módulo 3, parte 3: reportes del almacén / Pruebas manuales | "Movimientos", periodo por defecto (últimos 30 días) | Todos los movimientos recientes, el más nuevo primero; el pie muestra TOTAL ENTRADAS, TOTAL SALIDAS y SALDO DEL PERIODO | — | PENDIENTE DE EJECUTAR |
| M-040 | Módulo 3, parte 3: reportes del almacén / Pruebas manuales | "Movimientos" con tipo = Salida | Solo movimientos SALIDA; TOTAL ENTRADAS = 0 | — | PENDIENTE DE EJECUTAR |
| M-041 | Módulo 3, parte 3: reportes del almacén / Pruebas manuales | "Movimientos" con área = Mesa de Partes | Solo las salidas hechas a esa área (las entradas no tienen área y no aparecen) | — | PENDIENTE DE EJECUTAR |
| M-042 | Módulo 3, parte 3: reportes del almacén / Pruebas manuales | "Resumen por material", periodo por defecto, material MAT-001 | Entradas, salidas y saldo = entradas − salidas; coinciden con lo registrado (p. ej. 40 + 50 entradas, 20 salidas, saldo 70 si el periodo incluye el stock inicial) y con el Historial | — | PENDIENTE DE EJECUTAR |
| M-043 | Módulo 3, parte 3: reportes del almacén / Pruebas manuales | "Salidas por área" | Una fila por área con unidades entregadas y N.º de salidas, de mayor a menor; "% del total" suma 100 %; en la fila TOTAL, "Verificación" = OK (con los datos actuales ya dio 39 unidades, 2 salidas, 100,0 % y OK fuera de Tomcat) | — | PENDIENTE DE EJECUTAR |
| M-044 | Módulo 3, parte 3: reportes del almacén / Pruebas manuales | Desde = 2026-11-20 y hasta = 2026-11-01 | Mensaje rojo "La fecha "desde" no puede ser mayor que la fecha "hasta"." (en el navegador antes de enviar; en el servidor si se salta el JS); no se consulta | — | PENDIENTE DE EJECUTAR |
| M-045 | Módulo 3, parte 3: reportes del almacén / Pruebas manuales | Desde = 2025-01-01 y hasta = 2026-11-14 | "El periodo no puede pasar de 366 días." | — | PENDIENTE DE EJECUTAR |
| M-046 | Módulo 3, parte 3: reportes del almacén / Pruebas manuales | "Exportar CSV" con un reporte con datos, abrir el archivo en Excel | Se descarga `reporte-almacen-<reporte>-<fecha>.csv`; Excel separa en columnas y muestra bien tildes y ñ (p. ej. "Área", "Logistica y Almacen"); al final están las filas de totales | — | PENDIENTE DE EJECUTAR |
| M-047 | Módulo 3, parte 3: reportes del almacén / Pruebas manuales | Entrar con `atencion1` y abrir `/sigmud/almacen/reportes` | 403 "Acceso denegado" | — | PENDIENTE DE EJECUTAR |
| M-048 | Módulo 3, parte 3: reportes del almacén / Pruebas manuales | Sin sesión, abrir `/sigmud/almacen/reportes` | Redirige a `/login` | — | PENDIENTE DE EJECUTAR |
| M-049 | Módulo 4, parte 1: capa de datos y reglas de negocio de las Citas / Casos de pru | Cita válida | ACEPTADA | ACEPTADA | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-050 | Módulo 4, parte 1: capa de datos y reglas de negocio de las Citas / Casos de pru | Segunda cita al MISMO funcionario, fecha y hora | CitaCruzadaException | CitaCruzadaException | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-051 | Módulo 4, parte 1: capa de datos y reglas de negocio de las Citas / Casos de pru | Misma hora con OTRO funcionario | ACEPTADA | ACEPTADA | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-052 | Módulo 4, parte 1: capa de datos y reglas de negocio de las Citas / Casos de pru | Cancelar la primera y volver a agendar esa hora | ACEPTADA | ACEPTADA | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-053 | Módulo 4, parte 1: capa de datos y reglas de negocio de las Citas / Casos de pru | Reprogramar una cita a una hora ocupada | CitaCruzadaException | CitaCruzadaException | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-054 | Módulo 4, parte 1: capa de datos y reglas de negocio de las Citas / Casos de pru | Reprogramar sin cambiar la hora (solo la ventanilla) | ACEPTADA | ACEPTADA | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-055 | Módulo 4, parte 1: capa de datos y reglas de negocio de las Citas / Casos de pru | Fecha pasada (ayer) | RECHAZADA | RECHAZADA: "La fecha no puede ser anterior a hoy." | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-056 | Módulo 4, parte 1: capa de datos y reglas de negocio de las Citas / Casos de pru | Sábado y domingo | RECHAZADA | RECHAZADA: "Solo se atiende de lunes a viernes." | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-057 | Módulo 4, parte 1: capa de datos y reglas de negocio de las Citas / Casos de pru | Hora 07:30 | RECHAZADA | RECHAZADA: "La hora debe estar entre 08:00 y 16:30." | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-058 | Módulo 4, parte 1: capa de datos y reglas de negocio de las Citas / Casos de pru | Hora 09:15 | RECHAZADA | RECHAZADA: "La hora debe ser en intervalos de 30 minutos…" | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-059 | Módulo 4, parte 1: capa de datos y reglas de negocio de las Citas / Casos de pru | Funcionario de otra área | RECHAZADA | RECHAZADA: "El funcionario no pertenece al área seleccionada." | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-060 | Módulo 4, parte 1: capa de datos y reglas de negocio de las Citas / Casos de pru | Contribuyente dado de baja | RECHAZADA | RECHAZADA: "El contribuyente está dado de baja." | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-061 | Módulo 4, parte 1: capa de datos y reglas de negocio de las Citas / Casos de pru | Pasar una cita CANCELADA a ATENDIDA | CitaEstadoException | CitaEstadoException: "Una cita CANCELADA ya no se puede modificar." | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-062 | Módulo 4, parte 2: pantallas de Citas, Ajax auxiliar y reporte / Pruebas manuale | Nueva cita: contribuyente "Quispe", área Mesa de Partes, funcionario, fecha de la próxima semana, turno 10:00, ventanilla 1, motivo | Vuelve al listado de esa fecha con "Cita registrada correctamente." (una sola vez) y la cita aparece con insignia azul PROGRAMADA | — | PENDIENTE DE EJECUTAR |
| M-063 | Módulo 4, parte 2: pantallas de Citas, Ajax auxiliar y reporte / Pruebas manuale | Otra cita al mismo funcionario, fecha y hora (en la cuadrícula el 10:00 está en gris; también se puede forzar con otra pestaña abierta antes) | Rechazo: "El funcionario ya tiene una cita el dd/mm/aaaa a las 10:00. Elija otro horario."; el formulario conserva lo escrito | — | PENDIENTE DE EJECUTAR |
| M-064 | Módulo 4, parte 2: pantallas de Citas, Ajax auxiliar y reporte / Pruebas manuale | Mismo día y hora con OTRO funcionario (otra área) | Aceptada | — | PENDIENTE DE EJECUTAR |
| M-065 | Módulo 4, parte 2: pantallas de Citas, Ajax auxiliar y reporte / Pruebas manuale | Abrir Nueva cita con el funcionario y fecha de los casos anteriores | Turnos ocupados en gris y deshabilitados; libres en verde y seleccionables | — | PENDIENTE DE EJECUTAR |
| M-066 | Módulo 4, parte 2: pantallas de Citas, Ajax auxiliar y reporte / Pruebas manuale | Reprogramar (modal "Continuar") a un turno libre | "Cita reprogramada correctamente."; contribuyente, área, funcionario y motivo de solo lectura; su turno actual aparece libre (verde) | — | PENDIENTE DE EJECUTAR |
| M-067 | Módulo 4, parte 2: pantallas de Citas, Ajax auxiliar y reporte / Pruebas manuale | Cancelar una cita (modal) y luego agendar otra en ese mismo turno | Cancelada con insignia gris; el turno vuelve a verde y se puede reservar | — | PENDIENTE DE EJECUTAR |
| M-068 | Módulo 4, parte 2: pantallas de Citas, Ajax auxiliar y reporte / Pruebas manuale | "Atender" una cita del día de hoy o de un día pasado | Insignia verde ATENDIDA. (Si el día mostrado es futuro, el botón aparece deshabilitado) | — | PENDIENTE DE EJECUTAR |
| M-069 | Módulo 4, parte 2: pantallas de Citas, Ajax auxiliar y reporte / Pruebas manuale | Intentar atender una cita CANCELADA (por ejemplo, con un formulario manipulado o con el listado desactualizado) | Mensaje rojo "Una cita CANCELADA ya no se puede modificar." En la pantalla normal las canceladas no tienen botones | — | PENDIENTE DE EJECUTAR |
| M-070 | Módulo 4, parte 2: pantallas de Citas, Ajax auxiliar y reporte / Pruebas manuale | Elegir una fecha de sábado o domingo en el formulario | La cuadrícula muestra "Solo se atiende de lunes a viernes."; al enviar, el campo fecha se marca en rojo | — | PENDIENTE DE EJECUTAR |
| M-071 | Módulo 4, parte 2: pantallas de Citas, Ajax auxiliar y reporte / Pruebas manuale | Escribir "Qu" en el buscador de contribuyentes | No consulta; ayuda "Escriba al menos 3 caracteres." Con 3 o más aparecen hasta 10 resultados | — | PENDIENTE DE EJECUTAR |
| M-072 | Módulo 4, parte 2: pantallas de Citas, Ajax auxiliar y reporte / Pruebas manuale | Reporte con datos (últimos 30 días) | Cuadro por área y cuadro por funcionario con total, programadas, atendidas, canceladas, % de atención y fila TOTAL | — | PENDIENTE DE EJECUTAR |
| M-073 | Módulo 4, parte 2: pantallas de Citas, Ajax auxiliar y reporte / Pruebas manuale | Reporte con desde = 2026-11-20 y hasta = 2026-11-01 | Mensaje rojo "La fecha "desde" no puede ser mayor que la fecha "hasta"."; no se consulta | — | PENDIENTE DE EJECUTAR |
| M-074 | Módulo 4, parte 2: pantallas de Citas, Ajax auxiliar y reporte / Pruebas manuale | "Exportar CSV" y abrirlo en Excel | `reporte-citas-<fecha>.csv`; columnas separadas, tildes y ñ correctas, los dos cuadros con su fila TOTAL | — | PENDIENTE DE EJECUTAR |
| M-075 | Módulo 4, parte 2: pantallas de Citas, Ajax auxiliar y reporte / Pruebas manuale | Entrar con `almacen1` y abrir `/sigmud/citas` (y `/sigmud/citas/reportes`) | 403 "Acceso denegado"; la tarjeta de Citas no aparece en su menú | — | PENDIENTE DE EJECUTAR |
| M-076 | Módulo 4, parte 2: pantallas de Citas, Ajax auxiliar y reporte / Pruebas manuale | Sin sesión, abrir `/sigmud/citas` | Redirige a `/login` | — | PENDIENTE DE EJECUTAR |
| M-077 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Ejecutar `sql/04_usuarios.sql` (sección 1) en Workbench | Sin errores; `SHOW COLUMNS FROM usuario` muestra `debe_cambiar_password` y `password_hash varchar(255)` | — | PENDIENTE DE EJECUTAR |
| M-078 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Desplegar e iniciar sesión con `admin` / `********`; luego `SELECT password_hash FROM usuario WHERE username='admin'` | Entra normalmente; el hash ahora empieza con `pbkdf2$120000$` (migración automática) | — | PENDIENTE DE EJECUTAR |
| M-079 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Menú → tarjeta "Usuarios" → listado | Ve los 4 usuarios con insignias de rol y estado, área y último acceso; la fila propia dice "usted" y sus botones "Desactivar" y "Restablecer clave" están deshabilitados | — | PENDIENTE DE EJECUTAR |
| M-080 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Filtrar por texto "atencion", por rol ALMACEN y por estado Inactivos | La lista se reduce según los filtros (se combinan) | — | PENDIENTE DE EJECUTAR |
| M-081 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Nuevo usuario: usuario `prueba01`, nombre, rol ATENCION **sin área** → Crear | El campo Área se marca en rojo y no se envía; en el servidor también se rechaza | — | PENDIENTE DE EJECUTAR |
| M-082 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Mismo formulario con área "Mesa de Partes" | Vuelve al listado con "Usuario creado" y un cuadro amarillo con la contraseña temporal (12 caracteres) y el botón "Copiar" | — | PENDIENTE DE EJECUTAR |
| M-083 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Pulsar "Copiar" y pegar en el bloc de notas; luego recargar la página | Se copia la contraseña; tras recargar el cuadro **no vuelve a aparecer** | — | PENDIENTE DE EJECUTAR |
| M-084 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Crear otro usuario con el nombre `prueba01` | Error "Ya existe un usuario con ese nombre de usuario"; lo escrito se conserva | — | PENDIENTE DE EJECUTAR |
| M-085 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Cerrar sesión; entrar como `prueba01` con la contraseña temporal | Entra, pero cualquier página (menú, citas, perfil) lo lleva a "Cambiar contraseña" con el aviso de contraseña temporal | — | PENDIENTE DE EJECUTAR |
| M-086 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | En esa pantalla escribir la nueva contraseña `abc`; luego `abcdefgh`; luego una buena | El medidor sube de "Débil" a "Fuerte" y la lista de requisitos se marca; con `abc` o `abcdefgh` el formulario no se envía | — | PENDIENTE DE EJECUTAR |
| M-087 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Nueva contraseña igual al nombre de usuario (`prueba01`); contraseña actual equivocada; confirmación distinta | Cada caso se rechaza con un mensaje claro (la actual equivocada cuenta como intento fallido) | — | PENDIENTE DE EJECUTAR |
| M-088 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Cambio correcto (por ejemplo `Definitiva2026x`) | Mensaje de éxito y pasa a "Mi perfil"; ya puede usar el sistema; la contraseña temporal ya no sirve | — | PENDIENTE DE EJECUTAR |
| M-089 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Como `prueba01` abrir `/sigmud/usuarios` | "Acceso denegado" (403) | — | PENDIENTE DE EJECUTAR |
| M-090 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Como `admin`: "Restablecer clave" de `prueba01` | Confirma y aparece un modal con la contraseña temporal nueva y "Copiar"; al cerrar el modal desaparece; `prueba01` vuelve a tener "Clave temporal" | — | PENDIENTE DE EJECUTAR |
| M-091 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Bloquear una cuenta: 5 contraseñas incorrectas seguidas con `prueba01`; luego, como `admin`, mirar la lista y el panel | La fila dice "Bloqueada"; el panel del ADMIN muestra 1 cuenta bloqueada (en rojo) | — | PENDIENTE DE EJECUTAR |
| M-092 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Pulsar "Desbloquear" en la lista de usuarios (y repetir con el botón del panel) | Mensaje "desbloqueada"; la cuenta vuelve a "Activo" y `prueba01` puede entrar | — | PENDIENTE DE EJECUTAR |
| M-093 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | "Desactivar" a `prueba01` (modal) y luego intentar entrar con ese usuario | Queda "Inactivo"; no puede iniciar sesión; "Activar" lo devuelve a la normalidad | — | PENDIENTE DE EJECUTAR |
| M-094 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Como `admin`: intentar desactivarse a sí mismo (botón deshabilitado; también probar enviando el formulario manipulado) | Rechazado con "No puede desactivar su propia cuenta" | — | PENDIENTE DE EJECUTAR |
| M-095 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Editar la propia cuenta del `admin`: el rol aparece bloqueado; intentar cambiarlo manipulando el formulario | Rechazado con "No puede cambiar su propio rol" | — | PENDIENTE DE EJECUTAR |
| M-096 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Dejar a `prueba01` como ADMIN activo, y con `admin` desactivar al otro ADMIN; luego intentar desactivar al último ADMIN | El último ADMIN no se puede desactivar ni cambiar de rol: "Siempre debe quedar al menos un administrador activo" | — | PENDIENTE DE EJECUTAR |
| M-097 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | Clic en el nombre del usuario en la barra | Abre "Mi perfil" con nombre, usuario, rol, área, último acceso anterior e inicio de la sesión actual | — | PENDIENTE DE EJECUTAR |
| M-098 | Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" / Tabla de pruebas  | `.\pruebas-seguridad.ps1` | 87 APROBADAS | — | PENDIENTE DE EJECUTAR |
| M-099 | Auditoría de cambios (solo ADMIN, solo lectura) / Tabla de pruebas manuales (pro | `atencion1` o `almacen1` abren `/auditoria` o `/auditoria?formato=csv` | 403 | 403 (aprobado) | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-100 | Auditoría de cambios (solo ADMIN, solo lectura) / Tabla de pruebas manuales (pro | Sin sesión abre `/auditoria` | Redirige a `/login` | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-101 | Auditoría de cambios (solo ADMIN, solo lectura) / Tabla de pruebas manuales (pro | `admin` abre `/auditoria` | Tabla con los últimos 7 días, 20 por página | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-102 | Auditoría de cambios (solo ADMIN, solo lectura) / Tabla de pruebas manuales (pro | `admin` hace `POST /auditoria` (con token) | 405: solo lectura | 405 | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-103 | Auditoría de cambios (solo ADMIN, solo lectura) / Tabla de pruebas manuales (pro | Periodo mayor de 366 días (servidor y navegador) | Mensaje de error, sin consultar | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-104 | Auditoría de cambios (solo ADMIN, solo lectura) / Tabla de pruebas manuales (pro | `' OR '1'='1` y `'; DROP TABLE auditoria; --` en los filtros | 0 resultados, tabla intacta | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-105 | Auditoría de cambios (solo ADMIN, solo lectura) / Tabla de pruebas manuales (pro | Login fallido con usuario nuevo | Aparece `LOGIN_FALLIDO` en `/auditoria` y la contraseña escrita no aparece en ningún lado | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-106 | Auditoría de cambios (solo ADMIN, solo lectura) / Tabla de pruebas manuales (pro | Usuario con salto de línea y `<script>` en el login | Se guarda en una sola línea y se muestra escapado (`&lt;script&gt;`) | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-107 | Auditoría de cambios (solo ADMIN, solo lectura) / Tabla de pruebas manuales (pro | Clic en una fila | Modal con el detalle campo por campo | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-108 | Auditoría de cambios (solo ADMIN, solo lectura) / Tabla de pruebas manuales (pro | Exportar CSV | Archivo `auditoria-AAAA-MM-DD.csv`, `;`, UTF-8 con BOM, con los mismos filtros | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-109 | Auditoría de cambios (solo ADMIN, solo lectura) / Tabla de pruebas manuales (pro | Panel ADMIN | Indicador rojo de accesos fallidos (si > 0) y "Actividad reciente" de 8 eventos | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-110 | Auditoría de cambios (solo ADMIN, solo lectura) / Tabla de pruebas manuales (pro | Panel ATENCION / ALMACEN | No reciben ni indicador ni actividad | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-111 | Auditoría de cambios (solo ADMIN, solo lectura) / Tabla de pruebas manuales (pro | Intentar `UPDATE`/`DELETE` en la tabla desde SQL | Error "La auditoría no se puede modificar ni eliminar" | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-112 | Ficha del contribuyente, reactivación y citas desde la ficha / Tabla de pruebas  | `almacen1` abre la ficha / intenta reactivar (con token) | 403 | 403 | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-113 | Ficha del contribuyente, reactivación y citas desde la ficha / Tabla de pruebas  | Sin sesión abre la ficha | Redirige a `/login` | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-114 | Ficha del contribuyente, reactivación y citas desde la ficha / Tabla de pruebas  | `abc`, `1 OR 1=1`, `999999`, `-5`, vacío, `0`, número gigante, sin id | 302 al listado con mensaje, sin 500 ni datos | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-115 | Ficha del contribuyente, reactivación y citas desde la ficha / Tabla de pruebas  | `atencion1` abre la ficha | 200, **sin** pestaña Actividad | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-116 | Ficha del contribuyente, reactivación y citas desde la ficha / Tabla de pruebas  | `atencion1` pide `&tab=actividad&rol=ADMIN` | Se ignora: 200 sin Actividad ni eventos | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-117 | Ficha del contribuyente, reactivación y citas desde la ficha / Tabla de pruebas  | `admin` abre la ficha | 200 con pestaña Actividad (últimos 10 eventos) | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-118 | Ficha del contribuyente, reactivación y citas desde la ficha / Tabla de pruebas  | Contribuyente con nombre `<script>alert(1)</script>` | Se ve como texto (`&lt;script&gt;`) | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-119 | Ficha del contribuyente, reactivación y citas desde la ficha / Tabla de pruebas  | Filtro `estado=X' OR '1'='1` | Se ignora (200) | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-120 | Ficha del contribuyente, reactivación y citas desde la ficha / Tabla de pruebas  | Baja desde la ficha sin token / con token | 403 / vuelve a la ficha con mensaje | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-121 | Ficha del contribuyente, reactivación y citas desde la ficha / Tabla de pruebas  | Ficha de un dado de baja | 200, insignia roja, Editar y Nueva cita deshabilitados, botón Reactivar | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-122 | Ficha del contribuyente, reactivación y citas desde la ficha / Tabla de pruebas  | Listado normal / con "Ver también dados de baja" | Sin el inactivo / con insignia y botón Reactivar | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-123 | Ficha del contribuyente, reactivación y citas desde la ficha / Tabla de pruebas  | Reactivar sin token / con token | 403 / vuelve a la ficha sin la insignia | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-124 | Ficha del contribuyente, reactivación y citas desde la ficha / Tabla de pruebas  | `CONSULTA_FICHA` y `REACTIVAR` en `/auditoria` | Aparecen los eventos | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-125 | Ficha del contribuyente, reactivación y citas desde la ficha / Tabla de pruebas  | Nueva cita desde la ficha | Contribuyente elegido y bloqueado | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-126 | Ficha del contribuyente, reactivación y citas desde la ficha / Tabla de pruebas  | `/citas?accion=nuevo&contribuyente=abc` (o inexistente, o dado de baja) | Se ignora; formulario normal con buscador | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-127 | Ficha del contribuyente, reactivación y citas desde la ficha / Tabla de pruebas  | Imprimir ficha (vista de impresión) | Sin barra ni botones; encabezado con fecha y usuario | aprobado | DOCUMENTADO EN avance.md (no repetido hoy) |
| M-128 | Cómo probar (Módulo 1) | Abrir `/sigmud/menu` sin sesión | Redirige a `/login` | — | PENDIENTE DE EJECUTAR |
| M-129 | Cómo probar (Módulo 1) | `admin` / `********` | Menú con 3 tarjetas "Próximamente", nombre, rol ADMIN y sesiones activas | — | PENDIENTE DE EJECUTAR |
| M-130 | Cómo probar (Módulo 1) | `atencion1` | Tarjetas Contribuyentes y Citas (sin Almacén) | — | PENDIENTE DE EJECUTAR |
| M-131 | Cómo probar (Módulo 1) | `almacen1` | Solo tarjeta Almacén | — | PENDIENTE DE EJECUTAR |
| M-132 | Cómo probar (Módulo 1) | Clave incorrecta | Mensaje genérico, `intentos_fallidos` +1 | — | PENDIENTE DE EJECUTAR |
| M-133 | Cómo probar (Módulo 1) | Usuario inexistente | Mismo mensaje genérico | — | PENDIENTE DE EJECUTAR |
| M-134 | Cómo probar (Módulo 1) | 5 claves incorrectas y luego la correcta | "La cuenta está bloqueada…", `estado = 0` | — | PENDIENTE DE EJECUTAR |
| M-135 | Cómo probar (Módulo 1) | `' OR '1'='1` como usuario | Mensaje genérico (no pasa el regex ni el SQL) | — | PENDIENTE DE EJECUTAR |
| M-136 | Cómo probar (Módulo 1) | `almacen1` abre `/sigmud/citas` | 403 "Acceso denegado" | — | PENDIENTE DE EJECUTAR |
| M-137 | Cómo probar (Módulo 1) | `admin` abre `/sigmud/citas` | 404 (el módulo no existe aún) | — | PENDIENTE DE EJECUTAR |
| M-138 | Cómo probar (Módulo 1) | Cerrar sesión y botón "atrás" | No muestra el menú; pide login | — | PENDIENTE DE EJECUTAR |

## 7. Análisis

### 7.1 Porcentaje de requerimientos cumplidos
Los requerimientos de `02_requerimientos.md` se dedujeron de lo implementado, así que **el 100 % (33 de 33 RF y 23 de 23 RNF) está implementado por construcción**.
Lo que sí se puede medir es **cuántos tienen una prueba repetible que lo respalde** (ver la matriz del Anexo E):

| Medida | Resultado | Porcentaje |
|---|---|---|
| RF con prueba repetible que cubre lo esencial | 27 de 33 | 81,8 % |
| RF con prueba solo parcial (RF-13, RF-14, RF-15, RF-24) | 4 de 33 | 12,1 % |
| RF sin prueba repetible en lo esencial (RF-20, RF-25) | 2 de 33 | 6,1 % |
| RNF con prueba automática | 14 de 23 | 60,9 % |
| RNF con prueba solo parcial (RNF-10, RNF-11, RNF-12) | 3 de 23 | 13,0 % |
| RNF sin prueba (RNF-09, RNF-13, RNF-15, RNF-20) | 4 de 23 | 17,4 % |
| RNF verificados por inspección (RNF-16, RNF-18) | 2 de 23 | 8,7 % |
| **Requerimientos con prueba que cubre lo esencial (RF + RNF)** | **41 de 56** | **73,2 %** |

Resultado de las pruebas ejecutadas: **100 % aprobadas** (376 de 376 JUnit y 207 de 207 del script), sin fallos.

### 7.2 Defectos encontrados y corregidos (según `docs/auditoria.md` y `docs/avance.md`)
| Hallazgo | Corrección | Fuente |
|---|---|---|
| No había protección CSRF | Token por sesión, `CsrfFilter`, campo oculto en los formularios y cabecera en Ajax | auditoria.md §3 |
| Sin cabeceras de seguridad | `SecurityHeadersFilter` (nosniff, DENY, Referrer-Policy, CSP) | auditoria.md §3 |
| Cookie de sesión sin SameSite y con el id posible en la URL | `SameSite=Strict` y `tracking-mode COOKIE` | auditoria.md §3 |
| Intentos fallidos y bloqueos no quedaban en el log | `LoginServlet` los registra (sin contraseña y con el usuario saneado) | auditoria.md §3 |
| El orden de los filtros no estaba garantizado | Declarados en `web.xml` en orden | auditoria.md §3 |
| Código CSV duplicado | Un solo `util/Csv` | auditoria.md §3 |
| 12 clases DAO sin comentario | Se agregó un comentario a cada una | auditoria.md §3 |
| `web.xml` sin páginas de error 400 y 405 (mostraban la versión de Tomcat) | Se agregaron a `error.jsp` | auditoria.md §1(f) |
| Texto del 403 que hablaba solo de rol | Se reescribió porque el CSRF también produce 403 | auditoria.md §2 |
| Hash SHA-256 sin sal | PBKDF2 con sal (los antiguos se migran al iniciar sesión) | auditoria.md §4 y §6 |

### 7.3 Limitaciones detectadas
Las que reconoce `docs/auditoria.md` y las que se detectaron al preparar esta documentación (detalle en `observaciones.md`):
- Sin pruebas de carga ni de concurrencia real; sin prueba de `AuthFilter`/`CsrfFilter`/Servlets en JUnit (solo el script en vivo).
- Sin pruebas de `MaterialDAOImpl`, de los DAO de reportes ni de `AreaDAOImpl`; sin pruebas automáticas del JavaScript (jQuery, `toast.js`, `tema.js`).
- 90 casos manuales de `docs/avance.md` siguen sin resultado.
- Las pruebas de DAO necesitan `mun_quinua_test` (script `03` como root); en esta ejecución se usó una instancia temporal.
- El bloqueo por 5 intentos es por usuario (no por IP); las sesiones abiertas siguen vigentes si se desactiva al usuario; no hay HTTPS (cookie sin `Secure`).
- Los triggers de auditoría requieren root: en la base real del usuario no estaban creados al momento de preparar esto (ver `observaciones.md`).

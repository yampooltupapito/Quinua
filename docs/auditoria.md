# SIGMUD – Auditoría de seguridad y calidad

Fecha de la revisión: 14/11/2026. Alcance: todo `src/main` (Java, JSP, JS, `web.xml`) tras construir los 4 módulos. Los comandos se ejecutaron desde la raíz del proyecto (Git Bash); se muestran tal como se usaron.

## A. Comprobaciones

### 1. (a) Ningún SQL se arma concatenando datos del usuario — APROBADO

Comando (busca literales SQL concatenados con algo que no sea una constante):
```
grep -rnE '"[^"]*(SELECT|INSERT|UPDATE|DELETE)[^"]*"\s*\+' src/main/java | grep -v "SELECT_BASE\|COLUMNAS\|CONTEOS"
```
Resultado: sin coincidencias.

Comando (todos los puntos de ejecución de SQL):
```
grep -rnE 'prepareStatement\(|createStatement\(|executeQuery\("|executeUpdate\("' src/main/java
```
Resultado: 38 llamadas, **todas** `prepareStatement(...)`; ninguna `createStatement`, ni `executeQuery("...")` ni `executeUpdate("...")` con texto directo. Los 3 sitios donde el SQL se arma en código se revisaron uno por uno:

| Archivo | Cómo se arma | Veredicto |
|---|---|---|
| `CitaDAOImpl.listarPorFecha` | `SQL_POR_FECHA + (idArea == null ? "" : FILTRO_AREA) + ORDEN_FECHA` | Solo constantes; el área entra con `ps.setInt` |
| `ReporteAlmacenDAOImpl.movimientosPorFecha` | `StringBuilder` + `sql.append(FILTRO_TIPO / FILTRO_AREA / FILTRO_MATERIAL)` | Solo fragmentos constantes; los valores van en una lista y se asignan con `setObject` |
| `ReporteCitasDAOImpl.consultar(sql, ...)` | recibe `SQL_POR_AREA` o `SQL_POR_FUNCIONARIO` | Solo constantes; las fechas con `setDate` |

Los `LIKE` de las búsquedas escapan `%`, `_` y `\` y usan parámetro. Prueba en vivo: ver `pruebas-seguridad.ps1` (`' OR '1'='1` y `admin' --` como usuario devuelven 401).

### 1. (b) No queda ningún `javax.` de Java EE — APROBADO
```
grep -rn "javax\." src pom.xml
```
Resultado: sin coincidencias (todo es `jakarta.*`).

### 1. (c) No hay scriptlets en los JSP — APROBADO
```
grep -rnE '<%[^@-]' src/main/webapp --include=*.jsp --include=*.jspf
```
Resultado: sin coincidencias (solo existen directivas `<%@ ... %>` y comentarios `<%-- --%>`). Además `web.xml` tiene `<scripting-invalid>true</scripting-invalid>`, así que Tomcat rechazaría cualquier scriptlet.

### 1. (d) No hay enlaces a CDN — APROBADO
```
grep -rnE 'https?://|//cdn|@import' src/main/webapp --include=*.jsp --include=*.jspf --include=*.xml --include=sigmud.css --include=reportes.css --include=login.js --include=almacen.js --include=contribuyentes.js --include=movimientos.js --include=citas.js --include=reportes.js
```
Resultado: solo 3 líneas de `web.xml`, que son los espacios de nombres XML (`xmlns`, `xsi:schemaLocation`), no cargas de recursos.
Se revisaron también las librerías locales (`bootstrap*.css/js`, `bootstrap-icons.min.css`): sus URLs son comentarios de licencia y el `xmlns` de SVG dentro de imágenes `data:`; ninguna se descarga. Además, la política CSP (punto B.4) impide en el navegador cargar cualquier recurso externo.

### 1. (e) Todo texto del usuario impreso en un JSP pasa por `<c:out>` — APROBADO
Comandos:
```
grep -rnE '\$\{' src/main/webapp/WEB-INF/views --include=*.jsp | grep -vE '<c:(out|if|when|forEach|set|param|choose|url)\b' | grep -v contextPath
grep -rn 'escapeXml' src/main/webapp
grep -rnE '\$\{[^}]*\b(param|header|cookie|paramValues)\.' src/main/webapp/WEB-INF/views
grep -nE '\.html\(|innerHTML|document\.write|eval\(|insertAdjacentHTML' src/main/webapp/assets/js/{login,almacen,contribuyentes,movimientos,citas,reportes}.js
```
Resultados:
- Las ~90 expresiones `${...}` fuera de `<c:out>` se revisaron una por una: son identificadores numéricos (`x.idMaterial`, `ct.idCita`), booleanos usados para elegir clases/atributos (`${esNuevo ? '' : 'readonly'}`), constantes del servidor (`${mensajeTipo}` = `success`/`danger`, claves de orden, la lista de unidades), fechas ya validadas (`${fecha}`) y direcciones construidas con `<c:url>`/`<c:param>` (codifica el texto). **Ningún texto escrito por el usuario se imprime sin `<c:out>`.**
- No existe `escapeXml="false"`.
- Ninguna vista lee `param.`, `header.` ni `cookie.` directamente.
- Los scripts no usan `.html(`, `innerHTML`, `document.write` ni `eval`; construyen el DOM con `.text()` y `.val()`.
- Los JSON (Gson) escapan `<`, `>` y `&` por defecto, y el CSV escapa fórmulas de Excel (`util/Csv`).
- Prueba en vivo: un contribuyente llamado `<script>alert(1)</script>` aparece como `&lt;script&gt;...` en el listado, y el texto de búsqueda reflejado también se escapa (`pruebas-seguridad.ps1`).

### 1. (f) Ningún mensaje de error muestra stack traces ni nombres de tablas — APROBADO (con 1 corrección)
Comandos:
```
grep -rn 'getMessage()' src/main/java | grep -v 'LOG\.\|registrar'
grep -rnE 'printStackTrace|System\.(out|err)' src/main/java
grep -rnE '"[^"]*(movimiento_stock|id_[a-z]+|FROM |password_hash|intentos_fallidos)[^"]*"' src/main/java/pe/gob/munquinua/sigmud/controller .../exception .../filter
grep -rniE 'movimiento_stock|password_hash|id_usuario' src/main/webapp/WEB-INF/views
```
Resultados:
- `e.getMessage()` solo se muestra para excepciones **de negocio** con mensajes propios (`CredencialesInvalidas`, `CuentaBloqueada`, `StockInsuficiente`, `MaterialNoDisponible`, `CitaCruzada`, `CitaEstado`, `CitaNoEncontrada`); las `DaoException` siempre se capturan, se registran en el log (`LOG.log(SEVERE, ..., e)`) y el usuario recibe "No se pudo completar la operación. Intente nuevamente.".
- Sin `printStackTrace` ni `System.out`.
- Sin nombres de tablas ni columnas en textos mostrados (las coincidencias de "contribuyente" son palabras del mensaje, no la tabla).
- **Hallazgo y corrección:** `web.xml` no definía páginas de error para **400** y **405**; Tomcat mostraría su pantalla por defecto con el número de versión del servidor. Se agregaron ambas a `error.jsp`. Verificado en vivo: GET a una ruta inexistente (404) y POST a `/citas/ajax` (405) devuelven la página propia, sin "Apache Tomcat" ni "Exception".

## 2. Páginas de error — APROBADO (con 1 ajuste de texto)
- `web.xml` mapea 400, 403, 404, 405, 500 y `java.lang.Throwable` a `/WEB-INF/views/error.jsp`.
- `error.jsp` solo usa `pageContext.errorData.statusCode` (un número) para escoger el título y un texto fijo; no imprime la excepción, la ruta ni la versión del servidor. Usa Bootstrap, los íconos y `sigmud.css` locales (mismo estilo del sistema) y tiene el botón "Volver al menú".
- Ajuste: el texto del 403 decía "Su rol no tiene permiso…", pero ahora el 403 también lo produce el filtro CSRF; ahora dice "No tiene permiso para esta sección o acción. Si el problema continúa, vuelva al menú e inicie sesión de nuevo."

## 3. Otros hallazgos corregidos en esta revisión
| Hallazgo | Corrección |
|---|---|
| No había protección CSRF | Token por sesión + `CsrfFilter` + campo oculto en los 8 formularios POST + cabecera en Ajax |
| Sin cabeceras de seguridad | `SecurityHeadersFilter` (nosniff, DENY, Referrer-Policy, CSP) |
| Cookie de sesión sin SameSite; el id podía viajar en la URL | `SameSite=Strict` y `tracking-mode COOKIE` en `web.xml` |
| Intentos fallidos y bloqueos no quedaban en el log | `LoginServlet` registra usuario y fecha/hora (sin contraseña, usuario saneado contra inyección en el log) |
| El orden de los filtros no estaba garantizado (anotaciones) | Los 3 filtros se declaran en `web.xml` en orden: cabeceras → sesión y roles → CSRF |
| Código CSV duplicado en `ReporteAlmacenServlet` y `Csv` | `ReporteAlmacenServlet` usa `util/Csv` |
| 12 clases DAO sin comentario de clase | Se agregó un comentario breve a cada una |

## 4. Limitaciones conocidas (no se cambiaron; decisiones de diseño o fuera de alcance)
- ~~Hash SHA-256 sin sal~~ **(resuelto en la gestión de usuarios):** las contraseñas ahora usan PBKDF2-HMAC-SHA256 con sal aleatoria (ver la sección 6). Mientras un usuario no inicie sesión por primera vez con la versión nueva, conserva su hash antiguo (SHA-256 sin sal); por ejemplo `atencion2`, si no ha entrado.
- **Cierre de sesión por GET (`/logout`):** un sitio externo podría cerrar la sesión de un usuario (riesgo bajo; con `SameSite=Strict` la cookie ni siquiera viaja en peticiones de otros sitios). Cambiarlo a POST implicaría cambiar la barra de navegación.
- **Sin HTTPS:** la cookie no lleva el atributo `Secure` porque se prueba por HTTP en localhost; en producción debe usarse HTTPS y activar `<secure>true</secure>`.
- **CSP con `style-src 'unsafe-inline'`:** se pidió así (los atributos `style` de las vistas); `script-src` es solo `'self'`, que es lo que frena el XSS.
- El bloqueo por 5 intentos es por usuario (no por dirección IP): permite que un atacante bloquee cuentas ajenas escribiendo mal su contraseña 5 veces.
- La prueba en vivo no cubre carga ni concurrencia.

## 5. Revisión de los paneles (dashboards)

| Comprobación | Comando / método | Resultado |
|---|---|---|
| Sin scriptlets en vistas ni en el tag | `grep -rnE '<%[^@-]' src/main/webapp --include=*.jsp --include=*.jspf --include=*.tag` | Sin coincidencias |
| Sin scripts dentro del HTML (compatible con la CSP) | `grep -rn '<script' src/main/webapp/WEB-INF \| grep -v 'src='` y `grep -rnE ' on(click\|load\|change\|submit\|error\|input)=' src/main/webapp/WEB-INF` | Solo `<script src=...>`; ningún manejador `on...=`, ningún `<style>` |
| Sin `javax.` ni CDN en lo nuevo | `grep -rn "javax\." src pom.xml`; `grep -rnE 'https?://' src/main/webapp/WEB-INF/views src/main/webapp/WEB-INF/tags src/main/webapp/assets/js/dashboard.js` | Sin coincidencias |
| Listas del panel sin HTML | Revisión de `dashboard.js` | Todo se arma con `.text()`/`.val()`; ningún `.html()` |
| SQL sin concatenar datos del usuario | Revisión de `DashboardDAOImpl` | Solo texto fijo; fechas e ids como parámetros `?`; `LIMIT 5` es constante y `LIMIT ?` va parametrizado |
| Rol e id solo de la sesión | Lectura de `DashboardServlet`, `DashboardDatosServlet`, `DashboardDesbloquearServlet` y prueba en vivo con parámetros manipulados | Ninguno lee parámetros para decidir el rol ni el funcionario (salvo `id` de la cuenta a desbloquear, que solo ADMIN puede enviar) |
| `/dashboard/*` sin sesión | `pruebas-seguridad.ps1` | 401 (JSON) |
| Desbloqueo solo ADMIN y con CSRF | `AuthFilter` + comprobación en el servlet + `pruebas-seguridad.ps1` | 403 para ATENCION y ALMACEN (aun con token válido) y 403 sin token |
| Mensajes de error | Revisión | Errores técnicos al log; el usuario ve un mensaje genérico; no se muestran nombres de tablas |

Limitación: el reloj de "hoy" y "mes" es el de la aplicación (JVM); si el servidor MySQL estuviera en otra zona horaria, las fechas guardadas con `NOW()` por la propia base podrían diferir en horas.

## 6. Gestión de usuarios, contraseñas y "Mi perfil"

### Hash de contraseñas con sal (PBKDF2)
| Aspecto | Antes | Ahora |
|---|---|---|
| Algoritmo | SHA-256 sin sal | PBKDF2-HMAC-SHA256 (biblioteca estándar de Java, sin dependencias nuevas) |
| Sal | ninguna (dos usuarios con la misma contraseña tenían el mismo hash) | 16 bytes aleatorios (`SecureRandom`) por contraseña |
| Costo | 1 pasada | 120000 iteraciones (se guardan en cada hash, así se pueden subir sin romper los viejos) |
| Formato | 64 hexadecimales | `pbkdf2$iteraciones$salBase64$hashBase64` (83 caracteres; la columna pasó a `VARCHAR(255)`) |
| Comparación | tiempo constante | tiempo constante (`MessageDigest.isEqual`); formato inválido o iteraciones fuera de 1000–1000000 → `false`, nunca excepción |
| Migración | — | `Seguridad.verificar` acepta ambos formatos y `LoginServlet` regraba la contraseña con el formato nuevo al primer inicio de sesión correcto (verificado: `admin`, `atencion1` y `almacen1` pasaron a `pbkdf2$120000$...` con las mismas contraseñas; `atencion2`, que no entró, conservó el hash antiguo). Si la migración fallara, el inicio de sesión continúa y se avisa en el log |
| Usuario inexistente | — | se verifica contra un hash PBKDF2 de relleno, para que tarde lo mismo que un usuario real |

`javax.crypto` (PBKDF2) es de la biblioteca estándar de Java y no del Java EE antiguo: el comando de la comprobación 1(b) solo debe excluirlo (`grep -rn "javax\." src pom.xml | grep -v javax.crypto`).

### Política y reglas de contraseñas y cuentas
| Regla | Dónde se verifica |
|---|---|
| Mínimo 8 caracteres, una mayúscula, una minúscula, un número, distinta del nombre de usuario (máximo 100) | `Validador.politicaPassword` (servidor) y `password.js` (navegador) |
| Contraseña temporal de 12 caracteres con `SecureRandom`, que cumple la política y sin caracteres que se confunden | `Seguridad.generarPasswordTemporal` (300 generaciones probadas) |
| La temporal solo se guarda **hasheada** (con `debe_cambiar_password = 1`); se muestra **una sola vez** al ADMIN y nunca se escribe en el log | `UsuarioServlet` (queda en la sesión solo hasta el siguiente listado), `UsuarioAjaxServlet` (viaja solo en la respuesta JSON) |
| Cambio obligatorio: con la clave temporal solo se puede usar `/perfil/password`, `/logout` y `/assets` | `AuthFilter` (verificado en vivo: `/menu`, `/almacen`, `/perfil` y `/dashboard/datos` redirigen) |
| La clave actual incorrecta al cambiarla cuenta como intento fallido (5 = bloqueo y cierre de sesión) y se registra en el log | `PerfilServlet` |
| Tras cambiar la clave se cambia el id de sesión (`changeSessionId`) | `PerfilServlet` (verificado: el cambio envía un `Set-Cookie JSESSIONID` nuevo) |
| Un ADMIN no puede desactivarse a sí mismo, ni cambiar su propio rol, ni restablecer su propia clave desde la lista | `UsuarioServlet`, `UsuarioAjaxServlet` |
| Siempre debe quedar al menos un ADMIN activo | `UsuarioDAOImpl` (transacción con `SELECT ... FOR UPDATE`: dos cambios simultáneos no pueden dejar el sistema sin ADMIN) + mensaje en `UsuarioServlet` |
| ATENCION exige área; nombre de usuario único (3 a 30 caracteres) | `UsuarioValidador`, `existeUsername` y el UNIQUE de la base |
| Un usuario nunca se borra: solo se activa o desactiva (al activar se reinician los intentos) | `UsuarioDAOImpl.cambiarEstado` |
| Solo ADMIN: `/usuarios/*` | `AuthFilter` por ruta + comprobación del rol de la sesión en `UsuarioAjaxServlet` |
| Registro en el log (sin contraseñas): usuario creado, activado/desactivado, contraseña restablecida, cuenta desbloqueada, contraseña cambiada, contraseña migrada | `UsuarioServlet`, `UsuarioAjaxServlet`, `PerfilServlet`, `LoginServlet` |

### Comprobaciones de código de esta etapa
| Comprobación | Resultado |
|---|---|
| Sin scriptlets (`.jsp`, `.jspf`, `.tag`) | Sin coincidencias |
| Sin scripts dentro del HTML (CSP) | Solo `<script src=...>`; todo el JavaScript de esta etapa está en `usuarios.js` y `password.js` |
| Todo texto del usuario escapado | `<c:out>` en listado, formulario, perfil y cambio de clave; las listas dinámicas del panel usan `.text()` |
| Sin imports sin usar en todo `src` | Revisado: sin resultados |
| `e.getMessage()` hacia la pantalla | Solo en excepciones de negocio con mensajes propios (`UltimoAdminException`, etc.) |

### Limitaciones conocidas de esta etapa (no se cambiaron)
- **Sesiones abiertas:** si el ADMIN desactiva a un usuario o le restablece la contraseña, sus sesiones ya abiertas siguen funcionando hasta que expiren o se cierren (el filtro no consulta la base en cada petición). La contraseña restablecida sí impide el siguiente inicio de sesión con la anterior.
- **Contraseña temporal en la sesión:** entre la creación del usuario y la carga del listado (una redirección) la contraseña temporal vive en la memoria de la sesión del ADMIN; se borra al mostrarse. Si Tomcat guardara sesiones en disco justo en ese instante podría quedar en el archivo de sesiones.
- **Subir las iteraciones:** `Seguridad.ITERACIONES` solo afecta a los hashes nuevos; los existentes conservan las suyas hasta que su dueño cambie la contraseña.
- **Mostrar la contraseña temporal por la pantalla del ADMIN** es una decisión de diseño pedida; en producción conviene entregarla por un canal seguro y, mejor aún, enviar un enlace de activación.

## 7. Auditoría de cambios (registro inmutable)

### Qué protege y cómo
- **Inmutabilidad:** los triggers `trg_auditoria_no_update` y `trg_auditoria_no_delete` rechazan cualquier `UPDATE` o `DELETE`
  (`SQLSTATE 45000`). La aplicación ni siquiera tiene una operación de edición o borrado: `AuditoriaServlet` solo implementa `GET`
  (un `POST` devuelve 405). Comprobado con JUnit y con el cliente `mysql`.
- **Solo ADMIN:** `/auditoria` está en el mapa de roles de `AuthFilter`; `atencion1` y `almacen1` reciben 403 (también en el CSV).
- **Sin secretos:** `Auditoria.diferencias` omite cualquier campo cuyo nombre contenga password, passwd, hash, clave, contraseña, token o
  secret; la contraseña temporal y la definitiva nunca se pasan a la auditoría (comprobado: la clave escrita en un login fallido no aparece).
- **Anti-inyección de registros (log forging):** `Auditoria.limpiar` convierte saltos de línea y caracteres de control en espacio y recorta
  cada columna, de modo que un usuario como `admin\r\n2026-01-01 LOGIN_OK falso` queda en una sola línea.
- **XSS:** todo se imprime con `<c:out>` (atributos `data-*` incluidos) y el modal se rellena con `.text()`. Comprobado con un usuario
  `<script>alert(7)</script>`: se ve como texto.
- **SQL:** consultas con `PreparedStatement`; los filtros opcionales agregan fragmentos fijos, `LIMIT`/`OFFSET` van como parámetros y `%`/`_` se escapan.
- **Origen de los datos:** usuario e IP salen de la sesión/petición del servidor, nunca de un parámetro (solo en `LOGIN_FALLIDO` se guarda el
  usuario escrito, porque aún no hay sesión; ya saneado).

### Comprobaciones de esta etapa (todas aprobadas)
| Comprobación | Resultado |
|---|---|
| `UPDATE` / `DELETE` directos sobre `auditoria` | fallan por trigger (JUnit y cliente `mysql`) |
| Registrar dentro de una transacción con `rollback` | no deja rastro (JUnit) |
| Movimiento fallido (stock insuficiente / clave foránea) | no deja auditoría; movimiento exitoso sí (JUnit) |
| Rol sin permiso en `/auditoria` y en el CSV | 403 (script) |
| Inyección SQL en los filtros | 0 resultados, tabla intacta (JUnit y script) |
| Usuario con `\r\n` y `<script>` en el login | una sola línea y escapado (script) |
| JUnit / script de seguridad | 335 sin fallos / 106 de 106 |

### Limitaciones conocidas de esta etapa (no se cambiaron)
- Un administrador con acceso `root` a MySQL puede quitar los triggers o usar `TRUNCATE`; la inmutabilidad protege contra la aplicación y los errores, no contra un DBA malintencionado.
- Los eventos fuera de la transacción son de "mejor esfuerzo": si la auditoría falla en ese instante, la operación se hace igual y el error queda solo en el log.
- La IP es `getRemoteAddr()`: detrás de un proxy inverso sería la del proxy.
- `REACTIVAR` está definido pero ninguna pantalla reactiva todavía contribuyentes ni materiales.
- La tabla crece sin límite (no hay archivado ni purga); el CSV exporta como máximo 10000 filas por descarga.
- Las filas creadas por `pruebas-seguridad.ps1` en la base real no se pueden borrar (quedan con usuarios `segpru###`/`segaud###`).

## 8. Ficha del contribuyente y protección de datos personales (Ley N.º 29733)

### Por qué se audita `CONSULTA_FICHA`
La ficha reúne **datos personales** (nombre, documento, teléfono, correo, dirección, fecha de nacimiento) y su historial de
atenciones. La Ley N.º 29733 (Ley de Protección de Datos Personales) exige confidencialidad y poder demostrar quién accedió a
esos datos. Por eso **cada apertura de una ficha** registra `CONSULTA_FICHA` (usuario, fecha y hora, IP, DNI/RUC y nombre del
contribuyente consultado). Así, el administrador puede responder "¿quién vio esta ficha y cuándo?" desde `/auditoria`
(filtro por acción `CONSULTA_FICHA`) o desde la pestaña **Actividad** de la propia ficha. Se registra incluso si el
contribuyente está dado de baja (sus datos siguen protegidos).

- **Mejor esfuerzo:** `Auditoria.registrar` nunca lanza excepción; si falla, la ficha se muestra igual y el error queda en el log (así una falla de auditoría no deja sin atención al ciudadano).
- **No se guardan los datos consultados**, solo que hubo una consulta (la auditoría no duplica datos personales: guarda el nombre y el documento que identifican la ficha).
- **Alcance:** se audita la consulta de la ficha. El listado de contribuyentes y el buscador no generan eventos (se consideran de uso operativo corriente).

### Controles de la ficha
| Control | Cómo se cumple |
|---|---|
| Solo personal autorizado | `AuthFilter`: ADMIN y ATENCION; ALMACEN recibe 403; sin sesión → login |
| La pestaña Actividad solo es del ADMIN | El **servidor** decide con el rol de la **sesión**: para ATENCION no se consulta la auditoría ni se genera el HTML de la pestaña; los parámetros (`tab`, `rol`, `verActividad`…) se ignoran. Comprobado con el script de seguridad |
| Identificador inválido | Se valida como entero positivo; si no, mensaje y vuelta al listado. Nunca 500 ni datos |
| SQL | Todas las consultas nuevas con `PreparedStatement`; el filtro de estado solo agrega un fragmento fijo y su valor va como parámetro (un valor desconocido se ignora en el servlet) |
| XSS | `<c:out>` en todos los textos, incluido el nombre con `<script>` (comprobado) |
| CSRF | Baja y reactivar son POST con token (403 sin token) y Post-Redirect-Get |
| Impresión | La hoja de impresión es solo texto (sin logotipos); lleva el usuario que imprime y la fecha/hora reales de impresión, para saber quién sacó una copia en papel |

### Comprobaciones de esta etapa (todas aprobadas)
- JUnit: filtro de auditoría por entidad e id, `ultimosPorEntidad` (máx. 10), `CONSULTA_FICHA` válida.
- Script: ALMACEN 403 en ficha y en reactivar; sin sesión → login; ids inválidos (`abc`, `1 OR 1=1`, inexistente, negativo, vacío, enorme) → listado con mensaje; ATENCION sin pestaña Actividad ni por parámetros; nombre con `<script>` escapado; `CONSULTA_FICHA` y `REACTIVAR` visibles en `/auditoria`; baja/reactivar sin token → 403. Total: 141 de 141.

### Limitaciones conocidas de esta etapa (no se cambiaron)
- Cada recarga de la ficha genera un evento (también al filtrar por estado): es deliberado, pero la tabla crece más rápido.
- ATENCION puede ver la ficha de **cualquier** contribuyente, no solo de los que atendió (el sistema no tiene la noción de "mis contribuyentes"); queda registrado en la auditoría.
- La impresión guarda en la hoja el usuario y la hora, pero el navegador no avisa al servidor: **imprimir no genera evento de auditoría** (solo la consulta previa de la ficha).
- La edad se calcula con la fecha actual del servidor.

## 9. Paquete visual: lista blanca de orden, tema oscuro y CSP

### Orden por columna sin inyección SQL (lista blanca)
El orden de una tabla (`?orden=nombre&dir=desc`) es el único lugar donde un nombre elegido por el usuario podría acabar dentro del SQL,
porque `ORDER BY` **no admite parámetros** (`?`). Por eso **nunca se concatena lo que llega del navegador**:
1. Cada DAO tiene un `Map` fijo (`ORDEN`) que traduce el nombre permitido (por ejemplo `nombre`) a su fragmento de SQL (`nombres_razon_social`).
2. `OrdenSql.resolver` solo usa la columna pedida **si es una clave del Map**; si no (o trae `;`, `--`, comillas, espacios, paréntesis…), usa el
   orden por defecto del DAO.
3. La dirección no se copia: se escribe el literal `ASC` o `DESC` según el valor recibido (otro valor = `ASC`).
4. `LIMIT` y `OFFSET` van como **parámetros** (`LIMIT ? OFFSET ?`); la página se corrige a un entero entre 1 y la última y el tamaño solo puede ser 10, 20 o 50.
5. Los filtros (texto, estado, rol…) siguen siendo parámetros `?` (con `%` y `_` escapados).

Comprobado con JUnit (`OrdenSqlTest`: 18 ataques distintos, y que los fragmentos de las 5 listas blancas solo contienen `[a-z_.]`),
con la base de pruebas (`ListadosPaginadosDAOTest`: `nombre;DROP TABLE contribuyente` y `1 OR 1=1` ordenan por defecto y la tabla sigue intacta)
y en vivo con el script (36 combinaciones hostiles de `orden`, `dir`, `pagina` y `tamano` en 3 listados: siempre 200 con el listado normal).

### Política de seguridad de contenido (CSP) y JavaScript
- La CSP no se tocó: `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; frame-ancestors 'none'`.
  `script-src 'self'` impide cualquier script en línea o de otro origen, **también el del tema**: por eso `tema.js` es un archivo externo cargado en el `<head>`
  (síncrono, sin parpadeo) y no un script dentro del HTML.
- Todos los comportamientos nuevos (tema, toasts, paginación, ficha) son archivos `.js` locales. Los mensajes del servidor viajan en atributos `data-*`
  escapados y se leen con JavaScript; **nunca se escribe HTML con datos**: toasts, modales, listas y actividad usan `textContent`/`.text()`.
- `style-src` mantiene `'unsafe-inline'` porque Bootstrap y Chart.js asignan estilos en línea desde JavaScript; los JSP **no** tienen `<style>` ni `style=` nuevos
  (solo atributos ya existentes de anchos).
- Sin CDN ni fuentes externas: Bootstrap, Bootstrap Icons y Chart.js están en `assets/`; el sistema se ve igual sin internet (los íconos son una fuente local).
- Comprobación automática: el script busca en el código fuente de todos los JSP y tags y exige 0 `<script>` en línea, 0 `on…=`, 0 `javascript:`, 0 `<style>` y 0 enlaces
  a CDN, y comprueba que la cabecera CSP está en 7 páginas distintas.

### Tema claro/oscuro y almacenamiento del navegador
- La elección se guarda en `localStorage` (clave `sigmud-tema`) solo con el valor `light` o `dark`: **no contiene datos personales** ni de sesión.
  Todo acceso está en `try/catch` (modo privado o almacenamiento bloqueado → se usa el tema del sistema y no falla nada).
- La impresión fuerza el tema claro, para que un documento impreso o guardado en PDF sea legible y no gaste tinta.

### Limitaciones conocidas de esta etapa (no se cambiaron)
- El logo oficial lo coloca la municipalidad; si el archivo es muy grande o no es PNG válido, el navegador lo mostrará roto (solo se comprueba que exista).
- El listener lee el logo al **arrancar**: si se agrega el archivo con el sistema encendido, hay que reiniciar (o redeploy) para que se muestre.
- La paginación recalcula el total en cada pedido (dos consultas por listado): suficiente para el volumen del distrito; con millones de filas habría que cachear el conteo.
- La ficha del contribuyente registra `CONSULTA_FICHA` también al cambiar de página o de orden del historial de citas (es una consulta nueva de la ficha).
- La lista blanca debe actualizarse a mano si se agregan columnas ordenables (es deliberado: ninguna columna es ordenable "por defecto").

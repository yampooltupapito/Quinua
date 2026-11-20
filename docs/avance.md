# SIGMUD – Avance

## Configuración base (hecha)

- `pom.xml` (pe.gob.munquinua:sigmud, finalName `sigmud`, release 17, servlet-api 6.0.0 provided, JSTL 3.0, mysql-connector-j 8.4.0, gson 2.11.0).
- Paquetes `pe.gob.munquinua.sigmud`: `model`, `dao`, `dao.impl`, `controller`, `filter`, `listener`, `util`, `exception`.
- `db.properties`, `ConexionBD`, `Seguridad`, `Validador`, `DaoException`.
- Bootstrap 5.3.3, jQuery 3.7.1 y Bootstrap Icons 1.11.3 locales en `webapp/assets`; `sigmud.css` (verde institucional).
- La conexión a MySQL se probó con éxito; el servlet temporal `/prueba-bd` ya fue eliminado.

## Módulo 1: Acceso y seguridad (hecho)

Archivos nuevos:

| Archivo | Función |
|---|---|
| `model/Usuario` | Entidad (tabla `usuario`). El hash solo se llena al buscar y se borra antes de guardarla en sesión. |
| `dao/UsuarioDAO`, `dao/impl/UsuarioDAOImpl` | `buscarPorUsername`, `registrarAccesoExitoso`, `registrarIntentoFallido` (PreparedStatement + try-with-resources). |
| `exception/CredencialesInvalidasException`, `CuentaBloqueadaException` | Errores de negocio del login con mensaje apto para el usuario. |
| `controller/LoginServlet` (`/login`) | GET: formulario. POST: valida, busca, compara hash, cuenta intentos, crea sesión nueva y redirige a `/menu` (PRG). |
| `controller/LogoutServlet` (`/logout`) | Invalida la sesión y vuelve al login. |
| `controller/MenuServlet` (`/menu`) | Calcula qué módulos ve el rol. |
| `filter/AuthFilter` (`/*`) | Solo `/login` y `/assets/*` son públicos; sin sesión → `/login`; rol no permitido → 403; cabeceras anti-caché. Mapa de roles por ruta. |
| `listener/SesionListener` | Cuenta sesiones activas (`applicationScope.sesionesActivas`). |
| `views/login.jsp`, `menu.jsp`, `error.jsp` | Vistas con Bootstrap local y JSTL, sin scriptlets. |
| `assets/js/login.js` | Validación jQuery del formulario. |
| `desplegar.ps1` | Compila, limpia webapps, copia el WAR y arranca Tomcat. |

Decisiones:
- No existe columna "bloqueado": la cuenta se bloquea poniendo `estado = 0` al llegar a 5 `intentos_fallidos` (se hace en un solo `UPDATE`). El administrador la reactiva con `estado = 1, intentos_fallidos = 0`.
- Mensaje único "Usuario o contraseña incorrectos" para usuario inexistente, clave errónea o datos con formato inválido. El aviso de "cuenta bloqueada" solo se muestra si la contraseña es correcta, para no revelar qué usuarios existen. Esto incluye el 5.º intento fallido, que muestra el mensaje genérico.
- Las cuentas inexistentes no suman intentos; se compara contra un hash de relleno para igualar tiempos.
- `login.jsp` y `error.jsp` usan `session="false"` para no crear sesiones anónimas (no inflan el contador).
- `/contribuyentes`, `/almacen`, `/citas` ya tienen roles definidos, pero aún no existen los servlets: un rol permitido recibe 404 y uno no permitido recibe 403.

## Módulo 2, parte 1: capa de datos y validación de Contribuyentes (hecha)

| Archivo | Función |
|---|---|
| `model/Contribuyente` | Entidad con todos los campos de la tabla; constantes `NATURAL` y `JURIDICA`. |
| `dao/ContribuyenteDAO`, `dao/impl/ContribuyenteDAOImpl` | `listar` (activos), `buscarPorId`, `buscarPorDniRuc`, `buscar` (LIKE parametrizado, solo activos), `insertar`, `actualizar`, `darDeBaja`, `reactivar`, `existeDniRuc`. |
| `exception/RegistroDuplicadoException` | Se lanza al violar el UNIQUE de `dni_ruc` (error MySQL 1062). Cualquier otro `SQLException` sale como `DaoException`. |
| `util/ContribuyenteValidador` | `validar(Contribuyente)` devuelve `List<String>` con errores en español. |
| `util/Validador` (ampliado) | RUC ahora exige prefijo 10 o 20; teléfono ahora es solo celular (9 dígitos que empieza con 9); nuevo `vacio()`. |
| `controller/PruebaContribuyentesServlet`, `views/prueba-contribuyentes.jsp` y `.jspf` | TEMPORALES (`/prueba-contribuyentes`); se borrarán en el siguiente paso. |

Decisiones:
- `buscarPorId` y `buscarPorDniRuc` devuelven el registro aunque esté dado de baja (así se detecta un duplicado inactivo y se puede reactivar). `listar` y `buscar` solo devuelven activos.
- `existeDniRuc(dniRuc, idExcluido)` también cuenta registros dados de baja, porque el UNIQUE de la tabla los incluye. Use `0` al registrar uno nuevo.
- Textos opcionales vacíos (teléfono, email, dirección) se guardan como NULL.
- En `buscar`, los caracteres `%` y `_` que escriba el usuario se escapan, así se buscan como texto normal.
- La BD limita el nombre a 150 caracteres y el email a 100; el validador también lo controla.
- `actualizar` no modifica `estado` ni `fecha_registro`.
- Probado fuera de Tomcat contra MySQL: listar (4), buscar "Quispe" (1), `existeDniRuc` y que insertar un DNI repetido lanza `RegistroDuplicadoException` (sin modificar datos).

### Casos de validación

| # | Dato de entrada | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| 1 | NATURAL, DNI `1234567` (7 dígitos) | Error de DNI | El DNI debe tener exactamente 8 dígitos. |
| 2 | JURIDICA, RUC `30123456789` | Error de RUC | El RUC debe tener 11 dígitos y empezar con 10 o 20. |
| 3 | Teléfono `96600000` (8 dígitos) | Error de teléfono | El teléfono debe tener 9 dígitos y empezar con 9. |
| 4 | Email `ana.example.com` | Error de correo | El correo electrónico no es válido. |
| 5 | Fecha de nacimiento dentro de 1 año | Error de fecha futura | La fecha de nacimiento no puede ser futura. |
| 6 | DNI `45678912`, cel. `966123456`, email `ana@example.com`, nacimiento 1990-05-20 | Sin errores | (sin errores) |

## Módulo 2, parte 2: pantallas de Contribuyentes (hecho)

Se eliminaron `PruebaContribuyentesServlet`, `prueba-contribuyentes.jsp` y `prueba-contribuyentes-tabla.jspf`.

| Archivo | Función |
|---|---|
| `controller/ContribuyenteServlet` (`/contribuyentes`) | GET: `listar` (con `q` usa `buscar`), `nuevo`, `editar`. POST: `guardar` (valida, inserta o actualiza, PRG) y `baja`. Mensajes de éxito/error en sesión (flash) que se muestran una sola vez. |
| `views/contribuyentes/listado.jsp` | Tabla, buscador, botón "Nuevo contribuyente", modal de confirmación de baja (POST). |
| `views/contribuyentes/formulario.jsp` | Una sola vista para registrar y editar. Al editar, tipo y DNI/RUC son de solo lectura. |
| `views/fragmentos/barra.jsp` | Barra de navegación compartida (usuario, rol, menú, cerrar sesión). También la usa `menu.jsp`. |
| `assets/js/contribuyentes.js` | jQuery: muestra/oculta la fecha según el tipo, cambia etiqueta y longitud (8 u 11), solo números, validación antes de enviar con `is-invalid`, y llena el modal de baja. |
| `views/menu.jsp` (modificado) | Tarjeta de Contribuyentes habilitada con botón "Abrir" (solo la ven ADMIN y ATENCION). |

Decisiones:
- Al editar, el servidor ignora el tipo y el DNI/RUC del formulario y los toma de la base de datos (no basta con `readonly` en el navegador).
- La validación del servidor corre siempre: si el navegador la omite, los errores vuelven al formulario conservando lo escrito.
- Errores de BD: mensaje genérico en pantalla y detalle en el log de Tomcat (`java.util.logging`, nivel SEVERE).
- `AuthFilter` ya protegía `/contribuyentes` (ADMIN y ATENCION); ALMACEN recibe 403 y sin sesión se redirige a `/login`.
- Si se intenta editar o dar de baja un id que no existe, se vuelve al listado con un mensaje.

### Pruebas manuales

Ejecutar `.\desplegar.ps1` y entrar con `admin` / `Admin123` (o `atencion1` / `almacen1`).

| # | Entrada | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| 1 | Nuevo, NATURAL, DNI `45678912`, "Ana Torres Rojas", cel. `966123456` | Vuelve al listado con "Contribuyente registrado correctamente." (una sola vez) | (pendiente) |
| 2 | Nuevo, JURIDICA, RUC `20123456781`, "Comercial Andina SAC" | Registro exitoso; el campo de fecha no aparece | (pendiente) |
| 3 | Nuevo con DNI `70000001` (ya existe) | Formulario con "Ya existe un contribuyente con ese DNI/RUC" y datos conservados | (pendiente) |
| 4 | Nuevo, NATURAL, DNI de 7 dígitos | Navegador: campo en rojo, no envía. Con JS desactivado: el servidor devuelve el error del DNI | (pendiente) |
| 5 | Editar un contribuyente: cambiar el teléfono | Tipo y DNI de solo lectura; guarda y muestra "actualizado correctamente" | (pendiente) |
| 6 | Buscar `Quispe` | Solo "Maria Quispe Huaman" | (pendiente) |
| 7 | Buscar `70000002` | Solo "Juan Ccasani Flores" | (pendiente) |
| 8 | Dar de baja (confirmar en el modal) | Desaparece del listado; en BD `estado = 0` y la fila sigue existiendo | (pendiente) |
| 9 | Entrar con `almacen1` y abrir `/sigmud/contribuyentes` | 403 "Acceso denegado"; la tarjeta no aparece en su menú | (pendiente) |
| 10 | Sin sesión, abrir `/sigmud/contribuyentes` | Redirige a `/login` | (pendiente) |

## Módulo 3, parte 1: CRUD de materiales del almacén (hecho)

Las entradas y salidas de stock (transacciones) quedan para el siguiente paso.

| Archivo | Función |
|---|---|
| `model/Material` | Entidad. `esCritico()` devuelve true si `stock <= stockMinimo`. |
| `dao/MaterialDAO`, `dao/impl/MaterialDAOImpl` | `listar`, `buscar`, `listarStockCritico`, `buscarPorId`, `buscarPorCodigo`, `insertar`, `actualizar`, `darDeBaja`, `reactivar`. |
| `util/MaterialValidador` | Regex del código `AAA-999`, nombre de 3 a 100, unidad de la lista `UNIDADES`, stock mínimo entero >= 0. |
| `controller/MaterialServlet` (`/almacen`) | Igual que Contribuyentes: `listar` (con `q` y `critico=1`), `nuevo`, `editar`, `guardar` (PRG) y `baja`. Mensajes flash en sesión. |
| `views/almacen/listado.jsp` | Tabla con insignia Crítico/Normal, búsqueda, filtro de stock crítico, modal de baja y columna "Movimientos" con Entrada/Salida deshabilitados ("Próximamente"). |
| `views/almacen/formulario.jsp` | Una vista para registrar y editar. Al editar, código de solo lectura y stock como texto. |
| `assets/js/almacen.js` | jQuery: código en mayúsculas, solo números en stock mínimo, `is-invalid` antes de enviar, modal de baja. |
| `views/menu.jsp` (modificado) | Tarjeta de Almacén habilitada (solo ADMIN y ALMACEN). |

Decisiones:
- **El stock no se puede cambiar a mano.** El `INSERT` fija `stock = 0` en el propio SQL y el `UPDATE` no incluye la columna `stock`. El formulario ni siquiera envía el stock.
- Al editar, el servidor toma el código y el stock de la BD, no del formulario.
- Si el código ya existe (UNIQUE, error 1062) se lanza `RegistroDuplicadoException`; cualquier otro `SQLException` sale como `DaoException` (mensaje genérico en pantalla, detalle en el log).
- El código se valida estricto en el servidor (`mat-006` en minúsculas se rechaza); el navegador lo convierte a mayúsculas al escribir.
- Con `critico=1` y `q` a la vez se aplican los dos filtros.
- `AuthFilter` ya protegía `/almacen` (ADMIN y ALMACEN): ATENCION recibe 403 y sin sesión se redirige a `/login`.
- Probado fuera de Tomcat contra MySQL: listar (5), buscar, crítico (0 materiales críticos hoy), código duplicado y los 4 casos de validación.

### Pruebas manuales

Ejecutar `.\desplegar.ps1`; entrar con `admin` / `Admin123` (o `almacen1`).
Hoy ningún material es crítico (MAT-005: stock 4, mínimo 3). Para el caso 7, edite MAT-005 y ponga stock mínimo 5.

| # | Entrada | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| 1 | Nuevo: `MAT-006`, "Clavos 2 pulgadas", CAJA, stock mínimo 5 | Listado con "Material registrado correctamente. Su stock inicial es 0."; fila con stock 0 y "Crítico" | (pendiente) |
| 2 | Nuevo con código `MAT-001` | Formulario con "Ya existe un material con ese código." y datos conservados | (pendiente) |
| 3 | Nuevo con código `mat1` | Navegador: se escribe `MAT1`, campo en rojo y no envía. Con JS desactivado, el servidor devuelve el error del formato | (pendiente) |
| 4 | Unidad `DOCENA` (solo posible manipulando el formulario con las herramientas del navegador) | El servidor rechaza: "Seleccione una unidad de la lista…" | (pendiente) |
| 5 | Editar MAT-002: cambiar nombre y stock mínimo | Código de solo lectura, stock mostrado como texto (200); al guardar el stock sigue en 200 (verificar en la BD) | (pendiente) |
| 6 | Buscar `papel` | Solo "Papel bond A4 (paquete de 500)" | (pendiente) |
| 7 | "Ver solo stock crítico" (con MAT-005 en mínimo 5) | Solo los materiales con stock <= mínimo, con insignia roja "Crítico" | (pendiente) |
| 8 | Dar de baja MAT-006 (confirmar en el modal) | Desaparece del listado; en BD `estado = 0` y la fila sigue existiendo | (pendiente) |
| 9 | Entrar con `atencion1` y abrir `/sigmud/almacen` | 403 "Acceso denegado"; la tarjeta de Almacén no aparece en su menú | (pendiente) |
| 10 | Sin sesión, abrir `/sigmud/almacen` | Redirige a `/login` | (pendiente) |

## Módulo 3, parte 2: entradas y salidas de stock con transacción, Ajax y JSON (hecho)

| Archivo | Función |
|---|---|
| `model/MovimientoStock` | Entidad de `movimiento_stock` + nombres para mostrar (`nombreMaterial`, `nombreArea`, `nombreUsuario`) y `getFechaFormateada()`. |
| `model/Area`, `dao/AreaDAO`, `dao/impl/AreaDAOImpl` | `listarActivas()`: llena la lista de áreas solicitantes del modal. |
| `exception/StockInsuficienteException` | Mensaje con el stock disponible ("Stock insuficiente: solo hay N unidades disponibles."). |
| `exception/MaterialNoDisponibleException` | El material no existe o está dado de baja. |
| `dao/MovimientoDAO`, `dao/impl/MovimientoDAOImpl` | `registrarMovimiento(...)` (transacción) y `listarPorMaterial(id)` (más recientes primero, con JOIN a material, área y usuario). |
| `controller/MovimientoServlet` (`/almacen/movimiento`) | POST `registrar`: valida y responde JSON con Gson. GET `historial`: página de historial. |
| `views/almacen/historial.jsp` | Tabla de movimientos (ENTRADA verde, SALIDA roja), stock actual del material y botón de volver. |
| `assets/js/movimientos.js` | `$.ajax` POST con `dataType: 'json'`; indicador de carga; actualiza stock e insignia en la fila sin recargar; errores dentro del modal. |
| `views/almacen/listado.jsp` (modificado) | Botones Entrada, Salida e Historial habilitados, modal de movimiento y zona de avisos. |
| `controller/MaterialServlet` (modificado) | Ahora también carga la lista de áreas para el modal. |

### La transacción paso a paso (`MovimientoDAOImpl.registrarMovimiento`)

Una transacción agrupa varias sentencias SQL para que se guarden **todas o ninguna**. Aquí son tres cambios que deben ir juntos: bloquear el material, insertar el movimiento y cambiar el stock.

1. `cn.setAutoCommit(false)`: por defecto JDBC guarda cada sentencia al instante (autocommit). Se desactiva para que nada quede guardado hasta el `commit()`.
2. `SELECT stock FROM material WHERE id_material = ? AND estado = 1 FOR UPDATE`: lee el stock y **bloquea esa fila**. Si otro usuario intenta una salida del mismo material, su transacción espera hasta que esta termine. Sin el bloqueo, dos salidas de 30 con stock 40 leerían ambas "40", ambas pasarían la validación y el stock quedaría en -20 (la BD lo impediría con el CHECK, pero se perdería una operación válida o habría un error confuso). Si no devuelve fila, el material no existe o está de baja: se lanza `MaterialNoDisponibleException`.
3. `if (SALIDA && stock < cantidad) throw new StockInsuficienteException(stock)`: regla de negocio. Se valida con el stock ya bloqueado, así nadie lo cambia entre la lectura y la escritura.
4. `INSERT INTO movimiento_stock ...`: guarda el movimiento (área solo en SALIDA; el usuario viene de la sesión). Si el área no existe, la clave foránea falla aquí.
5. `UPDATE material SET stock = stock + ?` (ENTRADA) o `stock - ?` (SALIDA): cambia el stock. Se usa `stock = stock ± ?` en la BD y no un valor calculado en Java.
6. `cn.commit()`: recién ahora los pasos 4 y 5 se guardan juntos y se libera el bloqueo.
7. `catch (...) { cn.rollback(); throw e; }`: si cualquier paso lanza una excepción (SQL, de negocio o inesperada), `rollback()` **deshace todo** lo hecho dentro de la transacción y libera el bloqueo. Sin rollback podría quedar un movimiento registrado sin su cambio de stock (o al revés), y el stock ya no coincidiría con el historial.
8. `finally { cn.setAutoCommit(true) }`: deja la conexión como estaba, pase lo que pase.

La excepción vuelve a lanzarse para que el servlet decida el mensaje: las de negocio llegan al usuario tal cual; las técnicas (`DaoException`) van al log y el usuario recibe un mensaje genérico.

### Consulta de verificación de consistencia

El stock de cada material debe ser igual a la suma de sus ENTRADAS menos la suma de sus SALIDAS. Lista todos los materiales:

```sql
SELECT m.codigo, m.nombre, m.stock AS stock_actual,
       COALESCE(SUM(CASE mv.tipo WHEN 'ENTRADA' THEN mv.cantidad ELSE -mv.cantidad END), 0) AS stock_calculado
FROM material m
LEFT JOIN movimiento_stock mv ON mv.id_material = m.id_material
GROUP BY m.id_material, m.codigo, m.nombre, m.stock
ORDER BY m.codigo;
```

Para ver solo las inconsistencias (debe devolver 0 filas), añadir antes del `ORDER BY`:

```sql
HAVING m.stock <> COALESCE(SUM(CASE mv.tipo WHEN 'ENTRADA' THEN mv.cantidad ELSE -mv.cantidad END), 0)
```

### Verificación ya realizada (fuera de Tomcat)

Con un material temporal `TST-999` (creado y borrado al terminar; sin tocar los materiales reales) contra MySQL:
ENTRADA 50 → stock 50; SALIDA 20 con área → stock 30; SALIDA 100 → `StockInsuficienteException` ("solo hay 30") y el stock sigue en 30 con 2 movimientos; SALIDA con área inexistente (999) → `DaoException` y el stock sigue en 30 con 2 movimientos (rollback); material inexistente y material de baja → `MaterialNoDisponibleException`; el historial devolvió los nombres de área y usuario. El stock inicial de un material nuevo fue 0.
La consulta de consistencia dio igualdad en los 6 materiales existentes antes de las pruebas de pantalla.

### Pruebas manuales

Ejecutar `.\desplegar.ps1`; entrar con `admin` / `Admin123` y abrir Almacén. Usar un material de prueba (por ejemplo MAT-002, stock 200).
Para las pruebas "a mano" (tipo inválido, material inexistente, área inexistente) abrir las herramientas del navegador (F12), pestaña Consola, y ejecutar el `$.post` indicado.

| # | Entrada | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| 1 | Entrada de 10 a MAT-002 | Aviso verde "Entrada registrada correctamente. Stock actual: 210."; la fila pasa de 200 a 210 sin recargar | (pendiente) |
| 2 | Salida de 15 a MAT-002 con área "Mesa de Partes" | Aviso verde; stock 195 en la fila | (pendiente) |
| 3 | Salida de 1000 a MAT-002 | Mensaje rojo dentro del modal (sin cerrarlo): "Stock insuficiente: solo hay 195 unidades disponibles."; stock y movimientos no cambian (verificar en Historial y con la consulta de consistencia) | (pendiente) |
| 4 | Cantidad `0` (o vacía) | Navegador: campo en rojo, no envía. Negativo no se puede escribir (solo números). Con `$.post` a mano y `cantidad=-5`: JSON `ok:false` "La cantidad debe ser un número entero entre 1 y 100000." | (pendiente) |
| 5 | Salida sin elegir área | Navegador: área en rojo, no envía. Con `$.post` sin `idArea`: `ok:false` "Seleccione el área solicitante." | (pendiente) |
| 6 | `$.post('/sigmud/almacen/movimiento', {accion:'registrar', idMaterial:1, tipo:'AJUSTE', cantidad:5})` | `ok:false` "El tipo de movimiento no es válido."; sin cambios | (pendiente) |
| 7 | Igual con `idMaterial:99999`, `tipo:'ENTRADA'`, `cantidad:5` | `ok:false` "El material no existe o está dado de baja." | (pendiente) |
| 8 | Falla provocada en el segundo paso: `$.post(..., {accion:'registrar', idMaterial:2, tipo:'SALIDA', cantidad:5, idArea:999})` | HTTP 500 con `ok:false` y mensaje genérico; en el log de Tomcat aparece la violación de clave foránea. **Rollback:** el stock de MAT-002 y la cantidad de movimientos en el Historial quedan igual que antes | (pendiente) |
| 9 | Entrar con `atencion1` y abrir `/sigmud/almacen/movimiento?accion=historial&id=1` | 403 "Acceso denegado" | (pendiente) |
| 10 | Sin sesión, abrir `/sigmud/almacen/movimiento?accion=historial&id=1` | Redirige a `/login` | (pendiente) |
| 11 | Abrir Historial de MAT-002 tras las pruebas 1 y 2 | Los movimientos más recientes primero, con área, usuario ("Administrador del Sistema") y badges verde/rojo | (pendiente) |

Notas:
- El usuario que se guarda en cada movimiento sale siempre de la sesión; aunque se envíe `idUsuario` en el formulario, el servidor lo ignora.
- Si la sesión expira, la petición Ajax recibe la página de login en vez de JSON; el navegador muestra "No se pudo completar la operación." y basta con volver a entrar.
- Si una salida deja el stock en el mínimo o por debajo, la insignia de la fila cambia a "Crítico" al instante (el servidor lee el stock mínimo después del commit).

## Módulo 3, parte 3: reportes del almacén (hecho)

| Archivo | Función |
|---|---|
| `model/FilaStockCritico`, `FilaResumenMaterial`, `FilaSalidaArea` | POJO de las filas de los reportes de stock crítico, resumen por material y salidas por área. El detalle de movimientos reutiliza `MovimientoStock`. |
| `model/TablaReporte` | Reporte ya armado como texto (título, columnas, filas y filas de totales). **La misma tabla alimenta la pantalla y el CSV**, así son idénticos. |
| `dao/ReporteAlmacenDAO`, `dao/impl/ReporteAlmacenDAOImpl` | Las 4 consultas. SQL de texto fijo; los valores van siempre como parámetros `?`. |
| `util/FiltrosReporte` | Lee y valida los filtros: formato de fechas, `desde <= hasta`, máximo 366 días, tipo, área y material. Solo valida los filtros que aplican al reporte elegido. |
| `controller/ReporteAlmacenServlet` (`/almacen/reportes`) | GET: página; `accion=csv`: descarga. Totales con Stream (`mapToInt/mapToLong().sum()`, `Collectors.groupingBy`) y orden con `Comparator` según `orden` y `dir`. |
| `views/almacen/reportes.jsp` | Botones de reporte, filtros que cambian según el reporte, tabla con totales, "Exportar CSV" e "Imprimir". |
| `assets/js/reportes.js` | Muestra/oculta filtros, valida fechas antes de enviar, deshabilita "Exportar CSV" sin resultados e imprime. |
| `assets/css/reportes.css` | Estilos de impresión: ocultan la barra, los filtros y los botones. |
| `views/almacen/listado.jsp` (modificado) | Botón "Reportes". |

Decisiones:
- Filtros opcionales: el DAO agrega fragmentos **constantes** (`" AND mv.tipo = ?"`, `" AND mv.id_area = ?"`, `" AND mv.id_material = ?"`) y guarda los valores en una lista que luego se asigna con `setObject`. El texto del usuario nunca se concatena al SQL (probado con `' OR '1'='1` como tipo: 0 resultados).
- El rango usa `fecha >= desde 00:00` y `fecha < (hasta + 1 día) 00:00`, así el día "hasta" entra completo y se puede usar el índice de la columna.
- Periodo por defecto: últimos 30 días. "Stock crítico" no usa fechas ni filtros.
- CSV: `text/csv; charset=UTF-8`, BOM UTF-8 (`﻿`) para que Excel muestre tildes y ñ, separador `;`, `Content-Disposition: attachment` con nombre `reporte-almacen-<reporte>-AAAA-MM-DD.csv` (incluye el reporte para distinguir las descargas). Las celdas con `;`, comillas o saltos de línea van entre comillas. Los textos que empiezan con `= + - @` llevan un apóstrofo delante para que Excel no los ejecute como fórmula (la observación la escriben los usuarios).
- Si hay errores de filtros no se ejecuta la consulta ni se genera el CSV; se muestra el mensaje.
- Los errores técnicos van al log (SEVERE) y el usuario ve un mensaje genérico.
- `AuthFilter` ya protege `/almacen/*` para ADMIN y ALMACEN: ATENCION recibe 403 y sin sesión se redirige a `/login`.

### SQL de cada reporte (explicado)

**a) Stock crítico**
```sql
SELECT m.codigo, m.nombre, m.unidad, m.stock, m.stock_minimo,
       (m.stock_minimo - m.stock) AS faltante          -- columna calculada: cuánto falta para llegar al mínimo
FROM material m
WHERE m.estado = 1                                      -- solo materiales activos (baja lógica)
  AND m.stock <= m.stock_minimo                         -- crítico = stock menor o igual al mínimo
ORDER BY faltante DESC, m.nombre;                       -- primero los que más faltan
```

**b) Movimientos por fecha** (los filtros marcados se agregan solo si el usuario los elige)
```sql
SELECT mv.id_movimiento, mv.tipo, mv.cantidad, mv.fecha, mv.observacion,
       CONCAT(m.codigo, ' - ', m.nombre) AS nombre_material,
       a.nombre AS nombre_area, u.nombres AS nombre_usuario
FROM movimiento_stock mv
JOIN material m ON m.id_material = mv.id_material       -- JOIN: todo movimiento tiene material
LEFT JOIN area a ON a.id_area = mv.id_area              -- LEFT JOIN: las ENTRADAS no tienen área y deben aparecer igual
JOIN usuario u ON u.id_usuario = mv.id_usuario          -- quién lo registró
WHERE mv.fecha >= ? AND mv.fecha < ?                    -- periodo: desde 00:00 hasta (hasta+1) 00:00
  AND mv.tipo = ?                                       -- (opcional) ENTRADA o SALIDA
  AND mv.id_area = ?                                    -- (opcional) área solicitante
  AND mv.id_material = ?                                -- (opcional) un material
ORDER BY mv.fecha DESC, mv.id_movimiento DESC;
```

**c) Resumen por material**
```sql
SELECT m.codigo, m.nombre, m.unidad,
       COALESCE(SUM(CASE WHEN mv.tipo = 'ENTRADA' THEN mv.cantidad ELSE 0 END), 0) AS total_entradas, -- SUM condicional: solo suma las entradas
       COALESCE(SUM(CASE WHEN mv.tipo = 'SALIDA'  THEN mv.cantidad ELSE 0 END), 0) AS total_salidas,  -- solo suma las salidas
       COALESCE(SUM(CASE WHEN mv.tipo = 'ENTRADA' THEN mv.cantidad ELSE -mv.cantidad END), 0) AS saldo, -- entradas - salidas
       COUNT(mv.id_movimiento) AS cantidad_movimientos                                                  -- COUNT: cuántos movimientos hubo
FROM movimiento_stock mv
JOIN material m ON m.id_material = mv.id_material
WHERE mv.fecha >= ? AND mv.fecha < ?
GROUP BY m.id_material, m.codigo, m.nombre, m.unidad    -- una fila por material: SUM y COUNT se calculan dentro de cada grupo
ORDER BY m.nombre;
```
**Verificación incluida en este reporte:** se agregan las columnas "Stock actual" (`m.stock`) y "Verificación". El SQL real suma además `m.stock AS stock_actual` y una subconsulta `saldo_historico` = entradas − salidas de **todos** los movimientos del material (sin filtro de fechas). La columna muestra `OK` si ambos coinciden y `REVISAR` si no; el pie dice "Todo OK" o "N con diferencia". Se compara contra el historial completo (no contra el saldo del periodo) porque solo así debe coincidir con el stock.

**Columnas agregadas en "Salidas por área"** (al final; las 3 originales no cambian y el SQL de este reporte tampoco): "% del total" (unidades del área / total entregado, calculado en Java) y "Verificación". La verificación aparece en la fila TOTAL: compara el total entregado a las áreas con la suma de todas las SALIDAS del periodo según el detalle de movimientos (consulta independiente, la del reporte b con tipo = SALIDA). Muestra `OK` o `REVISAR (movimientos: N)`.

`CASE` convierte cada fila en un número según su tipo (la cantidad o 0) y `SUM` los suma por grupo. `COALESCE` evita NULL. Solo aparecen materiales con movimientos en el periodo (JOIN interno).

**d) Salidas por área**
```sql
SELECT a.nombre AS area,
       SUM(mv.cantidad) AS total_unidades,              -- unidades entregadas a cada área
       COUNT(*) AS cantidad_salidas                     -- cuántas salidas se hicieron
FROM movimiento_stock mv
JOIN area a ON a.id_area = mv.id_area                   -- JOIN interno: las salidas siempre tienen área
WHERE mv.tipo = 'SALIDA' AND mv.fecha >= ? AND mv.fecha < ?
GROUP BY a.id_area, a.nombre                            -- una fila por área
ORDER BY total_unidades DESC, a.nombre;                 -- de mayor a menor
```

### Verificación ya realizada (fuera de Tomcat)

Con un material temporal `TST-998` (stock mínimo 40; entrada 50, salida 20 al área "Logistica y Almacen" y salida 5 a "Rentas y Tributacion"; luego borrado sin tocar los datos reales):
- Stock crítico: 25/40, faltante 15. Movimientos del material: 3 en total, 2 con tipo SALIDA, 1 con el área filtrada, 0 en un rango de días anteriores.
- Resumen: entradas 50, salidas 25, saldo 25, 3 movimientos. Salidas por área: Logistica 20 y Rentas 5 (más lo ya existente).
- Orden por cantidad descendente: 50, 20, 5; totales: entradas 50, salidas 25, saldo 25.
- Filtro de tipo con `' OR '1'='1` → 0 resultados. Fechas: `desde > hasta` rechazado; 367 días rechazado; 366 días aceptado; formato inválido rechazado.
- Escape CSV: `=cmd;prueba` → `'=cmd;prueba`; comillas duplicadas; números negativos sin cambios.

### Pruebas manuales

Ejecutar `.\desplegar.ps1`; entrar con `admin` / `Admin123` → Almacén → "Reportes".
Los números dependen de los movimientos que ya haya hecho; compárelos con las consultas SQL de arriba (o con la consulta de consistencia).
Para tener datos conocidos: registre una entrada de 50 y una salida de 20 (al área "Mesa de Partes") en el material MAT-001 antes de empezar.

| # | Entrada | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| 1 | Reporte "Stock crítico" con datos conocidos: editar un material para que su stock mínimo sea mayor que su stock (p. ej. MAT-005: stock 4, mínimo 10) | Aparece MAT-005 con faltante 6; el pie muestra el total de materiales y la suma de faltantes | (pendiente) |
| 2 | "Movimientos", periodo por defecto (últimos 30 días) | Todos los movimientos recientes, el más nuevo primero; el pie muestra TOTAL ENTRADAS, TOTAL SALIDAS y SALDO DEL PERIODO | (pendiente) |
| 3 | "Movimientos" con tipo = Salida | Solo movimientos SALIDA; TOTAL ENTRADAS = 0 | (pendiente) |
| 4 | "Movimientos" con área = Mesa de Partes | Solo las salidas hechas a esa área (las entradas no tienen área y no aparecen) | (pendiente) |
| 5 | "Resumen por material", periodo por defecto, material MAT-001 | Entradas, salidas y saldo = entradas − salidas; coinciden con lo registrado (p. ej. 40 + 50 entradas, 20 salidas, saldo 70 si el periodo incluye el stock inicial) y con el Historial | (pendiente) |
| 5a | "Resumen por material": columna "Verificación" | Todos los materiales con `OK` y el pie "Todo OK" (con los 5 materiales actuales ya lo dio así al probarlo fuera de Tomcat) | (pendiente) |
| 5b | Ordenar por "Saldo", descendente | Las filas se reordenan de mayor a menor saldo | (pendiente) |
| 6 | "Salidas por área" | Una fila por área con unidades entregadas y N.º de salidas, de mayor a menor; "% del total" suma 100 %; en la fila TOTAL, "Verificación" = OK (con los datos actuales ya dio 39 unidades, 2 salidas, 100,0 % y OK fuera de Tomcat) | (pendiente) |
| 7 | Desde = 2026-11-20 y hasta = 2026-11-01 | Mensaje rojo "La fecha "desde" no puede ser mayor que la fecha "hasta"." (en el navegador antes de enviar; en el servidor si se salta el JS); no se consulta | (pendiente) |
| 8 | Desde = 2025-01-01 y hasta = 2026-11-14 | "El periodo no puede pasar de 366 días." | (pendiente) |
| 9 | "Exportar CSV" con un reporte con datos, abrir el archivo en Excel | Se descarga `reporte-almacen-<reporte>-<fecha>.csv`; Excel separa en columnas y muestra bien tildes y ñ (p. ej. "Área", "Logistica y Almacen"); al final están las filas de totales | (pendiente) |
| 9b | Rango sin movimientos (p. ej. un solo día sin actividad) | "No hay datos para los filtros elegidos."; el botón "Exportar CSV" aparece deshabilitado | (pendiente) |
| 9c | "Imprimir" | Vista de impresión sin barra, filtros ni botones; solo título y tabla | (pendiente) |
| 10 | Entrar con `atencion1` y abrir `/sigmud/almacen/reportes` | 403 "Acceso denegado" | (pendiente) |
| 11 | Sin sesión, abrir `/sigmud/almacen/reportes` | Redirige a `/login` | (pendiente) |

## Módulo 4, parte 1: capa de datos y reglas de negocio de las Citas (hecho)

Las pantallas (servlet y JSP) vienen en el siguiente paso.

| Archivo | Función |
|---|---|
| `model/Cita` | Entidad con los campos de la tabla + campos solo para mostrar (`nombreContribuyente`, `dniRuc`, `nombreArea`, `nombreFuncionario`). Constantes de estado `PROGRAMADA`, `ATENDIDA`, `CANCELADA`. |
| `dao/UsuarioDAO`, `UsuarioDAOImpl` (modificados) | Nuevo `listarFuncionariosPorArea(idArea)`: usuarios activos con rol ATENCION de esa área (sin el hash). |
| `dao/CitaDAO`, `dao/impl/CitaDAOImpl` | `insertar`, `reprogramar`, `cambiarEstado`, `buscarPorId`, `listarPorFecha(fecha, idArea?)`, `listarPorContribuyente`, `existeCruce`, `horasOcupadas`. |
| `exception/CitaCruzadaException` | El funcionario ya tiene una cita en esa fecha y hora (mensaje con fecha y hora). |
| `exception/CitaEstadoException` | La operación no es válida para el estado de la cita. |
| `exception/CitaNoEncontradaException` | La cita no existe. |
| `util/CitaValidador` | `validar(Cita)` → lista de errores en español (contribuyente activo, área y funcionario, fecha, hora, ventanilla, motivo). Reutiliza `ContribuyenteDAO`, `AreaDAO.listarActivas()` y `UsuarioDAO`. |
| `controller/PruebaCitasServlet`, `views/prueba-citas.jsp` | TEMPORALES (`/prueba-citas`): ejecutan los 13 casos y muestran esperado/obtenido. Se borran en el siguiente paso. |

### La regla de doble defensa (por qué hacen falta las dos capas)

Regla: un funcionario no puede tener dos citas **no canceladas** en la misma fecha y hora.

- **Defensa 1: consulta previa `existeCruce`** (antes de insertar o reprogramar). Es la que da la respuesta amable y rápida en el 99 % de los casos ("El funcionario ya tiene una cita el 23/11/2026 a las 10:00. Elija otro horario."). Al reprogramar excluye la propia cita (`id_cita <> ?`), para poder dejar la misma hora y cambiar solo la ventanilla.
- **Defensa 2: el `UNIQUE (id_funcionario, fecha, hora_activa)` de la base** (`uq_cita_funcionario_horario`). La consulta previa NO basta sola porque entre el `SELECT` ("no hay cruce") y el `INSERT` pasa un instante: si dos personas agendan **a la vez**, las dos consultas dicen "libre" y las dos insertarían. La base de datos es la única que ve ambas operaciones con exactitud y rechaza la segunda con el error 1062; el DAO lo captura y lanza la misma `CitaCruzadaException`, así el usuario ve el mismo mensaje.
- **Y el UNIQUE tampoco basta solo**, porque su error es técnico y no distingue casos: la consulta previa permite avisar con claridad y antes de tocar la tabla.
- **Citas canceladas:** la columna generada `hora_activa` vale `NULL` cuando el estado es `CANCELADA`, y en un UNIQUE los `NULL` no se consideran iguales; por eso una cita cancelada libera el horario sin código extra. La consulta previa usa `estado <> 'CANCELADA'` para coincidir con esa regla.

### Reglas de estado

- Solo una cita `PROGRAMADA` puede pasar a `ATENDIDA` o `CANCELADA`, y solo una `PROGRAMADA` se puede reprogramar. En caso contrario: `CitaEstadoException` ("Una cita CANCELADA ya no se puede modificar.").
- `ATENDIDA` solo el mismo día de la cita o después.
- Los `UPDATE` incluyen `AND estado = 'PROGRAMADA'`: si otro usuario cambió el estado justo antes, afectan 0 filas y se lanza `CitaEstadoException` (no se pisa el cambio).
- El validador (`CitaValidador`) no revisa el cruce (lo hace el DAO) ni el estado.
- Reglas de horario del validador: fecha no pasada y a lo sumo 90 días desde hoy; lunes a viernes; hora de 08:00 a 16:30 en intervalos de 30 minutos; si es hoy, la hora no puede haber pasado; ventanilla de 1 a 10; motivo obligatorio de máximo 200 caracteres.
- `reprogramar` en el DAO revisa estado y cruce. La validación de fecha y hora de la nueva cita la hará el servlet con el validador en el siguiente paso.

### Verificación ya realizada (fuera de Tomcat)

Los 13 casos dieron el resultado esperado contra tu MySQL, y al terminar quedaron las 3 citas originales (la prueba borró solo lo que tenía motivo "PRUEBA"). La defensa 2 se probó aparte anulando la consulta previa (como si dos usuarios agendaran a la vez): la base rechazó con `SQLIntegrityConstraintViolationException` y el DAO lanzó `CitaCruzadaException`.
Nota: el caso 12 crea un contribuyente temporal (DNI 99999999, "PRUEBA CITA"), lo da de baja y lo borra al final.

### Casos de prueba

Ejecutar `.\desplegar.ps1`, entrar con cualquier usuario y abrir `/sigmud/prueba-citas`. Las fechas se calculan solas (próximo día hábil a 7 días o más).

| # | Caso | Esperado | Obtenido |
|---|---|---|---|
| 1 | Cita válida | ACEPTADA | ACEPTADA |
| 2 | Segunda cita al MISMO funcionario, fecha y hora | CitaCruzadaException | CitaCruzadaException |
| 3 | Misma hora con OTRO funcionario | ACEPTADA | ACEPTADA |
| 4 | Cancelar la primera y volver a agendar esa hora | ACEPTADA | ACEPTADA |
| 5 | Reprogramar una cita a una hora ocupada | CitaCruzadaException | CitaCruzadaException |
| 6 | Reprogramar sin cambiar la hora (solo la ventanilla) | ACEPTADA | ACEPTADA |
| 7 | Fecha pasada (ayer) | RECHAZADA | RECHAZADA: "La fecha no puede ser anterior a hoy." |
| 8 | Sábado y domingo | RECHAZADA | RECHAZADA: "Solo se atiende de lunes a viernes." |
| 9 | Hora 07:30 | RECHAZADA | RECHAZADA: "La hora debe estar entre 08:00 y 16:30." |
| 10 | Hora 09:15 | RECHAZADA | RECHAZADA: "La hora debe ser en intervalos de 30 minutos…" |
| 11 | Funcionario de otra área | RECHAZADA | RECHAZADA: "El funcionario no pertenece al área seleccionada." |
| 12 | Contribuyente dado de baja | RECHAZADA | RECHAZADA: "El contribuyente está dado de baja." |
| 13 | Pasar una cita CANCELADA a ATENDIDA | CitaEstadoException | CitaEstadoException: "Una cita CANCELADA ya no se puede modificar." |

La columna "Obtenido" es el resultado de ejecutar los casos directamente contra el DAO; al abrir `/prueba-citas` en Tomcat debe mostrar lo mismo, con todas las filas en verde.

## Módulo 4, parte 2: pantallas de Citas, Ajax auxiliar y reporte (hecho)

Se eliminaron `PruebaCitasServlet` y `prueba-citas.jsp`. Con esto los 3 módulos (Contribuyentes, Almacén, Citas) están construidos y ninguna tarjeta del menú dice "Próximamente".

| Archivo | Función |
|---|---|
| `controller/CitaServlet` (`/citas`) | GET `listar` (fecha y área; ATENCION ve su área por defecto, ADMIN todas), `nuevo`, `editar` (reprogramar, solo PROGRAMADA). POST `guardar` (valida, inserta o reprograma, PRG con mensaje flash) y `atender`/`cancelar` (reglas de estado del DAO). |
| `controller/CitaAjaxServlet` (`/citas/ajax`) | JSON con Gson: `contribuyentes` (autocompletado, mínimo 3 caracteres, hasta 10 activos), `funcionarios` (por área) y `horas` (18 turnos del día libres/ocupados; `excluir` = cita que se reprograma). |
| `controller/ReporteCitasServlet` (`/citas/reportes`), `dao/ReporteCitasDAO`, `dao/impl/ReporteCitasDAOImpl`, `model/FilaCitasResumen` | Reporte por área y por funcionario con totales (Stream), orden (Comparator) y CSV. |
| `util/Csv` | Escritura CSV (`;`, UTF-8 con BOM, escape de comillas y de fórmulas de Excel). |
| `views/citas/listado.jsp` | Selector de fecha y área, día anterior/siguiente, tabla por hora con insignias de estado, Atender, Reprogramar y Cancelar (estos dos con modal), "Nueva cita" y "Reportes". |
| `views/citas/formulario.jsp` | Una sola vista para nueva cita y reprogramación (cuadrícula de turnos, autocompletado, cascada área → funcionario). |
| `views/citas/reportes.jsp` | Filtros de fechas y orden, los dos cuadros con fila de totales, "Exportar CSV" e "Imprimir". |
| `assets/js/citas.js` | Modales del listado; autocompletado con espera de 300 ms; cascada de funcionarios y turnos con `$.ajax`; selección del turno; validación previa; validación de fechas del reporte. |
| `views/menu.jsp` (modificado) | Tarjeta de Citas habilitada con botón "Abrir" (ADMIN y ATENCION). |

Decisiones:
- `AuthFilter` ya protege `/citas` y `/citas/*` (incluye `/citas/ajax` y `/citas/reportes`) para ADMIN y ATENCION: ALMACEN recibe 403 y sin sesión se redirige a `/login`.
- **Reprogramar:** el `reprogramar` del DAO solo cambia fecha, hora y ventanilla; por eso en el formulario de edición el contribuyente, el área, el funcionario y el motivo son de solo lectura (el servidor ignora lo que llegue de ellos y los toma de la base de datos).
- El listado manda siempre el parámetro `area` (vacío = todas), así un funcionario de ATENCION puede ver todas las áreas aunque su valor por defecto sea la suya.
- Las respuestas Ajax usan HTTP 200 con `{"ok":false,"mensaje":...}` ante datos inválidos y HTTP 500 (mensaje genérico, detalle al log) ante errores técnicos. Los turnos de una hora ya pasada del día de hoy salen como ocupados (no seleccionables).
- Botón "Atender" deshabilitado si el día mostrado es futuro; el servidor lo valida igual.
- Porcentaje de atención = atendidas ÷ total de citas × 100.
- El reporte reutiliza `FiltrosReporte` (fechas, máximo 366 días, `desde <= hasta`, orden y sentido).

### SQL del reporte (explicado)

```sql
-- Citas por ÁREA
SELECT a.nombre AS nombre,
       COUNT(*) AS total,                                                      -- COUNT(*): todas las citas del grupo
       COUNT(CASE WHEN c.estado = 'PROGRAMADA' THEN 1 END) AS programadas,     -- CASE devuelve 1 solo si cumple; en los demás
       COUNT(CASE WHEN c.estado = 'ATENDIDA'   THEN 1 END) AS atendidas,       -- casos NULL, y COUNT no cuenta los NULL:
       COUNT(CASE WHEN c.estado = 'CANCELADA'  THEN 1 END) AS canceladas       -- así se cuenta por estado en una sola pasada
FROM cita c
JOIN area a ON a.id_area = c.id_area                                           -- JOIN para mostrar el nombre del área
WHERE c.fecha BETWEEN ? AND ?                                                  -- rango (incluye ambos días); fechas como parámetros
GROUP BY a.id_area, a.nombre                                                   -- una fila por área
ORDER BY a.nombre;

-- Citas por FUNCIONARIO (mismos conteos)
SELECT u.nombres AS nombre, COALESCE(a.nombre, '') AS area, COUNT(*) AS total, /* ...los mismos 3 COUNT(CASE...) */
FROM cita c
JOIN usuario u ON u.id_usuario = c.id_funcionario                              -- el funcionario que atiende
LEFT JOIN area a ON a.id_area = u.id_area                                      -- su área (LEFT: el usuario podría no tener área)
WHERE c.fecha BETWEEN ? AND ?
GROUP BY u.id_usuario, u.nombres, a.nombre
ORDER BY u.nombres;
```
El porcentaje de atención se calcula en Java (`atendidas * 100 / total`); la fila TOTAL se calcula con `Stream` (`mapToLong().sum()`).

### Verificación ya realizada

Se simularon las peticiones a los servlets (sin Tomcat) contra tu MySQL, con datos de prueba (motivo "PRUEBA") que se borraron al terminar; quedaron tus 3 citas originales:
- Ajax: autocompletado con "Qu" rechazado ("Escriba al menos 3…") y con "Quispe" devolvió 1 resultado; funcionarios del área 1; turnos del lunes 16/11 (18 turnos, 11:00 ocupado); con `excluir=3` el 11:00 queda libre; domingo y fecha pasada → lista vacía con mensaje; fecha inválida → `ok:false`.
- Citas: agendar válida; mismo funcionario/fecha/hora → vuelve al formulario con el mensaje y conserva lo escrito; otro funcionario a la misma hora → aceptada; reprogramar a turno libre y sin cambiar la hora → aceptadas; cancelar y reusar el turno → aceptado; atender una cancelada → "Una cita CANCELADA ya no se puede modificar."; atender una cita futura → rechazado; editar una cancelada → vuelve al listado con mensaje; ATENCION ve su área por defecto y ADMIN todas.
- Reporte: conteos por área y por funcionario, fila TOTAL, orden por total descendente, fechas invertidas rechazadas y CSV con BOM, `;` y los dos cuadros.

También se desplegó el WAR en una copia aparte de Tomcat (puerto 18080, ya apagada) y se pidieron las páginas con sesión de `admin`: `/citas`, `/citas?accion=nuevo`, `/citas?accion=editar&id=3` (campos de solo lectura y `data-excluir`), `/citas/reportes` (también con fechas invertidas y con más de 366 días), el CSV (`text/csv;charset=UTF-8`, BOM, nombre `reporte-citas-<fecha>.csv`), `/citas/ajax` y el menú. Todas respondieron 200 (las JSP compilan), `/citas` sin sesión redirigió a `/login`, y el menú ya no muestra "Próximamente". No se probó el rol ALMACEN (no se conoce su contraseña) ni el comportamiento de jQuery en el navegador (autocompletado, cuadrícula de turnos, modales).

### Pruebas manuales

Ejecutar `.\desplegar.ps1`; entrar con `admin` / `Admin123` → Citas. Para el caso 1 use una fecha de lunes a viernes de la próxima semana.

| # | Entrada | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| 1 | Nueva cita: contribuyente "Quispe", área Mesa de Partes, funcionario, fecha de la próxima semana, turno 10:00, ventanilla 1, motivo | Vuelve al listado de esa fecha con "Cita registrada correctamente." (una sola vez) y la cita aparece con insignia azul PROGRAMADA | (pendiente) |
| 2 | Otra cita al mismo funcionario, fecha y hora (en la cuadrícula el 10:00 está en gris; también se puede forzar con otra pestaña abierta antes) | Rechazo: "El funcionario ya tiene una cita el dd/mm/aaaa a las 10:00. Elija otro horario."; el formulario conserva lo escrito | (pendiente) |
| 3 | Mismo día y hora con OTRO funcionario (otra área) | Aceptada | (pendiente) |
| 4 | Abrir Nueva cita con el funcionario y fecha de los casos anteriores | Turnos ocupados en gris y deshabilitados; libres en verde y seleccionables | (pendiente) |
| 5 | Reprogramar (modal "Continuar") a un turno libre | "Cita reprogramada correctamente."; contribuyente, área, funcionario y motivo de solo lectura; su turno actual aparece libre (verde) | (pendiente) |
| 6 | Cancelar una cita (modal) y luego agendar otra en ese mismo turno | Cancelada con insignia gris; el turno vuelve a verde y se puede reservar | (pendiente) |
| 7 | "Atender" una cita del día de hoy o de un día pasado | Insignia verde ATENDIDA. (Si el día mostrado es futuro, el botón aparece deshabilitado) | (pendiente) |
| 8 | Intentar atender una cita CANCELADA (por ejemplo, con un formulario manipulado o con el listado desactualizado) | Mensaje rojo "Una cita CANCELADA ya no se puede modificar." En la pantalla normal las canceladas no tienen botones | (pendiente) |
| 9 | Elegir una fecha de sábado o domingo en el formulario | La cuadrícula muestra "Solo se atiende de lunes a viernes."; al enviar, el campo fecha se marca en rojo | (pendiente) |
| 10 | Escribir "Qu" en el buscador de contribuyentes | No consulta; ayuda "Escriba al menos 3 caracteres." Con 3 o más aparecen hasta 10 resultados | (pendiente) |
| 11 | Reporte con datos (últimos 30 días) | Cuadro por área y cuadro por funcionario con total, programadas, atendidas, canceladas, % de atención y fila TOTAL | (pendiente) |
| 12 | Reporte con desde = 2026-11-20 y hasta = 2026-11-01 | Mensaje rojo "La fecha "desde" no puede ser mayor que la fecha "hasta"."; no se consulta | (pendiente) |
| 13 | "Exportar CSV" y abrirlo en Excel | `reporte-citas-<fecha>.csv`; columnas separadas, tildes y ñ correctas, los dos cuadros con su fila TOTAL | (pendiente) |
| 14 | Entrar con `almacen1` y abrir `/sigmud/citas` (y `/sigmud/citas/reportes`) | 403 "Acceso denegado"; la tarjeta de Citas no aparece en su menú | (pendiente) |
| 15 | Sin sesión, abrir `/sigmud/citas` | Redirige a `/login` | (pendiente) |

## Revisión de seguridad, pruebas automáticas y limpieza (hecho)

No se agregaron funciones nuevas. La auditoría completa (comandos y resultados) está en `docs/auditoria.md`.

### Archivos nuevos y cambios

| Archivo | Qué es |
|---|---|
| `filter/SecurityHeadersFilter` | Agrega a toda respuesta: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: same-origin` y una CSP que solo permite recursos del mismo origen (`script-src 'self'`; `style-src 'self' 'unsafe-inline'`; `img-src 'self' data:`; además `default-src`, `font-src`, `frame-ancestors`, `base-uri`, `form-action`). |
| `filter/CsrfFilter` | En todo POST (menos `/login`) exige el token de la sesión en el parámetro `csrf` o en la cabecera `X-CSRF-Token`; si falta o no coincide, 403. Comparación en tiempo constante. |
| `util/Seguridad.tokenAleatorio()` | Token de 32 bytes (`SecureRandom`) en Base64 URL (43 caracteres). `LoginServlet` lo guarda en la sesión al iniciar sesión. |
| `views/fragmentos/csrf.jsp`, `meta-csrf.jsp` | Fragmentos reutilizables: el campo oculto `csrf` (en los 8 formularios POST) y la etiqueta `<meta name="csrf-token">` (en `almacen/listado.jsp` y `citas/formulario.jsp`). |
| `assets/js/movimientos.js`, `citas.js` (modificados) | `$.ajaxSetup` envía `X-CSRF-Token` en todas las llamadas Ajax. |
| `WEB-INF/web.xml` (modificado) | Declara los 3 filtros en orden (cabeceras → sesión y roles → CSRF); cookie de sesión `HttpOnly` + `SameSite=Strict`; `tracking-mode COOKIE`; páginas de error 400, 403, 404, 405, 500 y `Throwable`. |
| `filter/AuthFilter` (modificado) | Se quitó `@WebFilter`: ahora se registra en `web.xml` para fijar el orden. |
| `controller/LoginServlet` (modificado) | Registra en el log cada intento fallido y cada bloqueo (usuario saneado, fecha y hora; nunca la contraseña) y crea el token CSRF. |
| `views/error.jsp` (modificado) | Texto del 403 más general (también lo produce el filtro CSRF). |
| `pom.xml` (modificado) | `junit-jupiter` 5.10.2 (test) y `maven-surefire-plugin` 3.2.5. |
| `sql/03_bd_pruebas.sql` | Crea la base `mun_quinua_test` (mismas 6 tablas y datos de ejemplo) y da permisos a `sigmud`. **Se ejecuta una vez como root** (el usuario `sigmud` no puede crear bases). Nota: `sql/02_tablas_sigmud.sql` no existía en el proyecto, así que el esquema se copió de la base real. |
| `src/test/resources/db.properties` | Apunta a `mun_quinua_test`; en el classpath de pruebas reemplaza al de producción. |
| `src/test/java/.../util/` | `SeguridadTest`, `ValidadorTest`, `ContribuyenteValidadorTest`, `MaterialValidadorTest`, `CitaValidadorTest` (con DAO falsos y reloj fijo), `CsvTest`. Sin base de datos. |
| `src/test/java/.../dao/` | `BaseDaoTest` (salvaguarda), `UsuarioDAOTest`, `ContribuyenteDAOTest`, `MovimientoDAOTest`, `CitaDAOTest`. Con la base de pruebas. |
| `desplegar.ps1` (modificado) | Nuevo parámetro `-Pruebas`: corre `mvn test` antes de desplegar y no despliega si algo falla. Sin el parámetro, empaqueta con `-DskipTests`. |
| `pruebas-seguridad.ps1` | Prueba de seguridad en vivo: tabla APROBADO/FALLIDO de 33 comprobaciones. |
| `tools/LimpiarPruebaSeguridad.java` | Lo usa el script anterior para borrar el contribuyente de prueba (DNI 99999990, solo si su nombre lleva `<script>`). |
| `controller/ReporteAlmacenServlet`, 12 DAO (limpieza) | El servlet usa `util/Csv` (se quitó código duplicado); cada DAO y su implementación llevan un comentario de clase. |

Sobre la limpieza: no quedaban imports sin usar (revisión de todos los `.java`), no hay comentarios obsoletos ni referencias a los servlets temporales eliminados, y cada clase tiene ahora su comentario breve.

### Salvaguardas de las pruebas de DAO
- Usan solo `mun_quinua_test`: `BaseDaoTest` comprueba el nombre de la base conectada **antes** de cada clase y, si no es `mun_quinua_test`, hace fallar todas las pruebas sin tocar nada (probado apuntándolas a otra base: falló con el mensaje "PELIGRO…").
- Si la base de pruebas todavía no existe, las clases de DAO se **omiten** (Maven muestra `Tests run: 0` para ellas) y las pruebas unitarias corren igual.
- Cada prueba crea sus propios datos (DNI `9900000x`, material `TST-901`, citas con motivo `TEST-JUNIT`) y los borra al terminar; las que alteran un usuario seed lo restauran. Tras ejecutar todo, la base de pruebas quedó idéntica al script (mismos conteos y stock).

### Resultados de las pruebas JUnit (206 pruebas)

| Clase | Pruebas | Resultado |
|---|---|---|
| `SeguridadTest` (hash conocido, hashes distintos, comparación, token) | 8 | todas pasaron |
| `ValidadorTest` (usuario, DNI, RUC, teléfono, email) | 55 | todas pasaron |
| `ContribuyenteValidadorTest` | 20 | todas pasaron |
| `MaterialValidadorTest` | 28 | todas pasaron |
| `CitaValidadorTest` (fin de semana, hora, 90 días, funcionario/área, etc.) | 44 | todas pasaron |
| `CsvTest` | 6 | todas pasaron |
| `UsuarioDAOTest` (existente/inexistente, intentos fallidos, bloqueo al 5.º) | 8 | todas pasaron |
| `ContribuyenteDAOTest` (insertar, duplicado, baja lógica, reactivar) | 9 | todas pasaron |
| `MovimientoDAOTest` (entrada, salida, stock insuficiente sin cambios, ROLLBACK forzado) | 12 | todas pasaron |
| `CitaDAOTest` (cruce, cancelar libera, UNIQUE de la base, reprogramar, estados) | 16 | todas pasaron |
| **Total** | **206** | **0 fallos, 0 errores, 0 omitidas** |

Cómo se ejecutaron: las 161 pruebas sin base de datos corren en cualquier equipo. Las 45 de DAO se ejecutaron en una instancia temporal de MySQL 8.4 (otro puerto y carpeta, ya apagada) creada con `sql/03_bd_pruebas.sql`, porque el usuario `sigmud` no puede crear bases en tu MySQL. Por tanto **el script SQL también quedó probado**, pero todavía no se ha ejecutado en tu MySQL: ese paso lo debes hacer tú (ver "Cómo ejecutar las pruebas").

### Resultados del script de seguridad (`pruebas-seguridad.ps1`): 33 APROBADAS, 0 FALLIDAS

Ejecutado contra una copia de Tomcat (puerto 18080) con la versión final, conectada a la base temporal de pruebas (no a tus datos). Hay que volver a ejecutarlo contra tu Tomcat del puerto 8080.

| Grupo | Comprobaciones | Resultado |
|---|---|---|
| Sin sesión: `/menu`, `/contribuyentes`, `/almacen`, `/citas` | 302 hacia `/login` (4) | APROBADO |
| Login con `' OR '1'='1` y `admin' --` como usuario | 401, sin sesión (2) | APROBADO |
| Contraseña incorrecta y usuario inexistente | 401 con el mismo mensaje genérico (2) | APROBADO |
| Cabeceras `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, CSP | presentes con el valor esperado (4) | APROBADO |
| Login de admin y cookie de sesión | 302 a `/menu`; cookie `HttpOnly; SameSite=Strict` (2) | APROBADO |
| CSRF | el formulario trae el token de 43 caracteres; POST sin token → 403; con token falso → 403; con token correcto → 302; Ajax sin cabecera → 403; Ajax con cabecera → 200 (6) | APROBADO |
| XSS | contribuyente `<script>alert(1)</script>` creado y mostrado escapado (`&lt;script&gt;`); texto buscado reflejado escapado (3) | APROBADO |
| Páginas de error | 404 y 405 propias, sin "Apache Tomcat" ni "Exception" (2) | APROBADO |
| Roles | `almacen1` → `/contribuyentes` y `/citas` = 403; `atencion1` → `/almacen` = 403; los módulos propios = 200 (7, incluye los logins) | APROBADO |
| Cierre de sesión | después de `/logout`, `/menu` redirige a `/login` (1) | APROBADO |

Además se abrió el sistema en un navegador real (Chromium sin interfaz) con la CSP activa: Bootstrap, jQuery y los íconos funcionan (modales, validación jQuery, autocompletado y cascada de funcionarios de citas) y no hubo ningún error de CSP en la consola. El único mensaje fue un 404 del `favicon.ico` que el navegador pide a la raíz del servidor.

### Cómo ejecutar las pruebas

1. **Pruebas sin base de datos:** `mvn test` desde NetBeans (clic derecho en el proyecto → *Test*), o `.\desplegar.ps1 -Pruebas`.
2. **Pruebas de DAO (una sola vez):** ejecute `sql/03_bd_pruebas.sql` como root, por ejemplo `mysql -u root -p < sql/03_bd_pruebas.sql`, o ábralo completo en MySQL Workbench. Después `mvn test` ejecuta las 206 pruebas.
3. **Seguridad en vivo:** con Tomcat encendido (`.\desplegar.ps1`), en otra ventana de PowerShell: `.\pruebas-seguridad.ps1`. Usa las contraseñas de ejemplo (`Admin123`, `Atencion123`, `Almacen123`); si las cambió, páselas con `-ClaveAdmin`, `-ClaveAtencion` y `-ClaveAlmacen`.

## Paneles (dashboards) por rol: ADMIN, ATENCION y ALMACEN (hecho)

No se cambió la lógica de los módulos. `/menu` ahora muestra el panel del rol de la sesión: arriba las tarjetas de acceso a los módulos (la misma lógica de roles que ya tenía el menú) y debajo los indicadores, gráficos y listas. `MenuServlet` y `menu.jsp` se eliminaron (los reemplazan `DashboardServlet` y las 3 vistas).

### Archivos nuevos y cambios

| Archivo | Qué es |
|---|---|
| `assets/js/chart.umd.js` | Chart.js 4.4.3 minificado, instalado con npm y copiado solo ese archivo (sin mapa de código; sin CDN). |
| `dao/DashboardDAO`, `dao/impl/DashboardDAOImpl` | Consultas de agregación (COUNT, SUM, GROUP BY) con `PreparedStatement`; "hoy" y "mes" salen del reloj de la aplicación. Métodos por actor: ver el SQL más abajo. |
| `model/EtiquetaValor`, `model/PuntoDiario` | Puntos de los gráficos (etiqueta + valor; entradas y salidas por día). |
| `util/PanelDatos` | Arma, por rol, `{kpis, graficos, listas}` como mapas de texto y números. Lo usan la página y el JSON, así coinciden. Un rol solo puede obtener su propio panel (rol desconocido → error). |
| `controller/DashboardServlet` (`/menu`) | Elige la vista según el **rol de la sesión** (`dashboard/admin.jsp`, `atencion.jsp` o `almacen.jsp`) y carga indicadores y listas. Si la base falla, igual muestra la página con un aviso. |
| `controller/DashboardDatosServlet` (`/dashboard/datos`) | JSON con Gson (UTF-8) para los gráficos, indicadores y listas. **No lee ningún parámetro**: el rol y el id salen de la sesión. |
| `controller/DashboardDesbloquearServlet` (`/dashboard/desbloquear`) | POST (con token CSRF) que desbloquea una cuenta; solo ADMIN (se verifica en `AuthFilter` y de nuevo en el servlet); registra en el log quién desbloqueó a quién. |
| `dao/UsuarioDAO`, `UsuarioDAOImpl` (modificados) | `buscarPorId` y `desbloquear(id)`: pone `intentos_fallidos = 0` y `estado = 1` **solo si la cuenta tiene 5 o más intentos** (no reactiva una cuenta desactivada a mano). |
| `filter/AuthFilter` (modificado) | Rutas nuevas: `/dashboard/datos` (los 3 roles) y `/dashboard/desbloquear` (solo ADMIN). Sin sesión, `/dashboard/*` responde **401** con JSON (no redirige al login). |
| `WEB-INF/views/dashboard/admin.jsp`, `atencion.jsp`, `almacen.jsp` | Las 3 vistas (Bootstrap, JSTL, `<c:out>`, sin scriptlets ni scripts en el HTML). |
| `WEB-INF/tags/kpi.tag` | Tarjeta de indicador reutilizable (ícono, número grande, etiqueta; rojo si es de alerta y vale más de 0). |
| `WEB-INF/views/fragmentos/tarjetas-modulos.jsp` | Las tarjetas de módulos (lo que tenía `menu.jsp`). |
| `assets/js/dashboard.js` | Pide `/dashboard/datos` al cargar y **cada 60 s**; dibuja con Chart.js, actualiza indicadores y listas sin recargar, muestra la hora de la última actualización, el aviso discreto si falla (conserva los datos anteriores) y "No hay datos para mostrar". Maneja el botón "Desbloquear". |
| `assets/css/sigmud.css` (ampliado) | Estilos de las tarjetas de indicadores y de los gráficos (no cambia el diseño del resto). |
| `WEB-INF/web.xml` (modificado) | Página de error también para el 401. |
| `pruebas-seguridad.ps1` (ampliado) | 13 comprobaciones nuevas de los paneles (total: 46). |
| `src/test/.../DashboardDAOTest`, `PanelDatosTest`, `UsuarioDAOTest` (ampliado) | 24 pruebas nuevas sobre `mun_quinua_test`. |

Decisiones:
- **Rol e id siempre de la sesión.** Ninguna de las dos rutas JSON lee parámetros: `/dashboard/datos?rol=ADMIN&id=1&funcionario=3` devuelve exactamente lo mismo que sin parámetros.
- **Gráficos y KPI en vivo:** la página trae los valores ya calculados (no queda vacía) y `dashboard.js` los refresca; las listas se construyen con `.text()` (nunca con HTML) para evitar XSS.
- **Accesibilidad:** cada `<canvas>` tiene `role="img"` y un `aria-label` (y un `<figcaption>` oculto) que se reescribe con los datos actuales, por ejemplo "Citas por estado de los últimos 30 días. Programadas: 2; Atendidas: 3; Canceladas: 1."
- **Colores:** verde institucional (`#1b5e20`) con dorado para las salidas, azul/verde/gris para los estados de cita; en rojo solo las alertas.
- **Materiales para reponer:** muestra los 5 críticos (`stock <= stock mínimo`, igual que el indicador "en stock crítico") con mayor faltante; un material justo en el mínimo aparece con faltante 0.
- **ATENCION:** no hay dato de "contribuyentes que registré hoy" (la tabla no guarda el usuario), por eso se omitió.
- El botón "Atender" de la lista de próximas citas envía a `/citas` la acción que ya existe (`accion=atender`), con el token CSRF; "Registrar entrada" lleva a `/almacen?q=<código>` (el listado filtrado por ese material).
- Con la pestaña oculta el panel no consulta; al volver a mostrarla se actualiza de inmediato.

### SQL de cada indicador (explicado)

**ADMIN**
```sql
-- Totales: 6 subconsultas COUNT en una sola consulta (un viaje a la base)
SELECT (SELECT COUNT(*) FROM usuario WHERE estado = 1)                                   AS usuarios_activos,
       (SELECT COUNT(*) FROM contribuyente WHERE estado = 1)                             AS contribuyentes_activos,
       (SELECT COUNT(*) FROM material WHERE estado = 1)                                  AS materiales_activos,
       (SELECT COUNT(*) FROM material WHERE estado = 1 AND stock <= stock_minimo)        AS materiales_criticos,   -- stock crítico
       (SELECT COUNT(*) FROM cita WHERE fecha = ?)                                       AS citas_hoy,             -- ? = hoy
       (SELECT COUNT(*) FROM movimiento_stock WHERE fecha >= ? AND fecha < ?)            AS movimientos_hoy;       -- [hoy 00:00, mañana 00:00)

-- Citas por estado (dona), últimos 30 días: una fila por estado con su COUNT; Java completa con 0 los estados sin citas
SELECT estado, COUNT(*) AS total FROM cita WHERE fecha BETWEEN ? AND ? GROUP BY estado;

-- Movimientos por día (barras), últimos 14 días: SUM con CASE separa entradas y salidas en un mismo GROUP BY por día; Java completa con 0 los días sin datos
SELECT DATE(fecha) AS dia,
       COALESCE(SUM(CASE WHEN tipo = 'ENTRADA' THEN cantidad ELSE 0 END), 0) AS entradas,
       COALESCE(SUM(CASE WHEN tipo = 'SALIDA'  THEN cantidad ELSE 0 END), 0) AS salidas
FROM movimiento_stock WHERE fecha >= ? AND fecha < ? GROUP BY DATE(fecha);

-- Contribuyentes registrados por mes (línea), últimos 6 meses: se agrupa por AAAA-MM
SELECT DATE_FORMAT(fecha_registro, '%Y-%m') AS mes, COUNT(*) AS total
FROM contribuyente WHERE fecha_registro >= ? GROUP BY DATE_FORMAT(fecha_registro, '%Y-%m');

-- Cuentas bloqueadas: 5 o más intentos fallidos (no se consulta el hash de la contraseña)
SELECT id_usuario, username, nombres, rol, id_area, estado, intentos_fallidos, ultimo_acceso
FROM usuario WHERE intentos_fallidos >= ? ORDER BY nombres;          -- ? = 5

-- Últimos accesos: los 5 con ultimo_acceso más reciente
SELECT ... FROM usuario WHERE ultimo_acceso IS NOT NULL ORDER BY ultimo_acceso DESC LIMIT 5;
```

**ATENCION** (el `?` del funcionario es SIEMPRE el id del usuario de la sesión)
```sql
-- Mis citas de hoy: COUNT(CASE WHEN ... THEN 1 END) cuenta solo las filas que cumplen la condición
SELECT COUNT(*) AS total,
       COUNT(CASE WHEN estado = 'PROGRAMADA' THEN 1 END) AS programadas,
       COUNT(CASE WHEN estado = 'ATENDIDA'   THEN 1 END) AS atendidas,
       COUNT(CASE WHEN estado = 'CANCELADA'  THEN 1 END) AS canceladas
FROM cita WHERE id_funcionario = ? AND fecha = ?;

-- Mis próximas citas de hoy: PROGRAMADAS desde la hora actual, máximo 5 (JOIN para mostrar nombres)
SELECT c.*, ct.nombres_razon_social, ct.dni_ruc, a.nombre, u.nombres
FROM cita c JOIN contribuyente ct ON ct.id_contribuyente = c.id_contribuyente
            JOIN area a ON a.id_area = c.id_area JOIN usuario u ON u.id_usuario = c.id_funcionario
WHERE c.id_funcionario = ? AND c.fecha = ? AND c.estado = 'PROGRAMADA' AND c.hora >= ?
ORDER BY c.hora LIMIT 5;

-- Mis citas atendidas por día (barras), últimos 7 días; Java completa con 0 los días sin citas
SELECT fecha, COUNT(*) AS total FROM cita
WHERE id_funcionario = ? AND estado = 'ATENDIDA' AND fecha BETWEEN ? AND ? GROUP BY fecha;
```

**ALMACEN**
```sql
-- Totales: materiales activos, críticos, movimientos de hoy y SUM de entradas / salidas del mes
SELECT (SELECT COUNT(*) FROM material WHERE estado = 1)                                   AS materiales_activos,
       (SELECT COUNT(*) FROM material WHERE estado = 1 AND stock <= stock_minimo)         AS materiales_criticos,
       (SELECT COUNT(*) FROM movimiento_stock WHERE fecha >= ? AND fecha < ?)             AS movimientos_hoy,
       (SELECT COALESCE(SUM(cantidad), 0) FROM movimiento_stock WHERE tipo = 'ENTRADA' AND fecha >= ? AND fecha < ?) AS entradas_mes,  -- [1.º del mes, 1.º del mes siguiente)
       (SELECT COALESCE(SUM(cantidad), 0) FROM movimiento_stock WHERE tipo = 'SALIDA'  AND fecha >= ? AND fecha < ?) AS salidas_mes;

-- Los 5 materiales con mayor faltante (stock_minimo - stock), solo activos y críticos
SELECT codigo, nombre, unidad, stock, stock_minimo, (stock_minimo - stock) AS faltante
FROM material WHERE estado = 1 AND stock <= stock_minimo ORDER BY faltante DESC, nombre LIMIT 5;

-- Salidas por área del mes (barras horizontales): SUM y GROUP BY por área, de mayor a menor
SELECT a.nombre AS area, SUM(mv.cantidad) AS total
FROM movimiento_stock mv JOIN area a ON a.id_area = mv.id_area
WHERE mv.tipo = 'SALIDA' AND mv.fecha >= ? AND mv.fecha < ?
GROUP BY a.id_area, a.nombre ORDER BY total DESC, a.nombre;

-- Últimos movimientos (con nombres de material, área y usuario)
SELECT mv.*, CONCAT(m.codigo, ' - ', m.nombre), a.nombre, u.nombres
FROM movimiento_stock mv JOIN material m ON m.id_material = mv.id_material
     LEFT JOIN area a ON a.id_area = mv.id_area JOIN usuario u ON u.id_usuario = mv.id_usuario
ORDER BY mv.fecha DESC, mv.id_movimiento DESC LIMIT ?;           -- ? = 5
```

### Verificación realizada

- **JUnit: 230 pruebas, 0 fallos** (161 sin base de datos + 69 de DAO, de las cuales 24 son nuevas: `DashboardDAOTest` 15, `PanelDatosTest` 5 y 4 nuevas en `UsuarioDAOTest`). Las pruebas de DAO se ejecutaron en una instancia temporal de MySQL (ya apagada) creada con `sql/03_bd_pruebas.sql`. Los totales se comparan contra consultas SQL directas y contra el "antes y después" de insertar datos conocidos (incluidos casos que NO deben contarse: baja, mes pasado, otro funcionario, otro estado). Las pruebas de ATENCION comprueban que las citas de otro funcionario no suman.
- **Script de seguridad: 46 APROBADAS, 0 FALLIDAS** (13 nuevas), contra una copia de Tomcat (puerto 18080) conectada a la base temporal.
- **Navegador real (Chromium sin interfaz) con la CSP activa**, con datos de demostración **solo en la base temporal**: los 3 paneles se dibujan (dona, barras, línea, barras horizontales), los indicadores de alerta salen en rojo, los gráficos y listas se actualizan solos cada 60 s (19:54:54 → 19:55:54), al bloquear la comunicación aparece "No se pudo actualizar; se muestran los últimos datos" y se conservan los datos, y el botón "Desbloquear" desbloqueó a `atencion2` (la lista quedó vacía, el indicador pasó de 1 a 0 y salió de rojo) y quedó en el log: `Cuenta DESBLOQUEADA: usuario=atencion2 (id=3) por administrador=admin (id=1)`. No hubo errores de CSP en la consola.

### Tabla de pruebas por actor (qué DEBE ver y qué NO)

Abrir `http://localhost:8080/sigmud/` con cada usuario. Para ver los gráficos con datos conviene haber registrado antes algunas citas y movimientos.

| Rol (usuario) | DEBE ver | NO debe ver ni poder hacer | Resultado obtenido |
|---|---|---|---|
| ADMIN (`admin`) | Panel "Panel del administrador"; tarjetas de Contribuyentes, Almacén y Citas; 7 indicadores (usuarios, contribuyentes, materiales, stock crítico, citas de hoy, movimientos de hoy, cuentas bloqueadas); dona de citas, barras de movimientos, línea de contribuyentes; listas de cuentas bloqueadas (con "Desbloquear") y últimos accesos | — | Comprobado en navegador y con `pruebas-seguridad.ps1` |
| ADMIN | Pulsar "Desbloquear" en una cuenta bloqueada: confirma, la cuenta se desbloquea, la lista y el indicador se actualizan, queda en el log | Desbloquear una cuenta que no está bloqueada ("no está bloqueada") ni una inexistente | Comprobado en navegador y con pruebas JUnit |
| ATENCION (`atencion1`) | "Mi panel de atención"; tarjetas de Contribuyentes y Citas; sus 4 indicadores de citas de hoy; sus próximas citas (hasta 5) con "Atender"; gráfico de sus citas atendidas (7 días); accesos rápidos "Nueva cita" y "Nuevo contribuyente" | Datos de ADMIN (usuarios, cuentas bloqueadas, accesos), de ALMACEN, la tarjeta de Almacén, citas de otro funcionario; `/dashboard/desbloquear` (403); `/almacen` (403) | Comprobado en navegador y con `pruebas-seguridad.ps1` / JUnit |
| ATENCION | `/dashboard/datos?rol=ADMIN&id=1&funcionario=3` → exactamente lo mismo que sin parámetros | Datos de ADMIN o de `atencion2` cambiando parámetros | Comprobado con `pruebas-seguridad.ps1` |
| ALMACEN (`almacen1`) | "Panel del almacén"; solo la tarjeta de Almacén; 5 indicadores (materiales, stock crítico, movimientos de hoy, entradas y salidas del mes); los 5 materiales con mayor faltante con "Registrar entrada"; barras horizontales de salidas por área; últimos 5 movimientos; accesos rápidos "Nuevo material" y "Reportes" | Datos de ADMIN o de ATENCION (citas, cuentas, accesos); tarjetas de Contribuyentes y Citas; `/dashboard/desbloquear` (403); `/contribuyentes` y `/citas` (403) | Comprobado en navegador y con `pruebas-seguridad.ps1` |
| Sin sesión | — | `/dashboard/datos` y `/dashboard/desbloquear` → 401; `/menu` → redirige a `/login` | Comprobado con `pruebas-seguridad.ps1` |
| Cualquiera | POST a `/dashboard/desbloquear` sin token CSRF → 403 (también para ADMIN) | — | Comprobado con `pruebas-seguridad.ps1` |

## Gestión de usuarios (solo ADMIN), hash con sal y "Mi perfil" (hecho)

No se cambió la lógica de los módulos. Detalle de seguridad en `docs/auditoria.md` (sección 6).

### ORDEN DE EJECUCIÓN (importante)

1. **Primero la base de datos, ANTES de desplegar la versión nueva** (la aplicación nueva lee la columna `debe_cambiar_password` y guarda hashes de más de 64 caracteres; sin estos cambios el inicio de sesión falla):
   - **Su base actual `mun_quinua`:** en MySQL Workbench, como root, ejecute `sql/04_usuarios.sql` (la **sección 1**, `USE mun_quinua`). Es seguro repetirlo: si la columna ya existe no hace nada. Conserva los usuarios y sus contraseñas.
   - **Base de pruebas (opcional, para JUnit):** ejecute `sql/03_bd_pruebas.sql` (versión nueva; borra y recrea SOLO `mun_quinua_test` y ya incluye los cambios). Si ya la tenía creada con el 03 anterior, puede ejecutar la **sección 2** de `04_usuarios.sql` en vez de recrearla.
   - **Instalación nueva desde cero:** use solo `sql/02_tablas_sigmud.sql` (crea `mun_quinua` completa y ya con los cambios; nunca junto con el 04).
   > El 04 ejecuta las dos secciones seguidas: si todavía no creó `mun_quinua_test`, la sección 2 falla al hacer `USE mun_quinua_test`, pero la sección 1 ya quedó aplicada. Puede ignorar ese error o ejecutar el 04 seleccionando solo la sección 1.
2. **Después** `.\desplegar.ps1` (opcionalmente `-Pruebas`), y `.\pruebas-seguridad.ps1`.

### Archivos nuevos y cambios

| Archivo | Qué es |
|---|---|
| `sql/04_usuarios.sql` | Dos secciones (`mun_quinua` y `mun_quinua_test`, con `USE`): agrega `debe_cambiar_password TINYINT(1) NOT NULL DEFAULT 0` y amplía `password_hash` a `VARCHAR(255)`. Idempotente. |
| `sql/02_tablas_sigmud.sql` (nuevo; **no existía**) y `sql/03_bd_pruebas.sql` (modificado) | Instalación nueva y base de pruebas ya con los cambios. El 02 se generó a partir del esquema real. |
| `CLAUDE.md` (nuevo; **no existía**) | Reglas del proyecto tomadas del skill de SIGMUD, con la regla 9 actualizada (PBKDF2) y las reglas de seguridad web. |
| `util/Seguridad` (reescrita) | `hashear` (PBKDF2-HMAC-SHA256, 120000 iteraciones, sal de 16 bytes, formato `pbkdf2$it$sal$hash`), `verificar` (acepta ambos formatos, tiempo constante, nunca lanza excepción), `esLegado`, `generarPasswordTemporal` (12 caracteres) y `tokenAleatorio`. Se eliminó `hashCoincide`. |
| `util/Validador` (ampliado) | `politicaPassword(password, username)` con mensajes en español; el nombre de usuario ahora es de **3** a 30 caracteres (antes 4). |
| `util/UsuarioValidador` | Valida lo que escribe el ADMIN: usuario, nombre, rol y área obligatoria para ATENCION. |
| `exception/UltimoAdminException` | "Siempre debe quedar al menos un administrador activo." |
| `model/Usuario` (ampliado) | `debeCambiarPassword`, `nombreArea`, `isBloqueada()` y `getUltimoAccesoTexto()`. |
| `dao/UsuarioDAO`, `UsuarioDAOImpl` (reescritos) | `listar(texto, rol, activo)`, `buscarPorId`, `existeUsername`, `contarAdminsActivos`, `insertar`, `actualizar` (nombres, rol, área), `cambiarEstado`, `actualizarPassword` y `desbloquear`. La regla del último ADMIN va en una transacción con `FOR UPDATE`. |
| `controller/UsuarioServlet` (`/usuarios`) | Listado con filtros, nuevo, editar, guardar (PRG) y estado (con modal). Genera la contraseña temporal y la muestra una sola vez. Verifica las reglas en el servidor. |
| `controller/UsuarioAjaxServlet` (`/usuarios/restablecer`, `/usuarios/desbloquear`) | Ajax (POST + token CSRF + JSON). Restablecer devuelve `{"ok":true,"passwordTemporal":"..."}`. |
| `controller/PerfilServlet` (`/perfil`, `/perfil/password`) | Datos del usuario y cambio de contraseña (política, actual correcta, distinta de la actual, `changeSessionId`). |
| `controller/LoginServlet` (modificado) | Usa `Seguridad.verificar` y migra automáticamente los hashes antiguos al formato nuevo. |
| `filter/AuthFilter` (modificado) | `/usuarios` solo ADMIN; con `debeCambiarPassword` todas las rutas (salvo `/perfil/password`, `/logout` y `/assets`) redirigen a `/perfil/password` con un mensaje. |
| `views/usuarios/listado.jsp`, `formulario.jsp`; `views/perfil/perfil.jsp`, `password.jsp` | Vistas (Bootstrap, JSTL, `<c:out>`, sin scriptlets): insignias de rol y estado, "Bloqueada", "Clave temporal", último acceso, cuadro con "Copiar", modales, medidor de fortaleza. |
| `assets/js/usuarios.js`, `password.js` | Modales, restablecer y desbloquear con `$.ajax`, copiar al portapapeles, validación y medidor de fortaleza (todo en archivos externos, compatible con la CSP). |
| `fragmentos/barra.jsp`, `fragmentos/tarjetas-modulos.jsp`, `DashboardServlet` (modificados) | El nombre de la barra enlaza a `/perfil`; tarjeta "Usuarios" en el panel del ADMIN. |
| `assets/js/login.js`, `login.jsp` | Patrón de usuario de 3 a 30 caracteres. |
| `pruebas-seguridad.ps1`, `tools/LimpiarPruebaSeguridad.java` (ampliados) | 41 comprobaciones nuevas (total: 87); la limpieza también borra el usuario de prueba `segpru###`. |
| Pruebas JUnit nuevas/ampliadas | `SeguridadTest` (30), `PoliticaPasswordTest` (17), `UsuarioValidadorTest` (8), `ValidadorTest`, `UsuarioDAOTest` (29). |

### Decisiones y comportamientos que debe conocer
- **Migración automática:** los usuarios de ejemplo siguen entrando con las mismas contraseñas; al primer inicio de sesión su hash pasa a PBKDF2 (comprobado: `admin`, `atencion1` y `almacen1` migraron; `atencion2`, que no entró, conserva el antiguo hasta que entre).
- **"Último acceso" en Mi perfil** es el acceso **anterior** a la sesión actual (el de esta sesión se muestra como "Inicio de esta sesión").
- **Restablecer / desbloquear:** restablecer no está disponible para la propia cuenta (use "Mi perfil"); `desbloquear` solo actúa sobre cuentas con 5 o más intentos y las reactiva; el botón "Desbloquear" del panel del ADMIN sigue funcionando.
- **Activar** una cuenta también reinicia sus intentos fallidos (así una cuenta bloqueada no queda a un fallo de bloquearse otra vez).
- Se **corrigió un detalle del medidor** (el campo seguía en rojo tras corregir la contraseña).
- **Limitaciones** (sesiones abiertas de un usuario desactivado, la contraseña temporal pasa por la memoria de la sesión) en `docs/auditoria.md`, sección 6.

### Verificación realizada
- **JUnit: 296 pruebas, 0 fallos** (210 sin base de datos + 86 de DAO; 66 nuevas o ampliadas en esta etapa). Las de DAO se ejecutaron en una instancia temporal de MySQL (ya apagada) creada con `sql/03_bd_pruebas.sql`. Incluyen: hash nuevo verifica, dos hashes de la misma contraseña son distintos, hash antiguo SHA-256 verifica, contraseña incorrecta falla y formatos inválidos no lanzan excepción; política de contraseña; y en `UsuarioDAO`: insertar, duplicado, cambiar estado, desbloquear, listar con filtros y las reglas del último ADMIN.
- **Scripts SQL probados:** `03` (nuevo) crea la base de pruebas con las dos columnas; `04` convirtió una base con el esquema antiguo (conservó los hashes) y es idempotente (segunda ejecución sin cambios); `02` creó una `mun_quinua` completa (6 tablas, 4 usuarios) y la aplicación arrancó y funcionó sobre ella.
- **`pruebas-seguridad.ps1`: 87 APROBADAS, 0 FALLIDAS** (41 nuevas): ATENCION y ALMACEN reciben 403 en `/usuarios` y no pueden crear usuarios ni restablecer claves (ni con token válido); POST sin token → 403; contraseña temporal de 12 caracteres mostrada una sola vez; nombre repetido y ATENCION sin área rechazados; el ADMIN no puede desactivarse ni cambiarse el rol; restablecer clave; cambio obligatorio (`/menu`, `/almacen`, `/perfil` y `/dashboard/datos` redirigen, `/assets` no); contraseña débil, igual al usuario, actual incorrecta, confirmación distinta y nueva igual a la actual rechazadas; cambio correcto con `JSESSIONID` nuevo; la temporal ya no sirve; un usuario desactivado no entra. El script borró el usuario de prueba al terminar.
- **Navegador real (Chromium sin interfaz):** crear un usuario desde el formulario (el área se marca en rojo si falta), cuadro con la contraseña temporal y botón "Copiar" (pasa a "Copiado"), que desaparece al recargar; restablecer con modal; desactivar/activar con modal; las filas del propio ADMIN tienen deshabilitados "Desactivar" y "Restablecer"; filtros; el usuario nuevo es llevado a cambiar su contraseña, el medidor pasa de "Débil" a "Muy fuerte", y tras cambiarla entra a su perfil y `/usuarios` le da "Acceso denegado".

### Tabla de pruebas manuales

Entrar con `admin` / `Admin123` salvo que se indique otra cosa. Anote el resultado en la última columna.

| # | Entrada | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| 1 | Ejecutar `sql/04_usuarios.sql` (sección 1) en Workbench | Sin errores; `SHOW COLUMNS FROM usuario` muestra `debe_cambiar_password` y `password_hash varchar(255)` | (pendiente) |
| 2 | Desplegar e iniciar sesión con `admin` / `Admin123`; luego `SELECT password_hash FROM usuario WHERE username='admin'` | Entra normalmente; el hash ahora empieza con `pbkdf2$120000$` (migración automática) | (pendiente) |
| 3 | Menú → tarjeta "Usuarios" → listado | Ve los 4 usuarios con insignias de rol y estado, área y último acceso; la fila propia dice "usted" y sus botones "Desactivar" y "Restablecer clave" están deshabilitados | (pendiente) |
| 4 | Filtrar por texto "atencion", por rol ALMACEN y por estado Inactivos | La lista se reduce según los filtros (se combinan) | (pendiente) |
| 5 | Nuevo usuario: usuario `prueba01`, nombre, rol ATENCION **sin área** → Crear | El campo Área se marca en rojo y no se envía; en el servidor también se rechaza | (pendiente) |
| 6 | Mismo formulario con área "Mesa de Partes" | Vuelve al listado con "Usuario creado" y un cuadro amarillo con la contraseña temporal (12 caracteres) y el botón "Copiar" | (pendiente) |
| 7 | Pulsar "Copiar" y pegar en el bloc de notas; luego recargar la página | Se copia la contraseña; tras recargar el cuadro **no vuelve a aparecer** | (pendiente) |
| 8 | Crear otro usuario con el nombre `prueba01` | Error "Ya existe un usuario con ese nombre de usuario"; lo escrito se conserva | (pendiente) |
| 9 | Cerrar sesión; entrar como `prueba01` con la contraseña temporal | Entra, pero cualquier página (menú, citas, perfil) lo lleva a "Cambiar contraseña" con el aviso de contraseña temporal | (pendiente) |
| 10 | En esa pantalla escribir la nueva contraseña `abc`; luego `abcdefgh`; luego una buena | El medidor sube de "Débil" a "Fuerte" y la lista de requisitos se marca; con `abc` o `abcdefgh` el formulario no se envía | (pendiente) |
| 11 | Nueva contraseña igual al nombre de usuario (`prueba01`); contraseña actual equivocada; confirmación distinta | Cada caso se rechaza con un mensaje claro (la actual equivocada cuenta como intento fallido) | (pendiente) |
| 12 | Cambio correcto (por ejemplo `Definitiva2026x`) | Mensaje de éxito y pasa a "Mi perfil"; ya puede usar el sistema; la contraseña temporal ya no sirve | (pendiente) |
| 13 | Como `prueba01` abrir `/sigmud/usuarios` | "Acceso denegado" (403) | (pendiente) |
| 14 | Como `admin`: "Restablecer clave" de `prueba01` | Confirma y aparece un modal con la contraseña temporal nueva y "Copiar"; al cerrar el modal desaparece; `prueba01` vuelve a tener "Clave temporal" | (pendiente) |
| 15 | Bloquear una cuenta: 5 contraseñas incorrectas seguidas con `prueba01`; luego, como `admin`, mirar la lista y el panel | La fila dice "Bloqueada"; el panel del ADMIN muestra 1 cuenta bloqueada (en rojo) | (pendiente) |
| 16 | Pulsar "Desbloquear" en la lista de usuarios (y repetir con el botón del panel) | Mensaje "desbloqueada"; la cuenta vuelve a "Activo" y `prueba01` puede entrar | (pendiente) |
| 17 | "Desactivar" a `prueba01` (modal) y luego intentar entrar con ese usuario | Queda "Inactivo"; no puede iniciar sesión; "Activar" lo devuelve a la normalidad | (pendiente) |
| 18 | Como `admin`: intentar desactivarse a sí mismo (botón deshabilitado; también probar enviando el formulario manipulado) | Rechazado con "No puede desactivar su propia cuenta" | (pendiente) |
| 19 | Editar la propia cuenta del `admin`: el rol aparece bloqueado; intentar cambiarlo manipulando el formulario | Rechazado con "No puede cambiar su propio rol" | (pendiente) |
| 20 | Dejar a `prueba01` como ADMIN activo, y con `admin` desactivar al otro ADMIN; luego intentar desactivar al último ADMIN | El último ADMIN no se puede desactivar ni cambiar de rol: "Siempre debe quedar al menos un administrador activo" | (pendiente) |
| 21 | Clic en el nombre del usuario en la barra | Abre "Mi perfil" con nombre, usuario, rol, área, último acceso anterior e inicio de la sesión actual | (pendiente) |
| 22 | `.\pruebas-seguridad.ps1` | 87 APROBADAS | (pendiente) |

## Auditoría de cambios (solo ADMIN, solo lectura) (hecho)

Registra **quién hizo qué, cuándo y desde qué IP** en todos los módulos. Los registros no se pueden editar ni borrar
(ni desde la aplicación ni con SQL directo: lo impiden triggers de la base de datos).

### ORDEN DE EJECUCIÓN (importante)

1. **Base real (`mun_quinua`) ya instalada:** ejecutar `sql/05_auditoria.sql` **sección 1** **como `root`** (los triggers
   exigen privilegio TRIGGER/SUPER que el usuario `sigmud` no tiene) **antes** de desplegar la versión nueva.
   - Workbench: abrir `sql/05_auditoria.sql`, ejecutar con el rayo (el script ya incluye `USE mun_quinua;`).
   - Consola: `mysql -u root -p --default-character-set=utf8mb4 < sql/05_auditoria.sql`
   - Es idempotente: se puede ejecutar más de una vez (`CREATE TABLE IF NOT EXISTS` y triggers recreados).
2. **Instalación nueva:** `sql/02_tablas_sigmud.sql` ya incluye la tabla y los triggers (no hace falta el 05).
3. **Base de pruebas:** `sql/03_bd_pruebas.sql` (recrea `mun_quinua_test`; ya incluye la tabla). Si solo quiere agregar
   la tabla a una base de pruebas ya existente, ejecute la **sección 2** de `sql/05_auditoria.sql`.
4. Desplegar (`.\desplegar.ps1`). Si la tabla no existe, la aplicación **sigue funcionando** (la auditoría falla en
   silencio y queda en el log), pero `/auditoria` mostrará un error y no se guardará nada.

### Archivos nuevos y cambios

| Archivo | Qué hace |
|---|---|
| `sql/05_auditoria.sql` | Sección 1 (`mun_quinua`) y sección 2 (`mun_quinua_test`): tabla `auditoria` (fecha con milisegundos, usuario, acción, entidad, id, descripción, detalle, IP; índices por fecha, acción y usuario) y triggers `BEFORE UPDATE`/`BEFORE DELETE` que lanzan `SQLSTATE 45000` "La auditoría no se puede modificar ni eliminar". |
| `sql/02_...sql`, `sql/03_...sql` | Incluyen la tabla y los triggers (ahora 7 tablas). |
| `model/RegistroAuditoria`, `model/FiltrosAuditoria` | Fila de la tabla (con texto de fecha y clase de color de la insignia) y filtros de búsqueda. |
| `dao/AuditoriaDAO` + `dao/impl/AuditoriaDAOImpl` | `registrar`, `registrar(Connection, …)` (para usar dentro de una transacción), `buscar` (filtros + `LIMIT/OFFSET` parametrizados), `contar`, `recientes(n)`. SQL fijo, valores siempre como parámetros, `%` y `_` escapados en los `LIKE`. |
| `util/Auditoria` | Punto único para registrar: toma el usuario de la **sesión** y la IP, limpia saltos de línea/caracteres de control, recorta a cada columna, **nunca lanza excepción** y `diferencias(antes, después)` arma `campo: a -> b; …` omitiendo campos sensibles (password, hash, clave, token, secret). |
| `dao/MovimientoDAO(+Impl)` | `registrarMovimiento(…, ip)`: la auditoría se guarda **dentro de la misma transacción**. El método de 6 argumentos sigue existiendo. |
| `controller/AuditoriaServlet` (`/auditoria`) | Solo ADMIN (`AuthFilter`). Filtros, paginación de 20, exportación CSV (`?formato=csv`, `;`, UTF-8 con BOM, máx. 10000 filas). Solo `doGet`: no hay forma de modificar nada. |
| `WEB-INF/views/auditoria/listado.jsp`, `assets/js/auditoria.js` | Pantalla: filtros, tabla con insignias de color, modal de detalle (clic o Enter en la fila), paginación, botón CSV. Todo con `<c:out>` y `.text()`; JavaScript en archivo externo (CSP). |
| `filter/AuthFilter` | Ruta `/auditoria` solo para ADMIN. |
| `util/PanelDatos`, `assets/js/dashboard.js`, `views/dashboard/admin.jsp` | Panel ADMIN: indicador **"Intentos de acceso fallidos en las últimas 24 horas"** (rojo si > 0) y tarjeta **"Actividad reciente"** (últimos 8 eventos, se actualiza con el Ajax del panel). |
| `DashboardServlet`, `fragmentos/tarjetas-modulos.jsp` | Tarjeta "Auditoría" en el menú del ADMIN. |
| `LoginServlet`, `LogoutServlet`, `ContribuyenteServlet`, `MaterialServlet`, `CitaServlet`, `UsuarioServlet`, `UsuarioAjaxServlet`, `DashboardDesbloquearServlet`, `PerfilServlet`, `MovimientoServlet` | Llaman a `Auditoria.registrar(...)` **después** de que la operación tuvo éxito. No cambia su lógica de negocio (solo se hicieron que algunos métodos auxiliares devolvieran el estado "antes" para poder calcular las diferencias). |
| `AuditoriaDAOTest`, `AuditoriaTest`, `PanelDatosTest`, `MovimientoDAOTest` | Pruebas nuevas/ampliadas (ver abajo). |
| `pruebas-seguridad.ps1` | Sección 8d nueva (acceso por rol, solo lectura, login fallido visible, saneamiento de saltos de línea y `<script>`). |

### Eventos auditados

| Evento | Módulo | Qué se guarda (nunca contraseñas ni hashes) |
|---|---|---|
| `LOGIN_OK` | Sesión | usuario, IP |
| `LOGIN_FALLIDO` | Sesión | usuario **escrito** (saneado), IP, motivo (`contraseña incorrecta`, `usuario inexistente`, `datos con formato inválido`…); id de la cuenta si existe |
| `CUENTA_BLOQUEADA` | Sesión | usuario y cuenta bloqueada al llegar a 5 intentos |
| `LOGOUT` | Sesión | usuario, IP |
| `CREAR` | Contribuyentes / Materiales | id y nombre/código |
| `ACTUALIZAR` | Contribuyentes / Materiales | diferencias campo por campo (`campo: antes -> después`) |
| `BAJA` | Contribuyentes / Materiales | id y nombre/código |
| `REACTIVAR` | Contribuyentes / Materiales | **definido pero todavía no ocurre**: ninguna pantalla llama a `reactivar` (no se agregó funcionalidad nueva). Quedará activo cuando exista la pantalla. |
| `MOVIMIENTO_STOCK` | Almacén | tipo, cantidad, stock antes → después, material, área, observación (**dentro de la transacción**) |
| `CITA_CREAR` | Citas | contribuyente, área, funcionario, ventanilla, motivo |
| `CITA_REPROGRAMAR` | Citas | diferencias (fecha, hora, ventanilla…) |
| `CITA_ATENDER` / `CITA_CANCELAR` | Citas | cambio de estado |
| `USUARIO_CREAR` | Usuarios | nombre, rol, área (solo dice que se asignó contraseña temporal; **no** cuál) |
| `USUARIO_ACTUALIZAR` | Usuarios | diferencias |
| `USUARIO_ACTIVAR` / `USUARIO_DESACTIVAR` | Usuarios | cambio de estado |
| `PASSWORD_RESTABLECIDA` | Usuarios | a qué usuario; "debe cambiarla" (**sin** la temporal) |
| `PASSWORD_CAMBIADA` | Perfil | quién la cambió (**sin** la contraseña) |
| `USUARIO_DESBLOQUEADO` | Usuarios / Panel | usuario desbloqueado e intentos anteriores |

### Por qué `MOVIMIENTO_STOCK` va dentro de la transacción y los demás son "mejor esfuerzo"

- **Movimiento de stock = dentro de la transacción** (`registrarMovimiento` inserta la auditoría con la misma
  `Connection`, antes del `commit`). Si falla la auditoría, se hace `rollback` de todo: el stock cambia **si y solo si**
  queda constancia. *Ventaja:* inventario y auditoría nunca se desincronizan (es el dato más delicado: son bienes
  municipales). *Desventaja:* si la tabla de auditoría falla, el usuario no puede registrar movimientos hasta arreglarlo.
- **Los demás eventos = "mejor esfuerzo"** (`Auditoria.registrar` atrapa cualquier error, lo escribe en el log del
  servidor y deja seguir). *Ventaja:* una falla de la auditoría nunca le impide al usuario trabajar ni le muestra un
  error confuso. *Desventaja:* si la auditoría falla en ese momento, **se puede perder un registro** (la operación sí
  se hizo). Por eso se registra después del éxito y se deja el detalle en el log.

### Decisiones y limitaciones

- La IP sale de `request.getRemoteAddr()`: detrás de un proxy inverso mostraría la IP del proxy (no se usa
  `X-Forwarded-For` porque cualquiera podría falsearla). En pruebas locales aparece como `0:0:0:0:0:0:0:1` (IPv6 de localhost).
- `id_usuario` es clave foránea con `ON DELETE SET NULL`: si alguien borra un usuario por SQL directo, la auditoría **no se
  pierde** y conserva el `username` escrito. Por eso la limpieza de `pruebas-seguridad.ps1` puede borrar su usuario de prueba.
- Las filas de auditoría creadas por `pruebas-seguridad.ps1` en la base real **quedan para siempre** (a propósito: no se
  pueden borrar). Se reconocen por los usuarios `segpru###`/`segaud###`. Para limpiar una base de pruebas se usa
  `TRUNCATE TABLE auditoria` (TRUNCATE no dispara el trigger de DELETE; requiere privilegio DROP, que `sigmud` tiene sobre `mun_quinua_test`).
  **`root` también podría hacerlo en la base real**: la inmutabilidad protege de la aplicación y de errores, no de un DBA malintencionado.
- Un texto largo se recorta (descripción 255, detalle 4000). Los saltos de línea del texto del usuario se convierten en espacio.

### Verificación realizada

- `sql/02` (instalación nueva), `sql/03`, `sql/05` (sobre una base sin la tabla y ejecutado dos veces) probados en un
  MySQL temporal: triggers activos (`UPDATE`/`DELETE` fallan con el mensaje esperado), `TRUNCATE` permitido.
- JUnit: **335 pruebas, 0 fallos** (antes 296).
- `pruebas-seguridad.ps1`: **106 APROBADAS, 0 FALLIDAS** (antes 87).
- Navegador (capturas revisadas): panel ADMIN con indicador rojo y actividad reciente, listado, modal, filtro por acción,
  validación de fechas en el navegador, sin errores de JavaScript.

### Pruebas automáticas nuevas

| Prueba | Qué comprueba |
|---|---|
| `AuditoriaDAOTest` (18) | registrar todos los campos y con nulos; filtros por usuario, acción, entidad, texto, fechas y momento; filtros combinados; `%` como texto; inyección SQL; paginación de 20 y conteo; `recientes`; `UPDATE` y `DELETE` directos **fallan** por los triggers; registrar dentro de una transacción con `rollback` **no deja rastro** y con `commit` sí; `ON DELETE SET NULL` conserva el nombre. |
| `AuditoriaTest` (17, sin BD) | quita saltos de línea/control, recorta a cada columna, conserva HTML (se escapa al mostrar); `diferencias`: solo lo que cambió, nulo = vacío, **nunca** incluye password/hash/clave/token, recorta valores largos, sin saltos de línea. |
| `MovimientoDAOTest` (+2) | un movimiento exitoso deja un registro `MOVIMIENTO_STOCK` con la IP; si el movimiento falla (stock insuficiente o clave foránea) **no queda auditoría**: se deshacen juntos. |
| `PanelDatosTest` (+2) | el ADMIN recibe `loginFallidos` (cuenta solo las últimas 24 h) y `actividad` (máx. 8, la más nueva primero); ATENCION no recibe ninguno de los dos. |

### Tabla de pruebas manuales (probadas con navegador y script)

| Prueba | Resultado esperado | Obtenido |
|---|---|---|
| `atencion1` o `almacen1` abren `/auditoria` o `/auditoria?formato=csv` | 403 | 403 (aprobado) |
| Sin sesión abre `/auditoria` | Redirige a `/login` | aprobado |
| `admin` abre `/auditoria` | Tabla con los últimos 7 días, 20 por página | aprobado |
| `admin` hace `POST /auditoria` (con token) | 405: solo lectura | 405 |
| Periodo mayor de 366 días (servidor y navegador) | Mensaje de error, sin consultar | aprobado |
| `' OR '1'='1` y `'; DROP TABLE auditoria; --` en los filtros | 0 resultados, tabla intacta | aprobado |
| Login fallido con usuario nuevo | Aparece `LOGIN_FALLIDO` en `/auditoria` y la contraseña escrita no aparece en ningún lado | aprobado |
| Usuario con salto de línea y `<script>` en el login | Se guarda en una sola línea y se muestra escapado (`&lt;script&gt;`) | aprobado |
| Clic en una fila | Modal con el detalle campo por campo | aprobado |
| Exportar CSV | Archivo `auditoria-AAAA-MM-DD.csv`, `;`, UTF-8 con BOM, con los mismos filtros | aprobado |
| Panel ADMIN | Indicador rojo de accesos fallidos (si > 0) y "Actividad reciente" de 8 eventos | aprobado |
| Panel ATENCION / ALMACEN | No reciben ni indicador ni actividad | aprobado |
| Intentar `UPDATE`/`DELETE` en la tabla desde SQL | Error "La auditoría no se puede modificar ni eliminar" | aprobado |

### Cómo probar a mano

1. Ejecute `sql/05_auditoria.sql` (sección 1) como root y despliegue (`.\desplegar.ps1`).
2. Entre como `admin`, haga algo (por ejemplo editar un contribuyente) y abra **Auditoría** desde el menú: verá el evento con el detalle `campo: antes -> después`.
3. Cierre sesión, escriba una clave incorrecta con cualquier usuario y vuelva a entrar como `admin`: el panel mostrará el indicador rojo.
4. `.\pruebas-seguridad.ps1` y `.\desplegar.ps1 -Pruebas` (o `mvn test`) para las pruebas automáticas.

## Ficha del contribuyente, reactivación y citas desde la ficha (hecho)

Una pantalla que une los datos de un contribuyente con su historial de citas. **No hay SQL nuevo** (no cambió ninguna
tabla): no hace falta ejecutar ningún script; solo desplegar (`.\desplegar.ps1`). Los módulos existentes no cambiaron su lógica.

### Cómo se usa
- `Contribuyentes` → clic en el **nombre** (o botón **Ficha**) → `GET /contribuyentes?accion=ficha&id=N`.
- Se puede abrir también la ficha de un contribuyente **dado de baja** (insignia roja; Editar y Nueva cita quedan deshabilitados con una explicación; aparece **Reactivar**).
- El interruptor **"Ver también dados de baja"** del listado muestra los inactivos (por defecto el listado sigue mostrando solo activos).
- **Nueva cita** abre `/citas?accion=nuevo&contribuyente=ID` con el contribuyente ya elegido y bloqueado.
- **Imprimir ficha** imprime solo la pestaña activa con el encabezado "Municipalidad Distrital de Quinua - Ficha del contribuyente", la fecha/hora de impresión y el usuario que imprime (sin escudo ni logotipo).

### Archivos nuevos y cambios

| Archivo | Qué hace |
|---|---|
| `model/ResumenCitas` (nuevo) | POJO con total, programadas, atendidas y canceladas. |
| `dao/CitaDAO` + `CitaDAOImpl` | `resumenPorContribuyente(id)` (una consulta con `COUNT` y `SUM(CASE…)`), `proximaCita(id)` (la próxima PROGRAMADA desde hoy, o `null`) y `listarPorContribuyente(id, estado)` (filtro opcional por estado, de la más reciente a la más antigua, con JOIN a área y funcionario). El método de un solo argumento sigue igual. |
| `model/FiltrosAuditoria`, `dao/AuditoriaDAO` + `AuditoriaDAOImpl` | Filtro por `idEntidad` (junto con `entidad`) y `ultimosPorEntidad(entidad, id, n)`. |
| `util/Auditoria` | Nueva acción `CONSULTA_FICHA` (también en el filtro de `/auditoria`). |
| `dao/ContribuyenteDAO` + `Impl` | `listarConInactivos()` y `buscarConInactivos(texto)`. `listar()` y `buscar()` no cambian (solo activos). |
| `controller/ContribuyenteServlet` | `GET accion=ficha`, `POST accion=reactivar` (usa el `reactivar` que ya existía), parámetro `inactivos=1` en el listado y `volver=ficha` para regresar a la ficha después de dar de baja o reactivar (Post-Redirect-Get). |
| `WEB-INF/views/contribuyentes/ficha.jsp` (nuevo) | Encabezado, datos personales (con edad solo en personas naturales), indicadores, historial con filtro por estado, pestaña **Actividad** (solo ADMIN), modales de baja/reactivar y botones. |
| `WEB-INF/views/contribuyentes/listado.jsp`, `assets/js/contribuyentes.js` | Interruptor de inactivos, nombre como enlace a la ficha, insignia "Dado de baja", botón Reactivar con modal, impresión (`beforeprint` escribe la fecha y hora reales). |
| `assets/css/impresion.css` (nuevo) | Solo `@media print`: oculta barra, botones, filtros, modales y pestañas inactivas; muestra el encabezado de impresión. Sin imágenes. |
| `controller/CitaServlet`, `views/citas/formulario.jsp` | `accion=nuevo&contribuyente=ID`: si existe y está activo, viene elegido y bloqueado; si no, se ignora y sale el formulario normal. |
| `model/Cita`, `model/Contribuyente` | Solo getters de texto para mostrar fechas (`dd/MM/yyyy`). Sin lógica de negocio. |
| `FichaContribuyenteDAOTest` (nuevo), `pruebas-seguridad.ps1` | Pruebas (ver abajo). |

### Reglas de la ficha (todas verificadas en el servidor)
- Solo ADMIN y ATENCION (`AuthFilter`); ALMACEN recibe 403; sin sesión → login.
- `id` no numérico, vacío, `0`, negativo, enorme o con texto (`abc`, `1 OR 1=1`) o inexistente → vuelve al listado con un mensaje rojo; **nunca error 500 ni datos**.
- La pestaña **Actividad** solo se genera para el ADMIN: el rol sale de la **sesión**; para ATENCION el HTML ni siquiera contiene la pestaña ni los eventos, y los parámetros `tab=actividad`, `rol=ADMIN`, etc. se ignoran.
- Un `estado` de filtro desconocido se ignora (muestra todos).
- Todo texto del contribuyente se imprime con `<c:out>` (un nombre con `<script>` se ve como texto).
- **Cada apertura de una ficha registra `CONSULTA_FICHA`** (ver la nota de protección de datos en `docs/auditoria.md`).
- `REACTIVAR` (que antes estaba definido pero sin pantalla) ahora se registra al reactivar desde la ficha o el listado.

### SQL de los indicadores (explicado)

```sql
-- Total y conteo por estado en UNA sola lectura de la tabla (en vez de 4 consultas)
SELECT COUNT(*) AS total,                                                        -- todas las citas del contribuyente
       COALESCE(SUM(CASE WHEN estado = 'PROGRAMADA' THEN 1 ELSE 0 END), 0) AS programadas,   -- suma 1 por cada fila programada
       COALESCE(SUM(CASE WHEN estado = 'ATENDIDA'   THEN 1 ELSE 0 END), 0) AS atendidas,
       COALESCE(SUM(CASE WHEN estado = 'CANCELADA'  THEN 1 ELSE 0 END), 0) AS canceladas
FROM cita
WHERE id_contribuyente = ?;                      -- SUM devuelve NULL si no hay filas: COALESCE lo convierte en 0
```

```sql
-- Próxima cita: PROGRAMADA, de hoy en adelante (si es hoy, solo si la hora no pasó), la más cercana
SELECT ... FROM cita c JOIN contribuyente ct ... JOIN area a ... JOIN usuario u ...
WHERE c.id_contribuyente = ? AND c.estado = 'PROGRAMADA'
  AND (c.fecha > ? OR (c.fecha = ? AND c.hora >= ?))      -- ?=hoy, ?=hoy, ?=hora actual
ORDER BY c.fecha, c.hora, c.id_cita
LIMIT 1;
```

```sql
-- Historial: filtro de estado opcional (fragmento fijo " AND c.estado = ?" solo si se eligió un estado)
SELECT ... WHERE c.id_contribuyente = ? [AND c.estado = ?] ORDER BY c.fecha DESC, c.hora DESC, c.id_cita DESC;
```

Nota: una cita PROGRAMADA con fecha pasada (no atendida ni cancelada) cuenta como "programada" en el indicador pero **no** como "próxima cita": es lo esperado (la "próxima" mira hacia adelante).

### Verificación realizada
- JUnit: **350 pruebas, 0 fallos** (antes 335; 15 nuevas en `FichaContribuyenteDAOTest`).
- `pruebas-seguridad.ps1`: **141 APROBADAS, 0 FALLIDAS** (antes 106).
- Navegador (capturas revisadas): ficha con datos e indicadores, filtro por estado, pestaña Actividad, modal de baja, ficha de dado de baja (botones deshabilitados + Reactivar), vista de impresión (barra y botones ocultos, encabezado con usuario y fecha), listado con inactivos, reactivar y "Nueva cita" con contribuyente bloqueado. Sin errores de JavaScript.

### Pruebas automáticas nuevas (`FichaContribuyenteDAOTest`)

| Prueba | Qué comprueba |
|---|---|
| `elResumenCuentaCadaEstado` | 6 citas sembradas → total 6 = 3 programadas + 2 atendidas + 1 cancelada |
| `elResumenSinCitasEsCeroYNoNulo`, `elResumenNoMezclaCitasDeOtroContribuyente` | ceros sin citas; no suma citas ajenas |
| `laProximaCitaEsLaProgramadaMasCercanaDesdeHoy` | ignora la cancelada más cercana y la programada vencida |
| `sinCitasProgramadasLaProximaEsNull` | `null` si solo hay atendidas, canceladas o vencidas |
| `sinFiltroListaTodas…`, `elFiltroPorEstado…`, `unEstadoConInyeccionSql…`, `elMetodoAnterior…` | orden descendente, JOIN con área/funcionario, filtro por estado, inyección SQL sin efecto, compatibilidad |
| `elFiltroPorEntidadEIdSoloDevuelveEseContribuyente`, `ultimosPorEntidad…` | filtro entidad + id; máximo 10, el más nuevo primero |
| `consultaFichaEsUnaAccionValidaYSeGuarda` | la acción existe y se guarda |
| `listarConInactivos…`, `buscarConInactivos…`, `reactivar…` | los inactivos solo aparecen con el método nuevo; el listado normal no cambia |

### Tabla de pruebas manuales

| Prueba | Resultado esperado | Obtenido |
|---|---|---|
| `almacen1` abre la ficha / intenta reactivar (con token) | 403 | 403 |
| Sin sesión abre la ficha | Redirige a `/login` | aprobado |
| `abc`, `1 OR 1=1`, `999999`, `-5`, vacío, `0`, número gigante, sin id | 302 al listado con mensaje, sin 500 ni datos | aprobado |
| `atencion1` abre la ficha | 200, **sin** pestaña Actividad | aprobado |
| `atencion1` pide `&tab=actividad&rol=ADMIN` | Se ignora: 200 sin Actividad ni eventos | aprobado |
| `admin` abre la ficha | 200 con pestaña Actividad (últimos 10 eventos) | aprobado |
| Contribuyente con nombre `<script>alert(1)</script>` | Se ve como texto (`&lt;script&gt;`) | aprobado |
| Filtro `estado=X' OR '1'='1` | Se ignora (200) | aprobado |
| Baja desde la ficha sin token / con token | 403 / vuelve a la ficha con mensaje | aprobado |
| Ficha de un dado de baja | 200, insignia roja, Editar y Nueva cita deshabilitados, botón Reactivar | aprobado |
| Listado normal / con "Ver también dados de baja" | Sin el inactivo / con insignia y botón Reactivar | aprobado |
| Reactivar sin token / con token | 403 / vuelve a la ficha sin la insignia | aprobado |
| `CONSULTA_FICHA` y `REACTIVAR` en `/auditoria` | Aparecen los eventos | aprobado |
| Nueva cita desde la ficha | Contribuyente elegido y bloqueado | aprobado |
| `/citas?accion=nuevo&contribuyente=abc` (o inexistente, o dado de baja) | Se ignora; formulario normal con buscador | aprobado |
| Imprimir ficha (vista de impresión) | Sin barra ni botones; encabezado con fecha y usuario | aprobado |

### Cómo probarlo a mano
1. `.\desplegar.ps1`, entre como `admin` → Contribuyentes → clic en un nombre.
2. Pruebe el filtro por estado, la pestaña **Actividad**, **Dar de baja** (modal) → **Reactivar**, y el interruptor "Ver también dados de baja".
3. **Nueva cita** desde la ficha; **Imprimir ficha** (Ctrl+P muestra la vista de impresión).
4. Entre como `atencion1`: la ficha funciona pero **no** aparece "Actividad". Entre como `almacen1`: 403.
5. `.\pruebas-seguridad.ps1` y `mvn test`.

## Paquete visual: logo, favicon, paginación, orden, avisos (toasts), modo oscuro y accesibilidad (hecho)

**No hay SQL nuevo** ni cambió la lógica de negocio: solo desplegar (`.\desplegar.ps1`). La política de seguridad (CSP)
no cambió: **ningún JavaScript en el HTML** (todo en `assets/js/*.js`), sin CDN ni fuentes externas.

### 1. Logo y favicon
- `listener/ContextoListener` (`@WebListener`): al iniciar comprueba con `getResource` si existe `/assets/img/logo.png` y deja
  `logoDisponible` (y `faviconPng` si además existe `assets/img/favicon.png`) en el contexto de la aplicación.
- **Con logo:** se usa en el login (grande, sobre el formulario), en la barra (pequeño, junto a "SIGMUD") y en el encabezado de
  impresión de la ficha. **Sin logo:** ícono de Bootstrap Icons `bi-building`. No se dibujó ningún escudo.
- **Cómo poner el logo oficial:** copie el archivo a `src/main/webapp/assets/img/logo.png` (y, opcional, `favicon.png`), vuelva a
  desplegar y reinicie (el listener lee el archivo al arrancar). Todos los `<img>` llevan texto alternativo.
- `assets/img/favicon.svg`: favicon sencillo (letras SIGMUD en verde institucional) que se usa si no hay `favicon.png`.
- `fragmentos/cabecera.jsp`: fragmento común de `<head>` (favicon, `theme-color`, `tema.js` y las hojas de estilo). Está incluido
  en **todas** las páginas; cada página sigue teniendo su `<title>` descriptivo (por ejemplo "SIGMUD - Ficha del contribuyente").

### 2. Paginación y orden por columna (del lado del servidor)
| Archivo | Qué hace |
|---|---|
| `model/Pagina<T>` | items, paginaActual, tamano, totalRegistros, totalPaginas, desde/hasta. Normaliza: tamaño solo 10/20/50 (otro = 10); página mínima 1 y máxima la última. |
| `util/OrdenSql` | **Lista blanca de orden**: `resolver(lista, columnaPedida, dirPedida, porDefecto, dirPorDefecto, desempate)`. Solo una columna que esté en el `Map` se traduce a su fragmento fijo de SQL; la dirección se escribe como literal `ASC`/`DESC`. |
| `util/Paginador` | Cuenta, corrige la página y trae solo esa página con `LIMIT ? OFFSET ?` **como parámetros**. |
| `util/ListaVista` | Lee `pagina`, `tamano`, `orden`, `dir` del pedido y deja en la petición lo que usan los fragmentos. |
| DAO (5) | `ContribuyenteDAO.listarPagina`, `MaterialDAO.listarPagina`, `UsuarioDAO.listarPagina`, `MovimientoDAO.listarPorMaterialPagina`, `CitaDAO.listarPorContribuyentePagina`. Cada `Impl` tiene su `ORDEN` (Map) y `resolverOrden(...)`. Los métodos anteriores siguen igual. |
| Servlets | `ContribuyenteServlet` (listado y ficha), `MaterialServlet`, `UsuarioServlet`, `MovimientoServlet` (historial) usan los métodos nuevos. |
| `fragmentos/paginacion.jsp`, `tags/urlLista.tag`, `tags/thOrden.tag`, `assets/js/paginacion.js` | Barra de paginación (Primera, Anterior, números cercanos, Siguiente, Última, "Mostrando X a Y de Z", selector de tamaño que conserva filtros y orden) y encabezados ordenables con flecha y `aria-sort`. |

Columnas ordenables: contribuyentes (dniRuc, nombre, tipo, telefono, email), materiales (codigo, nombre, unidad, stock, minimo),
usuarios (username, nombre, rol, area, estado, acceso), movimientos (fecha, tipo, cantidad, area, usuario), citas de la ficha
(fecha, hora, area, funcionario, motivo, estado). Orden por defecto: nombre ascendente; movimientos y citas: fecha descendente.

### 3. Avisos (toasts)
- `fragmentos/toasts.jsp` (incluido por la barra): contenedor en la esquina superior derecha con `aria-live` y, si el servidor dejó
  un mensaje de una sola vez (flash), un elemento **oculto** con `data-tipo` y `data-mensaje` (escapado con `c:out`). Nunca va
  dentro de un script.
- `assets/js/toast.js`: `mostrarToast(tipo, mensaje)` con `exito`, `error`, `aviso`, `info`: ícono, cierre con el botón o con Escape,
  se cierra solo a los 5 s (8 s los errores), se detiene mientras el mouse (o el foco) está encima y se apilan (máximo 5). El texto se
  inserta con `textContent` (nunca HTML). Los errores usan `role="alert"` y los demás `role="status"`.
- Los cuadros `alert` de mensajes flash se quitaron de los listados, la ficha y el perfil. Los resultados de las llamadas `$.ajax`
  (movimientos, citas, usuarios, panel y desbloqueos) usan `mostrarToast`. Los errores de validación del formulario siguen junto a
  los campos.

### 4. Modo oscuro
- `assets/js/tema.js` (en el `<head>`, síncrono): aplica `data-bs-theme` con la elección guardada en `localStorage` (dentro de
  `try/catch`) o, si no hay, con `prefers-color-scheme`. Botón sol/luna con `aria-label` en la barra y en el login. Sigue los
  cambios del sistema mientras no se haya elegido. **La impresión siempre sale en tema claro** (`beforeprint`/`afterprint`).
- `assets/css/sigmud.css`: todos los colores propios son variables CSS con valores para claro y oscuro (contraste AA, con las razones
  calculadas en el comentario inicial). Los gráficos de Chart.js leen el color del texto y de la cuadrícula del tema, usan tonos más
  claros en oscuro y se redibujan al cambiar el tema (evento `sigmud:tema`) sin recargar.

### 5. Accesibilidad básica
Foco visible en todos los controles (contorno de 3 px, blanco sobre la barra), enlace **"Saltar al contenido"** al inicio de cada
página (`<main id="contenido">`), `alt` en los logos, íconos decorativos con `aria-hidden`, `label` asociado a cada campo,
modales con el manejo de teclado de Bootstrap (foco atrapado y Escape), toasts con botón de cierre enfocable y Escape.

### Pruebas automáticas nuevas
| Prueba | Qué comprueba |
|---|---|
| `PaginaTest` (8) | páginas totales y límites (0, 1, 10, 11, 41…), tamaño inválido → 10, página -5/0/999999 corregida, `desde`/`hasta`, `desplazamiento` sin desbordarse con páginas enormes |
| `OrdenSqlTest` (8) | columna permitida → fragmento fijo; sin columna o no permitida → orden por defecto; ataques (`;DROP TABLE`, `1 OR 1=1`, `--`, comillas, `/* */`, subconsultas, saltos de línea, nombre de columna real de la BD) → el SQL **nunca** contiene texto del usuario; dirección solo asc/desc; las 5 listas blancas reales solo tienen fragmentos `[a-z_.]+` |
| `ListadosPaginadosDAOTest` (10) | contra `mun_quinua_test`: LIMIT/OFFSET y conteo con 25/12/13 registros, páginas 1-3, tamaño 10/20/50, página fuera de rango y tamaño inválido, orden asc/desc, columnas con inyección usan el orden por defecto y la tabla sigue intacta, inactivos, `%` como texto, críticos, usuarios, movimientos y citas con filtro de estado |

### Pruebas del script `pruebas-seguridad.ps1` (sección 8f)
Para `/contribuyentes`, `/almacen` y `/usuarios`: `?orden=nombre;DROP TABLE contribuyente`, `?orden=1 OR 1=1`, `orden` con `--` y comillas,
`dir` con `;DROP`, `?pagina=-5`, `999999`, `abc`, enorme, `?tamano=100000`, `-1`, `7` → 200 con el listado normal (valores por defecto), sin
error 500 ni datos extraños (36 chequeos). Además: tamaño 10/50 marcado en el selector; `aria-sort` correcto; historial y ficha con
parámetros hostiles; flash como toast (`datosFlash` y sin `alert`); favicon, `theme-color`, botón de tema, "Saltar al contenido" y
`contenedorToasts` presentes; recursos estáticos públicos; **CSP presente** en login, menú, listados, auditoría y ficha (sin
`unsafe-inline` ni orígenes externos); y búsqueda con `Select-String` en todos los JSP y tags: **0 `<script>` en línea, 0 `onclick`/`onerror`/…,
0 `javascript:`, 0 `<style>` en línea y 0 enlaces a CDN**.

### Verificación realizada
- JUnit: **376 pruebas, 0 fallos** (antes 350).
- `pruebas-seguridad.ps1`: **207 APROBADAS, 0 FALLIDAS** (antes 141).
- Navegador (capturas revisadas): login claro/oscuro, el tema se guarda y persiste, Tab muestra "Saltar al contenido", foco visible, panel y listados
  en oscuro, gráficos redibujados al cambiar el tema, paginación (Siguiente, Última, 10/20/50), orden por columna, toasts de éxito y de error por Ajax,
  flash de servidor como toast, cierre con Escape, pausa del temporizador con el mouse, y ficha impresa en tema claro. Sin errores de JavaScript.

### Cómo probarlo a mano
1. `.\desplegar.ps1` y abra `http://localhost:8080/sigmud/`. Pulse el botón de luna/sol del login: el tema cambia y se recuerda al recargar.
2. Entre como `admin`; en **Contribuyentes** (necesita más de 10 registros) pruebe Siguiente/Última, el selector "Por página" y los encabezados.
   Pruebe también en la dirección: `/contribuyentes?orden=nombre;DROP&pagina=-5&tamano=100000` (muestra el listado normal).
3. En **Almacén** registre una entrada (toast verde) y una salida mayor al stock (toast rojo). Dé de baja un contribuyente (toast al recargar).
4. Con el teclado: Tab en cualquier página muestra "Saltar al contenido"; Escape cierra un aviso enfocado.
5. **Ficha** → Imprimir: sale siempre en tema claro aunque la pantalla esté en oscuro.
6. `.\pruebas-seguridad.ps1` y `mvn test`.

### Capturas de pantalla para la monografía (lista numerada)
1. Login en tema claro (con el logo oficial cuando lo coloque en `assets/img/logo.png`).
2. Login en tema oscuro.
3. Menú/panel del ADMIN en tema claro.
4. Menú/panel del ADMIN en tema oscuro (con los gráficos).
5. Menú/panel de ATENCION en tema claro y 6. en tema oscuro.
7. Menú/panel de ALMACEN en tema claro y 8. en tema oscuro.
9. Listado de contribuyentes con paginación, el selector "Por página" y el orden por columna (flecha en un encabezado), en tema claro.
10. El mismo listado en tema oscuro, en la página 2 o 3.
11. Un toast de éxito (por ejemplo, tras registrar una entrada de stock).
12. Un toast de error (por ejemplo, "Stock insuficiente…").
13. La ficha del contribuyente en pantalla.
14. La ficha impresa (vista previa de impresión del navegador: tema claro, encabezado con fecha y usuario).
15. (Opcional) "Saltar al contenido" visible al pulsar Tab, y el foco visible en un botón.

## Cómo probar (Módulo 1)

1. `.\desplegar.ps1` desde la raíz del proyecto (PowerShell). Abrir `http://localhost:8080/sigmud/`.

| Caso | Esperado | Obtenido |
|---|---|---|
| Abrir `/sigmud/menu` sin sesión | Redirige a `/login` | (pendiente) |
| `admin` / `Admin123` | Menú con 3 tarjetas "Próximamente", nombre, rol ADMIN y sesiones activas | (pendiente) |
| `atencion1` | Tarjetas Contribuyentes y Citas (sin Almacén) | (pendiente) |
| `almacen1` | Solo tarjeta Almacén | (pendiente) |
| Clave incorrecta | Mensaje genérico, `intentos_fallidos` +1 | (pendiente) |
| Usuario inexistente | Mismo mensaje genérico | (pendiente) |
| 5 claves incorrectas y luego la correcta | "La cuenta está bloqueada…", `estado = 0` | (pendiente) |
| `' OR '1'='1` como usuario | Mensaje genérico (no pasa el regex ni el SQL) | (pendiente) |
| `almacen1` abre `/sigmud/citas` | 403 "Acceso denegado" | (pendiente) |
| `admin` abre `/sigmud/citas` | 404 (el módulo no existe aún) | (pendiente) |
| Cerrar sesión y botón "atrás" | No muestra el menú; pide login | (pendiente) |

## Pendiente

- Módulos Contribuyentes, Almacén y Citas.
- Limitar la longitud del campo clave en el formulario ya está; falta protección CSRF si el curso la exige.

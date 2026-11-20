# 02. Requerimientos del sistema (Capítulo III, secciones 3.2 y 3.3)

> Los requerimientos se **dedujeron de lo que realmente está implementado** en el código (Servlets, DAO, validadores, filtros y
> scripts SQL), no de un documento de requisitos original. La columna **Prioridad** es una **propuesta** (Alta/Media/Baja) que debe
> confirmarse con el documento del curso: el código no guarda prioridades. Los actores se escriben con los roles de la base de datos:
> ADMIN, ATENCION (funcionario de atención) y ALMACEN (encargado de almacén).

## 1. Requerimientos funcionales

| ID | Nombre | Descripción (según lo implementado) | Módulo | Actor | Prioridad (propuesta) |
|---|---|---|---|---|---|
| RF-01 | Iniciar sesión | El usuario entra con usuario y contraseña. El servidor valida el formato, compara el hash, crea una sesión **nueva** y redirige al panel de su rol. Mensaje único "Usuario o contraseña incorrectos" para cualquier fallo | Acceso | Todos | Alta |
| RF-02 | Bloquear cuenta por intentos fallidos | Cada contraseña incorrecta de una cuenta activa suma un intento; al llegar a **5** la cuenta queda bloqueada (`estado = 0`). El aviso de "cuenta bloqueada" solo se muestra si la contraseña es correcta | Acceso | Sistema | Alta |
| RF-03 | Cerrar sesión | `/logout` invalida la sesión y vuelve al login; el botón "atrás" no muestra páginas protegidas (cabeceras anti-caché) | Acceso | Todos | Alta |
| RF-04 | Control de acceso por rol | Solo `/login` y `/assets` son públicos. Cada ruta exige uno de los roles permitidos (403 si no); el menú/panel muestra solo lo que el rol puede usar | Acceso | Todos | Alta |
| RF-05 | Cambio obligatorio de contraseña temporal | Una cuenta con contraseña temporal solo puede usar `/perfil/password`, `/logout` y `/assets` hasta cambiarla | Acceso | Todos | Alta |
| RF-06 | Mi perfil y cambio de contraseña | El usuario ve sus datos y cambia su contraseña (actual + nueva + confirmación); política: mínimo 8 caracteres, mayúscula, minúscula, número y distinta del usuario; al cambiarla se renueva el id de sesión | Acceso | Todos | Media |
| RF-07 | Registrar contribuyente | Alta de persona NATURAL (DNI de 8 dígitos) o JURIDICA (RUC de 11 dígitos que empieza con 10 o 20), nombre/razón social (3 a 150), teléfono (9 dígitos, empieza con 9), correo, dirección y fecha de nacimiento (solo naturales, no futura). DNI/RUC único | Contribuyentes | ADMIN, ATENCION | Alta |
| RF-08 | Editar contribuyente | Modifica los datos; el tipo y el DNI/RUC se muestran como solo lectura; no cambia el estado | Contribuyentes | ADMIN, ATENCION | Alta |
| RF-09 | Dar de baja contribuyente (baja lógica) | Pone `estado = 0` previa confirmación; la fila no se borra; deja de aparecer en el listado normal y no admite citas nuevas | Contribuyentes | ADMIN, ATENCION | Alta |
| RF-10 | Reactivar contribuyente | Desde la ficha, vuelve a poner `estado = 1` a un contribuyente dado de baja | Contribuyentes | ADMIN, ATENCION | Media |
| RF-11 | Listar, buscar, ordenar y paginar contribuyentes | Búsqueda por nombre, razón social, DNI o RUC (los comodines `%` y `_` se buscan como texto); interruptor para ver también los dados de baja; orden por columna y paginación de 10/20/50 en el servidor | Contribuyentes | ADMIN, ATENCION | Alta |
| RF-12 | Ficha del contribuyente | Datos personales, indicadores de citas (total y por estado), próxima cita, historial de citas (filtro por estado, paginado y ordenable), pestaña "Actividad" (últimos 10 eventos de auditoría, solo ADMIN), botón de imprimir (hoja sin logo) y acceso a "Nueva cita" con el contribuyente ya elegido. Abrirla registra `CONSULTA_FICHA` | Contribuyentes | ADMIN, ATENCION | Media |
| RF-13 | Registrar material | Alta con código `AAA-999` (por ejemplo MAT-006), nombre, unidad de una lista fija (UNIDAD, PAQUETE, BOLSA, CAJA, KILO, LITRO, METRO, GALON) y stock mínimo; el stock inicial es 0; código único | Almacén | ADMIN, ALMACEN | Alta |
| RF-14 | Editar material | Cambia nombre, unidad y stock mínimo; el código es de solo lectura y el **stock no se edita** (solo cambia con movimientos) | Almacén | ADMIN, ALMACEN | Alta |
| RF-15 | Dar de baja material (baja lógica) | `estado = 0` previa confirmación; sin `DELETE`; un material de baja no admite movimientos | Almacén | ADMIN, ALMACEN | Alta |
| RF-16 | Listar, buscar y paginar materiales | Búsqueda por texto, filtro "solo stock crítico" (stock ≤ mínimo, con insignia), orden por columna y paginación en el servidor | Almacén | ADMIN, ALMACEN | Alta |
| RF-17 | Registrar entrada de stock | Suma la cantidad al stock en una **transacción** (movimiento + stock + auditoría); respuesta Ajax JSON; la fila se actualiza sin recargar | Almacén | ADMIN, ALMACEN | Alta |
| RF-18 | Registrar salida de stock | Resta del stock; exige el área solicitante; si el stock no alcanza responde con el error y **no cambia nada** (rollback); bloqueo de fila (`FOR UPDATE`) contra salidas simultáneas; cantidad de 1 a 100 000 | Almacén | ADMIN, ALMACEN | Alta |
| RF-19 | Historial de movimientos de un material | Lista entradas y salidas del material con fecha, tipo, cantidad, área y usuario; paginada y ordenable | Almacén | ADMIN, ALMACEN | Media |
| RF-20 | Reportes de almacén | Cuatro reportes: stock crítico, movimientos por fechas (filtros de tipo, área y material), resumen por material (entradas, salidas, saldo y verificación contra el stock) y salidas por área; periodo máximo de 366 días; exportación CSV | Almacén | ADMIN, ALMACEN | Media |
| RF-21 | Programar cita | Cita para un contribuyente activo, en un área, con un funcionario de esa área, fecha (hoy a 90 días, de lunes a viernes), hora (08:00 a 16:30 cada 30 min), ventanilla (1 a 10) y motivo. **Regla de no cruce**: un funcionario no puede tener dos citas no canceladas en la misma fecha y hora | Citas | ADMIN, ATENCION | Alta |
| RF-22 | Reprogramar cita | Cambia fecha, hora y ventanilla de una cita PROGRAMADA aplicando la misma regla de no cruce | Citas | ADMIN, ATENCION | Media |
| RF-23 | Atender o cancelar cita | De PROGRAMADA a ATENDIDA (solo el día de la cita o después) o a CANCELADA (libera el horario); una cita ATENDIDA o CANCELADA ya no se modifica | Citas | ADMIN, ATENCION | Alta |
| RF-24 | Consultar citas del día y ayudas de la pantalla | Listado por fecha y área (ATENCION ve su área por defecto) y consultas Ajax: autocompletado de contribuyentes (mínimo 3 letras), funcionarios por área y turnos libres/ocupados | Citas | ADMIN, ATENCION | Media |
| RF-25 | Reporte de citas | Citas por área y por funcionario en un rango de fechas (máximo 366 días), con fila TOTAL y exportación CSV | Citas | ADMIN, ATENCION | Media |
| RF-26 | Panel del administrador | 7 indicadores (usuarios, contribuyentes, materiales, stock crítico, citas de hoy, movimientos de hoy, cuentas bloqueadas), 3 gráficos, lista de cuentas bloqueadas con "Desbloquear", últimos accesos, accesos fallidos de 24 h y actividad reciente; se actualiza cada 60 s | Paneles | ADMIN | Media |
| RF-27 | Panel del funcionario de atención | Sus citas de hoy por estado, sus próximas citas (hasta 5) con "Atender", gráfico de sus citas atendidas de 7 días; sin datos de otros usuarios | Paneles | ATENCION | Media |
| RF-28 | Panel del encargado de almacén | 5 indicadores, los 5 materiales con mayor faltante con acceso a "Registrar entrada", salidas por área del mes y últimos movimientos | Paneles | ALMACEN | Media |
| RF-29 | Gestionar usuarios | Crear usuarios (la contraseña temporal se genera y se muestra **una sola vez**), editar nombre/rol/área, activar o desactivar; reglas: nombre único, un usuario ATENCION debe tener área, nadie se desactiva ni se quita el rol a sí mismo y **siempre debe quedar un ADMIN activo** | Usuarios | ADMIN | Alta |
| RF-30 | Restablecer contraseña y desbloquear cuenta | El ADMIN genera una contraseña temporal para otro usuario (no la propia) y desbloquea cuentas bloqueadas (intentos a 0 y cuenta activa) | Usuarios | ADMIN | Alta |
| RF-31 | Registrar auditoría de cambios | Cada acción importante (inicio y cierre de sesión, login fallido, bloqueo, altas, cambios, bajas, reactivaciones, citas, usuarios, contraseñas, consulta de ficha) deja un registro con usuario, acción, entidad, descripción, detalle campo por campo e IP. El registro es **inmutable** (triggers) y los movimientos de stock se guardan dentro de la misma transacción | Auditoría | Sistema | Alta |
| RF-32 | Consultar y exportar la auditoría | El ADMIN filtra por fechas (máximo 366 días), usuario, acción, entidad y texto; ve el detalle en una ventana; exporta a CSV hasta 10 000 filas. No hay forma de editar ni borrar | Auditoría | ADMIN | Media |
| RF-33 | Avisos y mensajes al usuario | Resultados de operaciones como avisos (toasts) y mensajes de validación junto a los campos; errores en lenguaje claro sin detalles técnicos | Interfaz | Todos | Media |

## 2. Requerimientos no funcionales

### 2.1 Seguridad
| ID | Requerimiento | Cómo se cumple (lo implementado) |
|---|---|---|
| RNF-01 | Evitar inyección SQL | Todo SQL con `PreparedStatement`; el orden por columna usa una lista blanca (`OrdenSql`) |
| RNF-02 | Proteger las contraseñas | Hash PBKDF2-HMAC-SHA256, sal de 16 bytes, 120 000 iteraciones, comparación en tiempo constante; el hash no va a la sesión ni a la pantalla; las contraseñas no se escriben en el log |
| RNF-03 | Protección CSRF | Token por sesión (43 caracteres) en todo POST salvo `/login`; en formularios como campo oculto y en Ajax como cabecera `X-CSRF-Token` |
| RNF-04 | Cabeceras de seguridad y CSP | `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy`, `Content-Security-Policy` con `script-src 'self'` (ningún JavaScript dentro del HTML) |
| RNF-05 | Sesión segura | Cookie `HttpOnly` y `SameSite=Strict`, identificador solo en cookie, sesión de 30 minutos, sesión nueva al iniciar sesión (evita fijación) |
| RNF-06 | Evitar XSS | Todo texto del usuario se imprime con `<c:out>`; los avisos se insertan con `textContent` |
| RNF-07 | No revelar detalles técnicos | Páginas de error propias (400, 401, 403, 404, 405, 500 y excepciones) sin versión del servidor ni trazas; el detalle va al log |
| RNF-08 | Protección de datos personales | La consulta de la ficha queda auditada (`CONSULTA_FICHA`, por la Ley N.º 29733 según `docs/auditoria.md`) |

### 2.2 Usabilidad
| ID | Requerimiento | Cómo se cumple |
|---|---|---|
| RNF-09 | Interfaz adaptable | Bootstrap 5 (rejilla responsive, tablas con desplazamiento) |
| RNF-10 | Modo claro y oscuro | Botón de tema, preferencia guardada en el navegador, gráficos que se redibujan, impresión siempre en claro |
| RNF-11 | Accesibilidad básica | Foco visible, enlace "Saltar al contenido", etiquetas y `alt`, encabezados con `aria-sort`, avisos con `role`/`aria-live`, uso con teclado |
| RNF-12 | Doble validación | Validación jQuery en el navegador **y** validación obligatoria en el servidor |
| RNF-13 | Mensajes y textos en español | Toda la interfaz y los mensajes de error están en español |

### 2.3 Rendimiento
| ID | Requerimiento | Cómo se cumple |
|---|---|---|
| RNF-14 | Listados acotados | Paginación en el servidor con `LIMIT`/`OFFSET` (10, 20 o 50 filas) |
| RNF-15 | Consultas con índices | Índices en `movimiento_stock (id_material, fecha)`, `cita (fecha, id_area)` y claves únicas; `auditoria` indexada por fecha, acción y usuario |

> **No hay pruebas de carga ni de concurrencia** (reconocido en `docs/auditoria.md`); RNF-14 y RNF-15 describen el diseño, no una
> medición de rendimiento.

### 2.4 Mantenibilidad
| ID | Requerimiento | Cómo se cumple |
|---|---|---|
| RNF-16 | Arquitectura por capas | Controlador (Servlet) → DAO → MySQL, con modelos, utilidades, filtros y excepciones propias en paquetes separados |
| RNF-17 | Pruebas automatizadas | 376 pruebas JUnit (validadores, seguridad, DAO) y 207 comprobaciones en vivo |
| RNF-18 | Documentación | `CLAUDE.md` (reglas), `docs/avance.md` y `docs/auditoria.md` |

### 2.5 Portabilidad
| ID | Requerimiento | Cómo se cumple |
|---|---|---|
| RNF-19 | Funciona sin internet | Bootstrap, jQuery, Chart.js e íconos son archivos locales; sin CDN ni fuentes externas |
| RNF-20 | Ejecutable en cualquier Tomcat 10.1 con MySQL 8.x | WAR estándar; configuración de la base en `db.properties`; compilación a nivel Java 17 |

### 2.6 Confiabilidad
| ID | Requerimiento | Cómo se cumple |
|---|---|---|
| RNF-21 | Operaciones atómicas | Movimientos de stock en una transacción con `commit`/`rollback` y `autoCommit` restaurado |
| RNF-22 | Integridad de datos | Claves foráneas, `UNIQUE`, `CHECK` (documento, stock ≥ 0, cantidad > 0) e índice único de cita |
| RNF-23 | Registro inmutable | Triggers de la tabla `auditoria` impiden `UPDATE` y `DELETE` |

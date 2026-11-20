# 03. Actores y casos de uso (Capítulo III, sección 3.4)

Diagramas (texto PlantUML, ver `LEEME.md` para visualizarlos):
- `diagramas/casos_de_uso_general.puml` — todos los actores y casos de uso.
- `diagramas/casos_de_uso_administrador.puml`, `casos_de_uso_atencion.puml`, `casos_de_uso_almacen.puml` — uno por actor.

## 1. Actores

Los tres actores corresponden a los tres valores de `usuario.rol` (columna `ENUM('ADMIN','ATENCION','ALMACEN')`) y a los permisos que
`AuthFilter` aplica por ruta.

| Actor | Rol en la base de datos | Descripción | Rutas permitidas (según `AuthFilter`) |
|---|---|---|---|
| **Administrador** | `ADMIN` | Responsable del sistema: gestiona usuarios, desbloquea cuentas, consulta la auditoría y puede usar todos los módulos operativos | `/contribuyentes`, `/almacen/*`, `/citas/*`, `/usuarios/*`, `/auditoria`, `/dashboard/*` y las rutas comunes |
| **Funcionario de atención** | `ATENCION` | Atiende a los contribuyentes: registra y edita su padrón y gestiona las citas de su área. Pertenece a un área (`usuario.id_area`, obligatoria para este rol) | `/contribuyentes`, `/citas/*`, `/dashboard/datos` y las rutas comunes |
| **Encargado de almacén** | `ALMACEN` | Administra los materiales y registra las entradas y salidas de stock | `/almacen/*`, `/dashboard/datos` y las rutas comunes |

Rutas comunes (sin restricción de rol, pero con sesión): `/menu`, `/logout`, `/perfil`, `/perfil/password`. Rutas públicas: `/login` y
`/assets/*`. Cualquier otra combinación responde **403**.

## 2. Matriz de casos de uso por actor

| Caso de uso | ADMIN | ATENCION | ALMACEN |
|---|:-:|:-:|:-:|
| CU-01 Iniciar sesión / cerrar sesión | ✔ | ✔ | ✔ |
| CU-02 Cambiar mi contraseña (Mi perfil) | ✔ | ✔ | ✔ |
| CU-03 Ver mi panel (dashboard) | ✔ | ✔ | ✔ |
| CU-04 Registrar y editar contribuyente | ✔ | ✔ | — |
| CU-05 Dar de baja / reactivar contribuyente | ✔ | ✔ | — |
| CU-06 Consultar ficha del contribuyente | ✔ | ✔ (sin pestaña Actividad) | — |
| CU-07 Programar / reprogramar cita | ✔ | ✔ | — |
| CU-08 Atender / cancelar cita | ✔ | ✔ | — |
| CU-09 Reporte de citas | ✔ | ✔ | — |
| CU-10 Gestionar materiales | ✔ | — | ✔ |
| CU-11 Registrar entrada / salida de stock | ✔ | — | ✔ |
| CU-12 Reportes de almacén | ✔ | — | ✔ |
| CU-13 Gestionar usuarios | ✔ | — | — |
| CU-14 Restablecer contraseña / desbloquear cuenta | ✔ | — | — |
| CU-15 Consultar y exportar auditoría | ✔ | — | — |

## 3. Descripción de los casos de uso principales

### CU-01 Iniciar sesión
- **Actor:** cualquier usuario con cuenta.
- **Precondición:** la cuenta existe y está activa; el usuario está en `/login`.
- **Flujo básico:** 1) escribe usuario y contraseña; 2) jQuery valida el formato; 3) `LoginServlet` valida el formato otra vez, busca el
  usuario y verifica el hash; 4) se reinician los intentos fallidos y se guarda el último acceso; 5) si el hash era antiguo (SHA-256), se
  migra a PBKDF2; 6) se invalida cualquier sesión anterior y se crea una **sesión nueva** con su token CSRF; 7) se registra `LOGIN_OK` en
  la auditoría y se redirige a `/menu`, que muestra el panel del rol.
- **Flujo alterno:** (a) usuario inexistente, contraseña incorrecta o formato inválido → el mismo mensaje "Usuario o contraseña
  incorrectos" (401); en una cuenta activa suma un intento fallido y se audita `LOGIN_FALLIDO`; (b) al 5.º intento la cuenta queda
  bloqueada (`CUENTA_BLOQUEADA`); (c) con la contraseña correcta de una cuenta bloqueada se muestra el aviso de bloqueo; (d) si la
  contraseña es temporal se obliga a cambiarla antes de usar el sistema.
- **Postcondición:** sesión abierta con el usuario (sin su hash) y el token CSRF; evento registrado en la auditoría.

### CU-04 Registrar contribuyente
- **Actor:** ADMIN o ATENCION.
- **Precondición:** sesión activa con rol permitido.
- **Flujo básico:** 1) abre "Nuevo contribuyente"; 2) elige NATURAL o JURIDICA y completa los datos; 3) jQuery valida (DNI de 8 dígitos o
  RUC de 11 que empieza con 10 o 20, nombre de 3 a 150 caracteres, teléfono de 9 dígitos que empieza con 9, correo, fecha de nacimiento);
  4) el POST lleva el token CSRF; 5) `ContribuyenteServlet` valida con `ContribuyenteValidador`; 6) `ContribuyenteDAO.insertar` guarda;
  7) se audita `CREAR`; 8) se deja el mensaje "Contribuyente registrado correctamente." y se **redirige** al listado (Post-Redirect-Get).
- **Flujo alterno:** (a) datos inválidos → vuelve al formulario con los errores y los datos escritos; (b) DNI/RUC ya existe (`UNIQUE`) →
  "Ya existe un contribuyente con ese DNI/RUC"; (c) falla la base → mensaje genérico y detalle solo en el log.
- **Postcondición:** una fila nueva en `contribuyente` con `estado = 1` y un registro en `auditoria`.

### CU-05 Dar de baja contribuyente
- **Actor:** ADMIN o ATENCION.
- **Precondición:** el contribuyente existe y está activo.
- **Flujo básico:** 1) pulsa "Dar de baja" y confirma en la ventana; 2) POST con token CSRF; 3) `darDeBaja` hace `UPDATE ... estado = 0`;
  4) se audita `BAJA` con el DNI/RUC y el nombre; 5) mensaje de éxito y redirección.
- **Flujo alterno:** si el id no es válido o no existe → "El contribuyente no existe."
- **Postcondición:** la fila **no se borra**; deja de verse en el listado normal y no admite citas nuevas; se puede reactivar desde la ficha.

### CU-07 Programar cita
- **Actor:** ADMIN o ATENCION.
- **Precondición:** el contribuyente existe y está activo; hay funcionarios activos de ATENCION en el área.
- **Flujo básico:** 1) abre "Nueva cita" (o desde la ficha, con el contribuyente ya elegido y bloqueado); 2) busca al contribuyente
  (Ajax, mínimo 3 letras); 3) elige área y funcionario (Ajax: solo funcionarios de esa área); 4) elige fecha; el navegador muestra los
  turnos libres y ocupados; 5) elige hora, ventanilla y motivo; 6) `CitaValidador` comprueba contribuyente activo, funcionario del área,
  fecha de hoy a 90 días y de lunes a viernes, hora de 08:00 a 16:30 cada 30 minutos, ventanilla de 1 a 10 y motivo; 7) `CitaDAO.insertar`
  consulta si hay cruce y guarda; 8) se audita `CITA_CREAR`; 9) mensaje y redirección al listado de esa fecha.
- **Flujo alterno:** (a) hay cruce (consulta previa o violación del `UNIQUE` si dos personas guardan a la vez) → "El funcionario ya tiene
  una cita el dd/MM/aaaa a las HH:mm. Elija otro horario." y vuelve al formulario con lo escrito; (b) validación fallida → lista de errores.
- **Postcondición:** cita `PROGRAMADA` en `cita`; el horario queda ocupado para ese funcionario.

### CU-08 Atender o cancelar una cita
- **Actor:** ADMIN o ATENCION.
- **Precondición:** la cita está `PROGRAMADA`.
- **Flujo básico:** 1) pulsa "Atender" o "Cancelar"; 2) POST con token CSRF; 3) `CitaDAO.cambiarEstado` actualiza el estado solo si sigue
  `PROGRAMADA`; 4) se audita `CITA_ATENDER` o `CITA_CANCELAR`; 5) mensaje y redirección.
- **Flujo alterno:** (a) la cita ya no está programada → mensaje con el estado actual; (b) intentar marcar como atendida una cita de una
  fecha futura → rechazado.
- **Postcondición:** al cancelar, la columna `hora_activa` pasa a `NULL` y el horario **queda libre**.

### CU-11 Registrar salida de stock (y entrada)
- **Actor:** ADMIN o ALMACEN.
- **Precondición:** el material existe y está activo.
- **Flujo básico:** 1) en el listado pulsa "Salida" en la fila del material; 2) en la ventana escribe cantidad, área solicitante y
  observación; 3) jQuery valida (entero de 1 a 100 000, área obligatoria en salidas); 4) `$.ajax` envía POST con la cabecera
  `X-CSRF-Token`; 5) `MovimientoServlet` valida y toma el usuario de la **sesión**; 6) `MovimientoDAO.registrarMovimiento` abre una
  transacción: bloquea la fila del material (`FOR UPDATE`), comprueba el stock, inserta el movimiento, descuenta el stock y guarda la
  auditoría; 7) `commit`; 8) responde `{"ok":true,"mensaje":"Salida registrada correctamente. Stock actual: N.", ...}`; 9) la página
  actualiza el stock y la insignia (Normal/Crítico) de la fila y muestra un aviso, sin recargar.
- **Flujo alterno:** (a) stock insuficiente → `ok:false` con "solo hay N" y **no cambia nada**; (b) material inexistente o dado de baja →
  `ok:false`; (c) error de base de datos → `rollback`, respuesta 500 con mensaje genérico y detalle en el log.
- **Postcondición:** movimiento en `movimiento_stock`, stock actualizado en `material` y evento `MOVIMIENTO_STOCK` en `auditoria`, los tres
  confirmados juntos o ninguno.
- **Entrada:** mismo flujo, sin área, y el stock se **suma**.

### CU-13 Gestionar usuarios (crear)
- **Actor:** ADMIN.
- **Precondición:** sesión de ADMIN.
- **Flujo básico:** 1) abre "Nuevo usuario" y escribe nombre de usuario, nombres, rol y (si es ATENCION) área; 2) el servidor valida y
  genera una contraseña **temporal** de 12 caracteres que cumple la política; 3) se guarda con hash PBKDF2 y `debe_cambiar_password = 1`;
  4) se audita `USUARIO_CREAR` (sin la contraseña); 5) la contraseña temporal se muestra **una sola vez** al ADMIN.
- **Flujo alterno:** nombre de usuario repetido o ATENCION sin área → error en el formulario.
- **Reglas:** un ADMIN no puede desactivarse ni cambiarse el rol a sí mismo, y no se puede dejar al sistema sin un ADMIN activo.
- **Postcondición:** nuevo usuario activo que, al iniciar sesión, solo puede cambiar su contraseña.

### CU-14 Restablecer contraseña / desbloquear cuenta
- **Actor:** ADMIN.
- **Flujo básico:** desde el listado de usuarios (o "Desbloquear" en el panel) el ADMIN confirma; el POST Ajax lleva el token CSRF; para
  restablecer se genera una contraseña temporal (se muestra una vez) y `debe_cambiar_password = 1`; para desbloquear se ponen los intentos
  en 0 y se reactiva la cuenta bloqueada. Se audita `PASSWORD_RESTABLECIDA` o `USUARIO_DESBLOQUEADO`.
- **Flujo alterno:** el ADMIN no puede restablecer su propia contraseña desde aquí; desbloquear una cuenta que no está bloqueada responde
  "no está bloqueada".

### CU-15 Consultar auditoría
- **Actor:** ADMIN (los demás reciben 403).
- **Flujo básico:** `/auditoria` muestra los últimos 7 días; el ADMIN filtra por fechas (máximo 366 días), usuario, acción, entidad y
  texto; ve el detalle de cada evento en una ventana; puede exportar el resultado a CSV (hasta 10 000 filas).
- **Flujo alterno:** periodo inválido → mensaje de error sin consultar.
- **Postcondición:** solo lectura: no existe ninguna opción para editar o borrar, y la base lo impide con triggers.

### CU-06 Consultar la ficha de un contribuyente
- **Actor:** ADMIN o ATENCION.
- **Flujo básico:** desde el listado pulsa el nombre o "Ficha"; se muestran datos, indicadores de citas, próxima cita e historial (filtro
  por estado, paginado y ordenable); el ADMIN además ve la pestaña "Actividad" (últimos 10 eventos de auditoría de ese contribuyente);
  se registra `CONSULTA_FICHA`; puede imprimir la ficha, dar de baja o reactivar y crear una cita con el contribuyente ya elegido.
- **Flujo alterno:** id inválido o inexistente → vuelve al listado con un aviso de error.

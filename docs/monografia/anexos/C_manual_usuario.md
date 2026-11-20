# Anexo C. Manual de usuario (resumido por rol)

Los marcadores **[CAPTURA n]** indican dónde insertar la figura n de `10_capturas.md`. La dirección del sistema es
`http://localhost:8080/sigmud/` (o la que defina el servidor de la municipalidad).

## 1. Acciones comunes a todos los roles

### 1.1 Iniciar sesión
1. Abra la dirección del sistema. **[CAPTURA 1]**
2. Escriba su usuario y su contraseña y pulse **Ingresar**. Si se equivoca verá "Usuario o contraseña incorrectos" **[CAPTURA 3]**.
3. Entra al panel de su rol. Si su contraseña es temporal, el sistema lo lleva a cambiarla antes de seguir **[CAPTURA 5]**.
- Después de **5 intentos fallidos** la cuenta se bloquea; pida al administrador que la desbloquee.

### 1.2 Cambiar mi contraseña
Menú de usuario → **Mi perfil** → **Cambiar contraseña** **[CAPTURA 36]**. Escriba la actual, la nueva y su confirmación. La nueva debe tener al
menos 8 caracteres, una mayúscula, una minúscula y un número, y no puede ser igual a su nombre de usuario.

### 1.3 Modo claro/oscuro y cierre de sesión
El botón de sol/luna cambia el tema y el navegador lo recuerda. **Cerrar sesión** (menú superior) termina su sesión; la sesión también se
cierra sola tras 30 minutos sin actividad.

### 1.4 Listados
En los listados puede **ordenar** pulsando el encabezado de una columna, elegir cuántas filas ver por página (10, 20 o 50) y moverse con
los botones de página **[CAPTURA 42]**.

## 2. Administrador (ADMIN)

El administrador puede usar los módulos de los otros roles y además administra el sistema.

### 2.1 Panel del administrador **[CAPTURA 6]**
Muestra indicadores (usuarios, contribuyentes, materiales, stock crítico, citas y movimientos de hoy, cuentas bloqueadas), gráficos, la lista
de cuentas bloqueadas y los últimos accesos. Se actualiza cada minuto. En una cuenta bloqueada pulse **Desbloquear** y confirme.

### 2.2 Gestionar usuarios **[CAPTURA 34]**
- **Crear:** Usuarios → **Nuevo usuario**, complete nombre de usuario, nombres, rol y (para ATENCION) el área. Al guardar, el sistema muestra
  una **contraseña temporal una sola vez** **[CAPTURA 35]**: anótela y entréguela al usuario por un canal seguro.
- **Editar / activar / desactivar:** desde la fila del usuario. No puede desactivarse ni cambiarse el rol a sí mismo, y siempre debe quedar
  un administrador activo.
- **Restablecer contraseña:** botón en la fila; genera una nueva contraseña temporal (no disponible para su propia cuenta).
- **Desbloquear:** botón en la fila o en el panel.

### 2.3 Consultar la auditoría **[CAPTURA 37]**
Menú → **Auditoría**. Filtre por fechas (máximo 366 días), usuario, acción, entidad o texto; pulse un evento para ver su detalle
**[CAPTURA 38]**; **Exportar CSV** descarga hasta 10 000 filas. La auditoría es de solo lectura: no se puede editar ni borrar.

## 3. Funcionario de atención (ATENCION)

### 3.1 Mi panel **[CAPTURA 7]**
Sus citas de hoy por estado, sus próximas citas con el botón **Atender**, y accesos rápidos a "Nueva cita" y "Nuevo contribuyente".

### 3.2 Registrar un contribuyente **[CAPTURAS 12 a 14]**
1. Contribuyentes → **Nuevo contribuyente**.
2. Elija **Natural** (DNI de 8 dígitos) o **Jurídica** (RUC de 11 dígitos que empieza con 10 o 20).
3. Complete nombre o razón social; teléfono (9 dígitos, empieza con 9), correo y dirección son opcionales; la fecha de nacimiento solo aplica a personas naturales.
4. **Guardar**. Los campos con error se marcan en rojo. Verá "Contribuyente registrado correctamente." **[CAPTURA 13]**.

### 3.3 Buscar, editar y dar de baja **[CAPTURAS 10, 11, 15]**
- Escriba un nombre, razón social, DNI o RUC en el buscador. Active **Ver también dados de baja** para incluirlos.
- **Editar:** el tipo y el DNI/RUC no se pueden cambiar.
- **Dar de baja:** pida confirmación; el contribuyente no se borra, solo deja de aparecer y no admite citas nuevas. Se puede **reactivar** desde su ficha.

### 3.4 Ficha del contribuyente **[CAPTURAS 16 a 18]**
Pulse el nombre o **Ficha**: datos personales, cuántas citas tiene por estado, su próxima cita y el historial. **Imprimir** genera una hoja sin
logo. Desde la ficha puede crear una **Nueva cita** con el contribuyente ya elegido. Abrir una ficha queda registrado en la auditoría.

### 3.5 Programar una cita **[CAPTURAS 29 a 31]**
1. Citas → **Nueva cita**.
2. Escriba al menos 3 letras del contribuyente y elíjalo.
3. Elija el **área** y el **funcionario** (solo aparecen los de esa área).
4. Elija la **fecha** (de hoy a 90 días, de lunes a viernes). El sistema muestra los turnos libres y ocupados (08:00 a 16:30, cada 30 minutos).
5. Elija la hora, la ventanilla (1 a 10) y escriba el motivo. **Guardar**.
- Si el funcionario ya tiene una cita a esa hora, verá "El funcionario ya tiene una cita el ... a las ... Elija otro horario." y deberá elegir otro turno **[CAPTURA 31]**.

### 3.6 Reprogramar, atender o cancelar **[CAPTURA 32]**
En el listado de citas del día: **Reprogramar** (cambia fecha, hora y ventanilla), **Atender** (solo el día de la cita o después) o **Cancelar**
(libera el horario). Una cita atendida o cancelada ya no se puede modificar.

### 3.7 Reporte de citas **[CAPTURA 33]**
Citas → **Reportes**: elija el rango de fechas (máximo 366 días) y vea las citas por área y por funcionario; **CSV** descarga el reporte.

## 4. Encargado de almacén (ALMACEN)

### 4.1 Mi panel **[CAPTURA 8]**
Indicadores del almacén, los 5 materiales con mayor faltante (con acceso a "Registrar entrada"), salidas por área del mes y últimos movimientos.

### 4.2 Gestionar materiales **[CAPTURAS 19, 20]**
- **Nuevo material:** código con formato `AAA-999` (por ejemplo MAT-006), nombre, unidad (UNIDAD, PAQUETE, BOLSA, CAJA, KILO, LITRO, METRO o GALON) y stock mínimo. El stock inicial es 0.
- **Editar:** el código y el stock no se editan (el stock solo cambia con entradas y salidas).
- **Dar de baja:** con confirmación; el material no se borra.
- **Ver solo stock crítico:** filtra los materiales con stock igual o menor al mínimo (insignia roja "Crítico").

### 4.3 Registrar una entrada o una salida **[CAPTURAS 21 a 23]**
1. En la fila del material pulse **Entrada** o **Salida**.
2. Escriba la **cantidad** (entero de 1 a 100 000). En una **salida** elija además el **área solicitante**. La observación es opcional.
3. Pulse **Confirmar**. El aviso verde muestra el stock actual y la fila se actualiza sin recargar la página.
- Si pide más de lo que hay, aparece un aviso rojo "Stock insuficiente: solo hay N unidades disponibles." y **no se cambia nada**.

### 4.4 Historial y reportes **[CAPTURAS 24 a 28]**
- **Historial:** botón en la fila del material; lista entradas y salidas con fecha, tipo, cantidad, área y usuario.
- **Reportes:** stock crítico, movimientos por fechas (con filtros de tipo, área y material), resumen por material y salidas por área; todos con exportación CSV.

## 5. Mensajes de error más comunes y qué significan

| Mensaje | Qué significa | Qué hacer |
|---|---|---|
| "Usuario o contraseña incorrectos." | Datos de acceso erróneos (el sistema no dice cuál para proteger las cuentas) | Revise usuario y contraseña; tras 5 intentos la cuenta se bloquea |
| "La cuenta está bloqueada. Comuníquese con el administrador." | La cuenta alcanzó los 5 intentos fallidos o fue desactivada | Pida a un administrador que la desbloquee |
| "Su contraseña es temporal: debe cambiarla ahora para poder usar el sistema." | Tiene una contraseña temporal | Cámbiela en la pantalla que se abre |
| "La sesión expiró. Vuelva a iniciar sesión." | Pasaron 30 minutos sin actividad (o se cerró la sesión) | Inicie sesión de nuevo |
| Página "Acceso denegado" (403) | Su rol no puede usar esa sección, o la página estaba abierta con un permiso que ya no es válido | Vuelva al menú y use una opción permitida; si el problema continúa, inicie sesión de nuevo |
| "El DNI debe tener exactamente 8 dígitos." / "El RUC debe tener 11 dígitos y empezar con 10 o 20." | Documento mal escrito | Corríjalo |
| "Ya existe un contribuyente con ese DNI/RUC" | El documento ya está registrado | Búsquelo en el listado (puede estar dado de baja: actívelo "Ver también dados de baja" y reactívelo) |
| "Ya existe un material con ese código." | Código repetido | Use otro código |
| "Stock insuficiente: solo hay N unidades disponibles." | La salida pide más que el stock | Reduzca la cantidad |
| "El material no existe o está dado de baja." | El material ya no está disponible | Actualice la página |
| "El funcionario ya tiene una cita el dd/MM/aaaa a las HH:mm. Elija otro horario." | Cruce de horario | Elija otro turno |
| "Solo se atiende de lunes a viernes." / "La hora debe estar entre 08:00 y 16:30." | Fecha u hora fuera del horario de atención | Elija una fecha y hora válidas |
| "El contribuyente está dado de baja." | No se puede dar cita a un contribuyente dado de baja | Reactívelo desde su ficha |
| "Una cita CANCELADA ya no se puede modificar." | La cita ya no está programada | Cree una cita nueva si hace falta |
| "Siempre debe quedar al menos un administrador activo." | Intentó desactivar o quitar el rol al último administrador | Cree o active otro administrador antes |
| "No se pudo completar la operación. Intente nuevamente." | Error interno (el detalle queda en el registro del servidor) | Reintente; si persiste, avise al administrador |
| "Ocurrió un error. No se pudo completar la operación. Intente de nuevo o avise al administrador." | Error no previsto o problema de conexión con la base de datos | Avise al administrador con la hora aproximada |

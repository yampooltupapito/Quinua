# 04. Procesos críticos (Capítulo III, sección 3.5)

Cada proceso se describe con su diagrama de actividades (texto PlantUML en `diagramas/`) y las clases reales que lo ejecutan.
Todos los diagramas siguen el código tal como está hoy; no se agregó ningún paso que el código no tenga.

## 1. Registrar una salida de material con transacción y rollback
- **Diagrama de actividades:** `diagramas/actividad_salida_material.puml`
- **Diagrama de secuencia:** `diagramas/secuencia_salida_material.puml` (JSP, jQuery/Ajax, filtros, Servlet, DAO y MySQL)
- **Clases:** `almacen/listado.jsp`, `assets/js/movimientos.js`, `MovimientoServlet`, `MovimientoDAOImpl`, `StockInsuficienteException`,
  `MaterialNoDisponibleException`, `AuditoriaDAOImpl`.
- **Por qué es crítico:** toca tres tablas (`movimiento_stock`, `material`, `auditoria`) y no puede quedar a medias.
- **Pasos clave del DAO (`registrarMovimiento`):**
  1. `setAutoCommit(false)` inicia la transacción.
  2. `SELECT stock FROM material WHERE id_material = ? AND estado = 1 FOR UPDATE` bloquea la fila: dos salidas simultáneas no pueden
     descontar de más.
  3. Si es salida y `stock < cantidad` se lanza `StockInsuficienteException`.
  4. `INSERT` en `movimiento_stock`, `UPDATE material SET stock = stock - ?` y `INSERT` en `auditoria` con la **misma conexión**.
  5. `commit()`. Si cualquier paso falla: `rollback()`; en el `finally` se restaura `autoCommit`.
- **Respuesta JSON:** `{"ok":true,"mensaje":"...","stockActual":N,"critico":false}` o `{"ok":false,"mensaje":"..."}`; un error de
  base de datos responde 500 con un mensaje genérico y el detalle va al log.
- **Pruebas que lo verifican:** `MovimientoDAOTest` (14 pruebas, entre ellas `salidaMayorAlStockLanzaStockInsuficienteYNoCambiaNada`,
  `rollbackSiFallaElSegundoPasoNoDejaNadaGuardado`, `siFallaElMovimientoNoQuedaAuditoria`).

## 2. Programar una cita con la regla de no cruce de horarios
- **Diagrama de actividades:** `diagramas/actividad_programar_cita.puml`
- **Clases:** `citas/formulario.jsp`, `assets/js/citas.js`, `CitaAjaxServlet`, `CitaServlet`, `CitaValidador`, `CitaDAOImpl`,
  `CitaCruzadaException`.
- **Regla de negocio:** un funcionario no puede tener dos citas **no canceladas** en la misma fecha y hora; una cita cancelada libera el
  horario.
- **Doble defensa:**
  1. *Defensa 1 (consulta previa):* `hayCruce` cuenta las citas del funcionario en esa fecha y hora con `estado <> 'CANCELADA'`
     (al reprogramar excluye la propia cita).
  2. *Defensa 2 (la base de datos):* índice `UNIQUE (id_funcionario, fecha, hora_activa)`, donde `hora_activa` es una columna generada
     que vale `NULL` si la cita está cancelada (y `NULL` no choca en un `UNIQUE`). Si dos usuarios guardan a la vez, la base rechaza al
     segundo con el error 1062, y el DAO lo traduce a `CitaCruzadaException`.
- **Validaciones de `CitaValidador`:** contribuyente existente y activo; área válida; funcionario de esa área; fecha no anterior a hoy,
  no mayor a 90 días y de lunes a viernes; hora entre 08:00 y 16:30 en intervalos de 30 minutos (y no pasada si es hoy); ventanilla de 1
  a 10; motivo obligatorio de hasta 200 caracteres.
- **Pruebas que lo verifican:** `CitaDAOTest` (16) y `CitaValidadorTest` (44), por ejemplo
  `elMismoFuncionarioFechaYHoraSeRechazaPorCruce`, `cancelarUnaCitaLiberaElHorario`,
  `siFallaLaConsultaPreviaElUniqueDeLaBaseTambienRechazaElCruce`.

## 3. Registrar un contribuyente con validaciones
- **Diagrama de actividades:** `diagramas/actividad_registrar_contribuyente.puml`
- **Clases:** `contribuyentes/formulario.jsp`, `assets/js/contribuyentes.js`, `ContribuyenteServlet`, `ContribuyenteValidador`,
  `Validador`, `ContribuyenteDAOImpl`, `RegistroDuplicadoException`, `Auditoria`.
- **Validaciones:** documento según el tipo de persona (DNI de 8 dígitos o RUC de 11 que empieza con 10 o 20); nombre de 3 a 150
  caracteres; teléfono de 9 dígitos que empieza con 9; correo; dirección de hasta 200; fecha de nacimiento solo para naturales, no futura
  ni anterior a 1900. Se validan en el navegador (jQuery) **y** en el servidor.
- **Resultado:** Post-Redirect-Get con el mensaje "Contribuyente registrado correctamente."; en caso de DNI/RUC repetido, el formulario
  vuelve con el error y los datos escritos.
- **Pruebas que lo verifican:** `ContribuyenteValidadorTest` (20), `ValidadorTest` (57), `ContribuyenteDAOTest` (9).

## 4. Inicio de sesión con control de intentos
- **Diagrama de actividades:** `diagramas/actividad_inicio_sesion.puml`
- **Clases:** `login.jsp`, `assets/js/login.js`, `LoginServlet`, `UsuarioDAOImpl`, `Seguridad`, `Validador`, `AuthFilter`,
  `CsrfFilter` (ver nota), `Auditoria`.
- **Reglas:**
  - El mismo mensaje para usuario inexistente, contraseña incorrecta o formato inválido.
  - Si el usuario no existe se verifica contra un hash de relleno para que la respuesta tarde lo mismo.
  - Solo las cuentas activas suman intentos; al 5.º intento la cuenta se bloquea (`estado = 0`).
  - El aviso de cuenta bloqueada solo se muestra con la contraseña correcta.
  - Sesión nueva en cada inicio (evita fijación de sesión) y el hash no se guarda en la sesión.
  - Un hash antiguo se migra a PBKDF2 en el primer inicio correcto.
- **Nota:** `/login` es la única ruta POST sin token CSRF (el usuario aún no tiene sesión).
- **Pruebas que lo verifican:** `UsuarioDAOTest` (`alQuintoIntentoFallidoLaCuentaSeBloquea`, `cadaIntentoFallidoSumaUno`,
  `usuarioConComillasNoRompeLaConsultaNiDevuelveDatos`), `SeguridadTest` (30) y el script `pruebas-seguridad.ps1` (casos 5 a 8, 13 y 14).

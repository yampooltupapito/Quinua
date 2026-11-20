# 10. Lista de capturas de pantalla a tomar

Instrucciones generales:
- Entrar a `http://localhost:8080/sigmud/` (inicie con `.\desplegar.ps1`). Usuarios de ejemplo de una instalación nueva: `admin` (ADMIN),
  `atencion1` y `atencion2` (ATENCION), `almacen1` (ALMACEN). Las contraseñas de ejemplo están en `sql/02_tablas_sigmud.sql`; **no las
  escriba en la monografía** y, si cambió las contraseñas, use las suyas.
- Resolución sugerida: ventana de 1366 × 768 o superior, zoom 100 %. Formato PNG.
- Antes de las capturas de listados, registre algunos datos de ejemplo (más de 10 contribuyentes para ver la paginación).
- No muestre datos personales de personas reales: use solo datos de prueba (o tape los datos en la imagen).
- Los nombres de archivo siguen el patrón `fig_NN_descripcion.png`. La columna "Sección" es una propuesta; ajuste al índice de su monografía.

| N.º | Pantalla | Rol con el que entrar | Qué debe verse | Archivo sugerido | Sección propuesta |
|---|---|---|---|---|---|
| 1 | Inicio de sesión, tema claro | Sin sesión | Formulario con usuario y contraseña, logo o ícono, botón de tema | `fig_01_login_claro.png` | 4.6 / Anexo C |
| 2 | Inicio de sesión, tema oscuro | Sin sesión | La misma pantalla tras pulsar el botón de tema | `fig_02_login_oscuro.png` | 4.6 |
| 3 | Error de inicio de sesión | Sin sesión | Mensaje "Usuario o contraseña incorrectos" tras escribir una clave errónea | `fig_03_login_error.png` | 4.6 |
| 4 | Cuenta bloqueada tras 5 intentos | Sin sesión | Aviso de cuenta bloqueada (se obtiene con la contraseña **correcta** de una cuenta ya bloqueada) | `fig_04_cuenta_bloqueada.png` | 4.6 |
| 5 | Cambio obligatorio de contraseña | Usuario nuevo con clave temporal | Pantalla de cambio con el aviso "su contraseña es temporal" | `fig_05_cambio_clave_temporal.png` | 4.6 |
| 6 | Panel del administrador | `admin` | Indicadores, gráficos, lista de cuentas bloqueadas y últimos accesos | `fig_06_panel_admin.png` | 4.7 / Anexo C |
| 7 | Panel del funcionario de atención | `atencion1` | Sus indicadores de citas de hoy, próximas citas y gráfico | `fig_07_panel_atencion.png` | Anexo C |
| 8 | Panel del encargado de almacén | `almacen1` | Indicadores, materiales para reponer, salidas por área, últimos movimientos | `fig_08_panel_almacen.png` | Anexo C |
| 9 | Panel del administrador en tema oscuro | `admin` | Gráficos redibujados con los colores del tema oscuro | `fig_09_panel_admin_oscuro.png` | Anexo C |
| 10 | Listado de contribuyentes | `atencion1` | Tabla con paginación, encabezados ordenables y buscador | `fig_10_contribuyentes_listado.png` | 4.5 |
| 11 | Búsqueda de contribuyentes | `atencion1` | Resultado de buscar un apellido o DNI | `fig_11_contribuyentes_busqueda.png` | 4.5 |
| 12 | Formulario de nuevo contribuyente con errores | `atencion1` | Campos en rojo al escribir DNI de 7 dígitos | `fig_12_contribuyente_validacion.png` | 4.4 |
| 13 | Registro exitoso | `atencion1` | Aviso verde "Contribuyente registrado correctamente." | `fig_13_contribuyente_exito.png` | 4.4 |
| 14 | DNI duplicado | `atencion1` | Mensaje "Ya existe un contribuyente con ese DNI/RUC" con los datos conservados | `fig_14_contribuyente_duplicado.png` | 4.4 |
| 15 | Baja lógica | `atencion1` | Ventana de confirmación de "Dar de baja" y luego el listado con "Ver también dados de baja" | `fig_15_contribuyente_baja.png` | 4.3 |
| 16 | Ficha del contribuyente | `admin` | Datos, indicadores de citas, próxima cita, historial y pestaña Actividad | `fig_16_ficha_admin.png` | 4.5 |
| 17 | Ficha vista por ATENCION | `atencion1` | La misma ficha **sin** la pestaña Actividad | `fig_17_ficha_atencion.png` | 4.5 |
| 18 | Ficha lista para imprimir | `admin` | Vista previa de impresión en tema claro con usuario y hora en la hoja | `fig_18_ficha_impresion.png` | 4.5 |
| 19 | Listado de materiales | `almacen1` | Tabla de materiales con insignias Normal/Crítico | `fig_19_materiales_listado.png` | 4.5 |
| 20 | Formulario de material | `almacen1` | Alta de material con unidad en lista y stock mínimo | `fig_20_material_formulario.png` | 4.4 |
| 21 | Entrada de stock | `almacen1` | Ventana "Entrada" y luego el aviso verde con el stock actualizado en la fila | `fig_21_entrada_stock.png` | 4.7 |
| 22 | **Transacción de almacén con éxito** (salida) | `almacen1` | Ventana de salida con área y el aviso verde "Salida registrada correctamente. Stock actual: N." | `fig_22_salida_exito.png` | 4.7 |
| 23 | **Transacción de almacén con error** (stock insuficiente) | `almacen1` | Aviso rojo con "solo hay N" al pedir más que el stock; luego mostrar que el stock no cambió | `fig_23_salida_error_stock.png` | 4.7 |
| 24 | Historial de movimientos de un material | `almacen1` | Tabla paginada con fecha, tipo, cantidad, área y usuario | `fig_24_historial_movimientos.png` | 4.7 |
| 25 | Reporte de stock crítico | `almacen1` | Tabla con faltante y botón de exportar CSV | `fig_25_reporte_stock_critico.png` | 4.8 |
| 26 | Reporte de movimientos con filtros | `almacen1` | Filtros de fechas, tipo, área y material, con resultados | `fig_26_reporte_movimientos.png` | 4.8 |
| 27 | Reporte resumen por material | `almacen1` | Entradas, salidas, saldo y verificación de stock | `fig_27_reporte_resumen.png` | 4.8 |
| 28 | Reporte de salidas por área | `almacen1` | Tabla de salidas agrupadas por área | `fig_28_reporte_salidas_area.png` | 4.8 |
| 29 | Listado de citas del día | `atencion1` | Citas por fecha y área con acciones Atender, Reprogramar y Cancelar | `fig_29_citas_listado.png` | 4.5 |
| 30 | Nueva cita con turnos | `atencion1` | Formulario con contribuyente, área, funcionario y cuadrícula de turnos libres/ocupados | `fig_30_cita_formulario.png` | 4.4 |
| 31 | **Regla de cruce de citas** | `atencion1` | Mensaje "El funcionario ya tiene una cita el ... a las ... Elija otro horario." con el formulario conservado | `fig_31_cita_cruce.png` | 4.8 |
| 32 | Cita atendida y cancelada | `atencion1` | Listado con estados ATENDIDA y CANCELADA | `fig_32_cita_estados.png` | 4.5 |
| 33 | Reporte de citas | `atencion1` | Cuadros por área y por funcionario con fila TOTAL | `fig_33_reporte_citas.png` | 4.8 |
| 34 | Gestión de usuarios | `admin` | Listado con roles, estados y botones Restablecer y Desbloquear | `fig_34_usuarios_listado.png` | Anexo C |
| 35 | Nuevo usuario y clave temporal | `admin` | Formulario y el cuadro que muestra la contraseña temporal una sola vez (**tape la contraseña en la imagen**) | `fig_35_usuario_nuevo.png` | Anexo C |
| 36 | Mi perfil | cualquiera | Datos del usuario y enlace de cambio de contraseña | `fig_36_perfil.png` | Anexo C |
| 37 | **Auditoría** | `admin` | Tabla con filtros, eventos (LOGIN_OK, CREAR, MOVIMIENTO_STOCK, ...) y botón de CSV | `fig_37_auditoria.png` | 4.6 |
| 38 | Detalle de un evento de auditoría | `admin` | Ventana con el detalle campo por campo | `fig_38_auditoria_detalle.png` | 4.6 |
| 39 | Acceso denegado (403) | `almacen1` | Abrir `/sigmud/contribuyentes` y mostrar la página propia de error | `fig_39_error_403.png` | 4.6 |
| 40 | Página no encontrada (404) | cualquiera | Página de error propia, sin versión de Tomcat | `fig_40_error_404.png` | 4.6 |
| 41 | Avisos (toasts) | cualquiera | Un aviso verde y uno rojo visibles | `fig_41_toasts.png` | Anexo C |
| 42 | Orden y paginación | `atencion1` | Encabezado con flecha de orden y selector "Por página" | `fig_42_orden_paginacion.png` | 4.5 |
| 43 | Cabeceras de seguridad | cualquiera | Herramientas del navegador (pestaña Red) mostrando `Content-Security-Policy` y las demás cabeceras | `fig_43_cabeceras_seguridad.png` | 4.6 |
| 44 | Resultado de las pruebas JUnit | — | Terminal con `mvn test` terminando en "Tests run: 376, Failures: 0, Errors: 0" (o la salida de NetBeans) | `fig_44_junit.png` | Cap. V / Anexo F |
| 45 | Resultado del script de seguridad | — | Terminal con `.\pruebas-seguridad.ps1` terminando en "207 APROBADAS, 0 FALLIDAS" | `fig_45_script_seguridad.png` | Cap. V / Anexo F |
| 46 | Estructura del proyecto | — | Vista del proyecto en NetBeans con los paquetes | `fig_46_estructura_proyecto.png` | 4.2 |
| 47 | Base de datos | — | Las 7 tablas en MySQL Workbench (o el resultado de `SHOW TABLES`) | `fig_47_tablas_bd.png` | 3.6 |
| 48 | Triggers de auditoría | — | `SHOW TRIGGERS` y el error al intentar un `UPDATE` sobre `auditoria` | `fig_48_triggers_auditoria.png` | 3.6 |

> **Cifras de las pruebas:** las capturas 44 y 45 mostrarán lo que usted obtenga al ejecutarlas. Al generar esta documentación
> (2026-11-19) los resultados reales fueron 376 pruebas JUnit sin fallos y 207 de 207 comprobaciones del script, en una copia aislada
> con base temporal; ejecute usted las pruebas en su equipo para la captura.

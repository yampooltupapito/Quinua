# SIGMUD – Reglas del proyecto

Sistema **web** (Servlet + JSP) de la Municipalidad Distrital de Quinua. No es de escritorio: nada de Swing ni JPA.
Responder siempre en español. El estudiante debe poder explicar y modificar todo el código en su sustentación:
código simple, métodos cortos (≤ 25 líneas) y comentarios útiles.

## Entorno
- Tomcat 10.1 (Servlet 6.0, Jakarta EE 10): usar `jakarta.*`, nunca `javax.servlet`/`javax.*` de Java EE
  (`javax.crypto` es de la biblioteca estándar de Java y sí se usa en `Seguridad`).
- JDK 25 instalado; el proyecto compila con `maven.compiler.release=17`.
- MySQL 8.4, esquema `mun_quinua` (y `mun_quinua_test` para las pruebas), JDBC puro (`mysql-connector-j`).
  La contraseña de la base va solo en `db.properties`.
- Paquete base `pe.gob.munquinua.sigmud`: `model`, `dao`, `dao.impl`, `controller`, `filter`, `listener`, `util`, `exception`.
- Vistas en `WEB-INF/views`, estáticos locales en `webapp/assets` (Bootstrap 5, jQuery, Bootstrap Icons, Chart.js). Sin CDN.
- Roles: `ADMIN`, `ATENCION`, `ALMACEN`.

## Reglas obligatorias
1. Todo SQL con `PreparedStatement`; nunca concatenar datos del usuario.
2. `Connection`, `PreparedStatement` y `ResultSet` en try-with-resources.
3. JSP con JSTL (`jakarta.tags.core`, `jakarta.tags.fmt`); sin scriptlets.
4. Post-Redirect-Get después de cada POST exitoso.
5. Validar en el Servlet (regex en `Validador`) y también con jQuery en el navegador.
6. Errores al usuario en lenguaje claro, sin detalles técnicos; el detalle va al log del servidor.
7. Si una operación toca más de una tabla: transacción (`setAutoCommit(false)`, `commit()`, `rollback()` en el catch,
   restaurar autoCommit).
8. Las operaciones transaccionales se llaman con `$.ajax` y devuelven JSON (Gson) con la forma
   `{"ok":true|false,"mensaje":"..."}`.
9. **Contraseñas:** solo como hash **PBKDF2-HMAC-SHA256** con sal aleatoria de 16 bytes y 120000 iteraciones, en el formato
   de texto `pbkdf2$iteraciones$salBase64$hashBase64` (clase `Seguridad`: `hashear`, `verificar`, `esLegado`). Se comparan en
   tiempo constante (`MessageDigest.isEqual`). Los hashes antiguos (SHA-256 sin sal, 64 hexadecimales) siguen verificando y
   se migran al formato nuevo en el siguiente inicio de sesión. El hash nunca va a la sesión ni a la pantalla, y las
   contraseñas (ni temporales ni definitivas) nunca se escriben en el log. Política: mínimo 8 caracteres, una mayúscula,
   una minúscula, un número y distinta del nombre de usuario (`Validador.politicaPassword`).
10. Baja lógica con el campo `estado` en contribuyentes y materiales; en usuarios solo se activa o desactiva (nunca se
    borra, porque tiene movimientos y citas asociados). Sin DELETE físico (salvo en pruebas).
11. Control de acceso por rol en `AuthFilter` (mapa de roles por ruta) y en el menú; las reglas de negocio importantes
    (por ejemplo "siempre debe quedar un ADMIN activo") se verifican en el servidor, no solo en pantalla.
12. Convenciones Java: PascalCase para clases, camelCase para métodos y variables, MAYÚSCULAS para constantes.
13. **Seguridad web:** todo POST (menos `/login`) exige el token CSRF (`csrf` o cabecera `X-CSRF-Token`); cabeceras de
    seguridad y CSP `script-src 'self'` → **nada de JavaScript dentro del HTML**, todo en archivos `.js` externos; el
    rol y el id del usuario salen SIEMPRE de la sesión, nunca de un parámetro.

14. **Interfaz:** listados con `Pagina<T>` + `Paginador` (LIMIT/OFFSET como parámetros); el ORDEN por columna solo con la lista blanca
    `OrdenSql`/`ORDEN` de cada DAO (nunca texto del usuario en el SQL); mensajes al usuario con `mostrarToast` (`toast.js`) y flash en `data-*`
    (los errores de validación van junto a los campos); colores propios como variables de `sigmud.css` (claro y oscuro, contraste AA);
    cada página usa `fragmentos/cabecera.jsp` y `<main id="contenido">`; el logo `assets/img/logo.png` es opcional (si falta, ícono). Nada de
    JavaScript, `<style>` ni CDN dentro del HTML.

## Base de datos (tablas)
`area`, `usuario`, `contribuyente`, `material`, `movimiento_stock`, `cita`, `auditoria`.
- `usuario`: `password_hash VARCHAR(255)`, `debe_cambiar_password` (1 = contraseña temporal: solo puede usar
  `/perfil/password`, `/logout` y `/assets` hasta cambiarla), `intentos_fallidos` (5 = cuenta bloqueada).
- `contribuyente`: DNI de 8 dígitos si es NATURAL, RUC de 11 si es JURIDICA; baja lógica con `estado`.
- `material`: `stock >= 0` por CHECK; `movimiento_stock.tipo` es ENTRADA o SALIDA.
- `cita`: UNIQUE (id_funcionario, fecha, hora_activa); una cita CANCELADA no bloquea el horario.
- Scripts: `sql/02_tablas_sigmud.sql` (instalación nueva), `sql/03_bd_pruebas.sql` (base de pruebas),
  `sql/04_usuarios.sql` (cambios de gestión de usuarios para una base existente),
  `sql/05_auditoria.sql` (tabla `auditoria` y sus triggers; se ejecuta como root).

- `auditoria`: registro INMUTABLE (triggers impiden UPDATE/DELETE). Todo cambio importante se registra con
  `Auditoria.registrar(req, accion, entidad, id, descripcion, detalle)` DESPUÉS de que la operación tuvo éxito (nunca lanza
  excepción; nunca se pasan contraseñas ni hashes). Excepción: `MOVIMIENTO_STOCK` se guarda dentro de la transacción del
  movimiento (`AuditoriaDAO.registrar(Connection, …)`). Un módulo nuevo debe auditar sus altas, cambios y bajas.
  Abrir la ficha de un contribuyente registra `CONSULTA_FICHA` (datos personales, Ley N.º 29733).

## Procedimiento para crear un módulo
Leer este archivo y `docs/avance.md`; `model` → `dao` → `dao.impl` → `controller` → vistas → filtro/menú → transacción y
Ajax si aplica → reporte si aplica → probar (feliz, inválidos, sin sesión, rol sin permiso, inyección SQL) → documentar
en `docs/avance.md` (archivos, cómo probar y casos de prueba).

## Pruebas
- `mvn test` (JUnit 5): las pruebas de validadores y `Seguridad` no usan base de datos; las de DAO usan SOLO
  `mun_quinua_test` (`src/test/resources/db.properties`) y se niegan a correr contra otra base.
- `pruebas-seguridad.ps1` prueba en vivo (con Tomcat encendido) accesos, CSRF, cabeceras, XSS, roles y contraseñas.
- Documentos: `docs/avance.md` (avance y casos de prueba) y `docs/auditoria.md` (auditoría de seguridad).

## Fuera de alcance
Trámite documentario, tributos, mercado municipal, registro civil y Defensa Civil: no construirlos; solo mencionarlos
como mejoras futuras.

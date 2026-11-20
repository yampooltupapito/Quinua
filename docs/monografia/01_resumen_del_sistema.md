# 01. Resumen del sistema (datos para la Introducción y el Resumen)

> Todo lo que sigue se midió o se leyó del proyecto real (`C:\Users\ASUS\Desktop\quinua\quinua`) el 2026-11-19. Donde un dato no se pudo
> confirmar se indica "por confirmar".

## 1. Ficha del sistema

| Campo | Dato |
|---|---|
| Nombre | **SIGMUD** (nombre del proyecto Maven: `SIGMUD`, artefacto `pe.gob.munquinua:sigmud:1.0`, empaquetado WAR) |
| Institución | Municipalidad Distrital de Quinua (nombre tomado del paquete `pe.gob.munquinua` y de `CLAUDE.md`) |
| Tipo de sistema | Aplicación **web** (Servlet 6.0 + JSP/JSTL), arquitectura MVC por capas con patrón DAO y JDBC puro. No es de escritorio y no usa JPA/Hibernate |
| Objetivo (según `CLAUDE.md` y los módulos implementados) | Apoyar la gestión municipal en tres frentes: **atención al contribuyente** (padrón y citas), **almacén** (materiales y stock con movimientos de entrada y salida) y **administración y seguridad** (usuarios, paneles de control, auditoría) |
| Idioma de la interfaz | Español |
| Base de datos | MySQL, esquema `mun_quinua` (y `mun_quinua_test` solo para pruebas) |
| Contenedor | Apache Tomcat 10.1 (Jakarta EE 10) |
| Alcance declarado como fuera del sistema | Trámite documentario, tributos, mercado municipal, registro civil y Defensa Civil (`CLAUDE.md`: "solo mencionarlos como mejoras futuras") |

> El **objetivo general** que se escriba en la monografía debe tomarse del documento del proyecto del curso. El texto de arriba solo
> resume lo que el sistema realmente hace; ver `observaciones.md` (punto sobre el documento original).

## 2. Módulos y actores

| Módulo | Qué hace (implementado) | Actor(es) con acceso |
|---|---|---|
| **Acceso y seguridad** | Inicio y cierre de sesión, bloqueo por 5 intentos, contraseña temporal con cambio obligatorio, "Mi perfil", protección CSRF, cabeceras y CSP | Todos |
| **Contribuyentes** | Registro, edición, baja lógica y reactivación; búsqueda; listado paginado y ordenable; **ficha** con indicadores, historial de citas y actividad | ADMIN y ATENCION (la pestaña "Actividad" solo ADMIN) |
| **Almacén** | Materiales (alta, edición, baja lógica, stock crítico), **entradas y salidas de stock con transacción** (Ajax + JSON), historial de movimientos, 4 reportes con exportación CSV | ADMIN y ALMACEN |
| **Citas** | Programar, reprogramar, atender y cancelar citas con regla de no cruce de horarios, listado por fecha y área, reporte por área y por funcionario con CSV | ADMIN y ATENCION |
| **Paneles (dashboards)** | Un panel distinto por rol con indicadores, gráficos (Chart.js) y listas que se actualizan cada 60 s | Cada rol ve el suyo |
| **Gestión de usuarios** | Crear usuarios con contraseña temporal, editar, activar/desactivar, restablecer contraseña, desbloquear cuentas; siempre debe quedar un ADMIN activo | ADMIN |
| **Auditoría** | Registro **inmutable** (triggers) de quién hizo qué y cuándo; consulta con filtros y exportación CSV | ADMIN (solo lectura) |
| **Interfaz** | Paginación y orden en el servidor, avisos (toasts), modo claro/oscuro, accesibilidad básica, logo opcional | Todos |

Actores (roles de la base de datos, `usuario.rol`): **ADMIN** (Administrador), **ATENCION** (Funcionario de atención) y **ALMACEN**
(Encargado de almacén). Ver `03_actores_y_casos_de_uso.md`.

## 3. Estadísticas reales

### 3.1 Base de datos
| Dato | Valor | Fuente |
|---|---|---|
| Tablas | **7**: `area`, `usuario`, `contribuyente`, `material`, `movimiento_stock`, `cita`, `auditoria` | `sql/02_tablas_sigmud.sql` |
| Triggers | **2** (`trg_auditoria_no_update`, `trg_auditoria_no_delete`) | `sql/05_auditoria.sql` |
| Claves foráneas | 8 (`fk_usuario_area`, `fk_mov_area`, `fk_mov_material`, `fk_mov_usuario`, `fk_cita_area`, `fk_cita_contrib`, `fk_cita_func`, `fk_auditoria_usuario`) | `sql/02_tablas_sigmud.sql` |
| Restricciones CHECK | 3 (`ck_contrib_doc`, `ck_material_stock`, `ck_mov_cantidad`) | `sql/02_tablas_sigmud.sql` |
| Scripts SQL | 4 archivos, 535 líneas en total (197 + 196 + 43 + 99) | `sql/` |

### 3.2 Clases Java (carpeta `src/main/java/pe/gob/munquinua/sigmud`)
| Paquete | Clases / interfaces | Líneas |
|---|---:|---:|
| `model` | 17 | 732 |
| `dao` (interfaces) | 10 | 436 |
| `dao.impl` | 10 | 2 408 |
| `controller` (Servlets) | 16 | 3 018 |
| `filter` | 3 | 197 |
| `listener` | 2 | 73 |
| `util` | 14 | 1 448 |
| `exception` | 10 | 136 |
| **Total** | **82** | **8 448** |

(El total de 8 448 líneas incluye comentarios y líneas en blanco; se contó con `wc -l`.)

### 3.3 Capa web
| Dato | Valor | Fuente |
|---|---|---|
| Servlets | **16** clases con `@WebServlet` | `controller/` |
| Rutas (URL) distintas | **18** (ver `06_arquitectura.md`, sección 5) | anotaciones `@WebServlet` |
| Filtros | 3 (`SecurityHeadersFilter`, `AuthFilter`, `CsrfFilter`) | `web.xml` |
| Listeners | 2 (`SesionListener`, `ContextoListener`) | `@WebListener` |
| JSP | **29** archivos (1 en la raíz, 28 en `WEB-INF/views`, de ellos 8 son fragmentos) | `src/main/webapp` |
| Tag files | 3 (`kpi.tag`, `thOrden.tag`, `urlLista.tag`) | `WEB-INF/tags` |
| Líneas de JSP + tags | 2 962 | `wc -l` |
| JavaScript propio | 13 archivos, 1 324 líneas | `assets/js` (sin bootstrap, jquery ni chart.js) |
| CSS propio | 3 archivos (`sigmud.css`, `impresion.css`, `reportes.css`), 221 líneas | `assets/css` |

### 3.4 Código total propio (sin bibliotecas de terceros)
| Parte | Líneas |
|---|---:|
| Java de producción | 8 448 |
| Java de pruebas (21 archivos) | 3 630 |
| JSP y tags | 2 962 |
| JavaScript propio | 1 324 |
| CSS propio | 221 |
| SQL | 535 |
| **Total aproximado** | **17 120** |

### 3.5 Pruebas (ejecutadas el 2026-11-19; ver `anexos/F_evidencias_pruebas.md`)
| Dato | Valor |
|---|---|
| Pruebas JUnit (`mvn test`) | **376 ejecutadas, 0 fallos, 0 errores, 0 omitidas** (20 clases de prueba con pruebas, más `BaseDaoTest` como apoyo) |
| Casos del script de seguridad (`pruebas-seguridad.ps1`) | **207 comprobaciones: 207 APROBADAS, 0 FALLIDAS** |
| Casos manuales de `docs/avance.md` con resultado "(pendiente)" | 90 filas sin resultado (ver observaciones y Anexo F) |

## 4. Tecnologías (resumen; el detalle con fuentes está en `09_entorno_de_desarrollo.md`)
Java (release 17, JDK 25 instalado) · Jakarta Servlet 6.0 · JSP + JSTL 3.0 · Tomcat 10.1.61 · MySQL 8.4 · JDBC (`mysql-connector-j` 8.4.0) ·
Gson 2.11.0 · Bootstrap 5.3.3 · Bootstrap Icons 1.11.3 · jQuery 3.7.1 · Chart.js 4.4.3 · JUnit 5.10.2 · Maven · NetBeans.
Todo es local (sin CDN): el sistema se ve igual sin internet.

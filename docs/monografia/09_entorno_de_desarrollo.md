# 09. Entorno de desarrollo (Capítulo IV, sección 4.1)

Versiones leídas de los archivos y programas reales del equipo el 2026-11-19. Cada dato indica su fuente. Donde no se pudo confirmar se
escribe "por confirmar".

## 1. Equipo y herramientas

| Componente | Versión | Fuente |
|---|---|---|
| Sistema operativo | Windows 11 Pro, versión 10.0.26300 | Información del sistema de la sesión de trabajo |
| **JDK usado para compilar y ejecutar** | **Oracle JDK 25.0.1 (LTS)** | `java -version` y `desplegar.ps1` (`$JavaHome = C:\Program Files\Java\jdk-25`) |
| Nivel de compilación del código | **Java 17** (`maven.compiler.release=17`) | `pom.xml` |
| Otros JDK instalados en el equipo | 17.0.12, 21.0.10, 24.0.2 | Registro de programas instalados de Windows |
| Apache Maven | **3.9.9** (el que trae NetBeans, en `C:\Program Files\Apache NetBeans\java\maven`) | `mvn -v` |
| IDE | **Apache NetBeans 26** | Registro de programas instalados de Windows |
| Contenedor web | **Apache Tomcat 10.1.61** (Servlet 6.0, Jakarta EE 10) | `RELEASE-NOTES` de la carpeta de Tomcat (`C:\Users\ASUS\Documents\toncat\apache-tomcat-10.1.61`) |
| Base de datos | **MySQL Server 8.4.9** (serie 8.4 LTS) | `mysql --version`, `SELECT VERSION()` y registro de Windows |
| Cliente MySQL | `mysql.exe` 8.4.9 | `mysql --version` |
| Control de versiones | No hay repositorio Git en la carpeta del proyecto | Información del entorno (`Is a git repository: false`) |

## 2. Bibliotecas Java (Maven)

| Biblioteca | Versión | Alcance | Fuente |
|---|---|---|---|
| `jakarta.servlet:jakarta.servlet-api` | 6.0.0 | `provided` (la aporta Tomcat) | `pom.xml` |
| `jakarta.servlet.jsp.jstl:jakarta.servlet.jsp.jstl-api` | 3.0.0 | compilación y ejecución | `pom.xml` |
| `org.glassfish.web:jakarta.servlet.jsp.jstl` (implementación JSTL) | 3.0.1 | compilación y ejecución | `pom.xml` |
| `com.mysql:mysql-connector-j` (controlador JDBC) | 8.4.0 | compilación y ejecución | `pom.xml` |
| `com.google.code.gson:gson` | 2.11.0 | compilación y ejecución | `pom.xml` |
| `org.junit.jupiter:junit-jupiter` | 5.10.2 | solo pruebas (`test`) | `pom.xml` (`junit.version`) |
| `maven-surefire-plugin` | 3.2.5 | construcción | `pom.xml` |
| `maven-compiler-plugin` | 3.12.1 | construcción | `pom.xml` |
| `maven-war-plugin` | 3.4.0 | construcción | `pom.xml` |

Otros datos de `pom.xml`: `groupId pe.gob.munquinua`, `artifactId sigmud`, `version 1.0`, `packaging war`, `finalName sigmud`,
`project.build.sourceEncoding UTF-8`, `failOnMissingWebXml false`. Versión de la especificación en `web.xml`: **Servlet 6.0**.

## 3. Bibliotecas del navegador (archivos locales en `src/main/webapp/assets`, sin CDN)

| Biblioteca | Versión | Archivo | Fuente (cabecera del archivo) |
|---|---|---|---|
| Bootstrap (CSS) | 5.3.3 | `assets/css/bootstrap.min.css` | Comentario de licencia del archivo |
| Bootstrap (JS con Popper) | 5.3.3 | `assets/js/bootstrap.bundle.min.js` | Comentario de licencia del archivo |
| Bootstrap Icons | 1.11.3 | `assets/css/bootstrap-icons.min.css`, `assets/fonts/*.woff2` y `*.woff` | Comentario de licencia del archivo |
| jQuery | 3.7.1 | `assets/js/jquery.min.js` | Comentario de licencia del archivo |
| Chart.js | 4.4.3 | `assets/js/chart.umd.js` | Comentario de licencia del archivo |

## 4. Configuración relevante
| Elemento | Valor | Fuente |
|---|---|---|
| Puerto de Tomcat | 8080 | `desplegar.ps1` (`$PuertoHttp = 8080`) |
| Nombre de la aplicación (ruta de contexto) | `/sigmud` | `desplegar.ps1` (`$NombreApp`) y `pom.xml` (`finalName`) |
| Conexión a la base | `jdbc:mysql://localhost:3306/mun_quinua` con zona horaria `America/Lima`, `useSSL=false`, `characterEncoding=UTF-8` | `src/main/resources/db.properties` |
| Usuario de la base | `sigmud` | `db.properties` |
| Contraseña de la base | ******** (enmascarada) | `db.properties` |
| Base de pruebas JUnit | `mun_quinua_test` (mismo servidor) | `src/test/resources/db.properties` |
| Codificación de solicitudes y respuestas | UTF-8 | `web.xml` |
| Tiempo de sesión | 30 minutos | `web.xml` |
| Sugerencia de NetBeans (`nb-configuration.xml`) | `j2eeVersion = 11-web` | `nb-configuration.xml` |

## 5. Datos por confirmar
| Dato | Estado |
|---|---|
| Versión del controlador MySQL realmente cargada por Tomcat | Es la del `pom.xml` (8.4.0); no se inspeccionó el WAR desplegado en el servidor del usuario |
| Versión del navegador usado para las capturas | **Por confirmar** (la elige quien tome las capturas) |
| Versión exacta de la imagen de Windows del equipo de la sustentación | **Por confirmar** |
| Versión de Git, si se llegara a usar | No aplica: el proyecto no está en un repositorio |

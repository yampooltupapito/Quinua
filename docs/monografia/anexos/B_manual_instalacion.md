# Anexo B. Manual de instalación y despliegue (Windows)

Pasos para instalar SIGMUD desde cero en un equipo Windows. Las versiones son las del equipo de desarrollo (ver `09_entorno_de_desarrollo.md`);
versiones cercanas deberían servir, pero **solo estas fueron probadas**.

> **Seguridad:** en este manual las contraseñas aparecen como `<SU_CONTRASEÑA>`. Elija las suyas y no las escriba en la monografía.

## 1. Requisitos
| Programa | Versión probada | Para qué |
|---|---|---|
| JDK | 25 (el proyecto compila a nivel Java 17) | Compilar y ejecutar |
| Apache Maven | 3.9.9 (incluido en NetBeans) | Compilar y empaquetar el WAR |
| MySQL Server | 8.4 | Base de datos |
| Apache Tomcat | 10.1.x (probado 10.1.61) | Servidor de aplicaciones (**debe ser 10.1: usa `jakarta.*`; Tomcat 9 no sirve**) |
| Apache NetBeans (opcional) | 26 | Editar y ejecutar desde el IDE |

## 2. Instalar el JDK y definir `JAVA_HOME`
1. Instale el JDK (se probó Oracle JDK 25). Anote la carpeta, por ejemplo `C:\Program Files\Java\jdk-25`.
2. Defina la variable de entorno **`JAVA_HOME`** apuntando a esa carpeta (sin `\bin` al final):
   *Inicio → "Editar las variables de entorno del sistema" → Variables de entorno → Nueva*, nombre `JAVA_HOME`, valor `C:\Program Files\Java\jdk-25`.
3. Agregue `%JAVA_HOME%\bin` a la variable `Path` si desea usar `java` desde cualquier terminal.
4. Abra una terminal **nueva** y verifique: `java -version` debe mostrar la versión instalada y `echo %JAVA_HOME%` la carpeta correcta.

## 3. Instalar MySQL y crear la base
1. Instale MySQL Server 8.4 y recuerde la contraseña del usuario `root` que defina durante la instalación.
2. Abra el archivo `sql/02_tablas_sigmud.sql` y **cambie `CAMBIAR_AQUI`** (al final del archivo, en la línea `CREATE USER IF NOT EXISTS 'sigmud'...`)
   por la contraseña que usará la aplicación (`<SU_CONTRASEÑA_APP>`). Guarde el archivo.
3. Ejecute los scripts **como root**, en este orden:

| Orden | Archivo | Cuándo |
|---|---|---|
| 1 | `sql/02_tablas_sigmud.sql` | Siempre en una instalación nueva: crea la base `mun_quinua`, las 7 tablas, los triggers de auditoría, los datos de ejemplo y el usuario `sigmud` |
| 2 | `sql/03_bd_pruebas.sql` | Solo si va a ejecutar las pruebas JUnit: crea `mun_quinua_test` (necesita que el usuario `sigmud` ya exista) |
| — | `sql/04_usuarios.sql`, `sql/05_auditoria.sql` | **Solo** si ya tenía una base creada con una versión anterior (la sección 1 de cada uno). No son necesarios tras ejecutar `02` |

   Desde una terminal (desde la carpeta del proyecto):
   ```
   mysql -u root -p < sql\02_tablas_sigmud.sql
   mysql -u root -p < sql\03_bd_pruebas.sql
   ```
   o abra cada archivo en MySQL Workbench y ejecútelo completo. Los scripts con `DELIMITER $$` (los triggers) deben ejecutarse como root.
4. Compruebe: `mysql -u sigmud -p mun_quinua -e "SHOW TABLES;"` debe listar `area`, `auditoria`, `cita`, `contribuyente`, `material`,
   `movimiento_stock` y `usuario`.

## 4. Configurar `db.properties`
Archivo `src/main/resources/db.properties` (la contraseña va **solo** aquí):
```
url=jdbc:mysql://localhost:3306/mun_quinua?serverTimezone=America/Lima&useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
usuario=sigmud
password=********
```
Reemplace `********` por `<SU_CONTRASEÑA_APP>` (la misma del paso 3.2). Si MySQL usa otro puerto, cambie `3306`.
Para las pruebas JUnit, `src/test/resources/db.properties` apunta a `mun_quinua_test` con el mismo usuario.

## 5. Instalar Tomcat
1. Descomprima Tomcat 10.1 en una carpeta sin espacios en la ruta, por ejemplo `C:\Tomcat\apache-tomcat-10.1.61`.
2. Verifique que el puerto **8080** esté libre (ver "Problemas frecuentes").

## 6. Compilar con Maven y desplegar el WAR

### Opción A (recomendada): el script `desplegar.ps1`
1. Abra `desplegar.ps1` y revise solo la sección `RUTAS` del inicio: `$JavaHome`, `$Mvn` (ruta de `mvn.cmd`), `$TomcatHome` y `$NombreApp`.
2. En PowerShell, desde la carpeta del proyecto:
   ```
   .\desplegar.ps1            # compila (sin pruebas), despliega y arranca Tomcat
   .\desplegar.ps1 -Pruebas   # antes corre las pruebas JUnit; si alguna falla, no despliega
   ```
   El script compila con `mvn clean package -DskipTests`, borra el despliegue anterior, copia `target\sigmud.war` a `webapps` y arranca Tomcat
   en esa ventana (se cierra con Ctrl+C).
3. Si PowerShell bloquea el script: `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass` y vuelva a ejecutar.

### Opción B: manual
1. `mvn clean package -DskipTests` (con `JAVA_HOME` definido). Se genera `target\sigmud.war`.
2. Copie `sigmud.war` a la carpeta `webapps` de Tomcat.
3. Defina `CATALINA_HOME` (carpeta de Tomcat) y ejecute `bin\catalina.bat run` (o `bin\startup.bat`).

### Opción C: desde NetBeans
Abra la carpeta del proyecto, registre el servidor Tomcat 10.1 (*Servicios → Servidores → Agregar servidor*), ejecute el proyecto (F6).

## 7. Verificación final
1. Abra `http://localhost:8080/sigmud/`: debe mostrar el inicio de sesión.
2. Inicie sesión con `admin` (la contraseña de ejemplo está en `sql/02_tablas_sigmud.sql`; **cámbiela** desde "Mi perfil"). Debe ver el
   panel del administrador.
3. Opcional: `mvn test` (requiere `mun_quinua_test`) debe terminar con 0 fallos; `.\pruebas-seguridad.ps1` (con Tomcat encendido) debe terminar
   con 0 fallidas.
4. Opcional: copie el logo oficial a `src\main\webapp\assets\img\logo.png` (y, si quiere, `favicon.png`) y vuelva a desplegar.
   Si no hay logo se muestra un ícono genérico.

## 8. Problemas frecuentes y soluciones

| Problema | Causa probable | Solución |
|---|---|---|
| `mvn` dice "JAVA_HOME is not defined correctly" o "No compiler is provided" | `JAVA_HOME` apunta a una carpeta equivocada, a un JRE o termina en `\bin`; el equipo puede tener varios JDK (17, 21, 24, 25) | Defina `JAVA_HOME` con la carpeta raíz del JDK (la que contiene `bin` y `lib`), abra una terminal **nueva** y verifique con `echo %JAVA_HOME%` y `java -version`. En `desplegar.ps1` corrija `$JavaHome` |
| "Access denied for user 'sigmud'@'localhost'" al abrir el sistema o en el log | La contraseña de `db.properties` no coincide con la del usuario `sigmud` de MySQL, o no se cambió `CAMBIAR_AQUI` antes de ejecutar el script | Ponga la contraseña correcta en `db.properties` o cámbiela en MySQL: `ALTER USER 'sigmud'@'localhost' IDENTIFIED BY '<SU_CONTRASEÑA_APP>';` y vuelva a desplegar |
| `Communications link failure` o "Connection refused" | MySQL apagado o en otro puerto | Inicie el servicio MySQL (`services.msc`) o corrija el puerto en `url` |
| "Unknown database 'mun_quinua'" | No se ejecutó `02_tablas_sigmud.sql` | Ejecútelo como root (paso 3) |
| La pantalla muestra "Ocurrió un error: No se pudo completar la operación..." al iniciar sesión y el log dice "Unknown column 'debe_cambiar_password'" (o la tabla `auditoria` no existe) | Base creada con una versión anterior: faltan los cambios de `04` y `05` | Ejecute la sección 1 de `sql/04_usuarios.sql` y de `sql/05_auditoria.sql` (el `05` como root); es seguro repetirlos |
| `Access denied ... TRIGGER` / "You do not have the SUPER privilege and binary logging is enabled" al crear los triggers | Los triggers de `05_auditoria.sql` y `02_tablas_sigmud.sql` requieren root | Ejecute esos scripts con el usuario `root` |
| "El puerto 8080 ya está en uso" (también lo detecta `desplegar.ps1`) | Otro Tomcat, NetBeans u otro programa usa el 8080 | Cierre el otro Tomcat; ver quién lo usa con `netstat -ano \| findstr :8080`; o cambie el puerto en `conf\server.xml` (y `$PuertoHttp` en `desplegar.ps1`) |
| Error 404 en `http://localhost:8080/sigmud/` | El WAR no se desplegó o Tomcat no terminó de arrancar | Mire el log de Tomcat (`logs\catalina.<fecha>.log`); confirme que `sigmud.war` está en `webapps` y que existe la carpeta `webapps\sigmud` |
| Error `java.lang.NoClassDefFoundError: javax/servlet/...` o la aplicación no carga | Se usa Tomcat 9 o anterior | El sistema usa `jakarta.*`: instale **Tomcat 10.1** |
| Tomcat no aparece o no se puede agregar en NetBeans / el proyecto no ofrece el servidor | Posible desajuste de versión de Jakarta EE: `nb-configuration.xml` declara `11-web` (Jakarta EE 11) y Tomcat 10.1 implementa Jakarta EE 10 (Servlet 6.0). *No se verificó en el IDE al redactar este manual* | Registre Tomcat 10.1 en NetBeans (*Servicios → Servidores*) y elíjalo en *Propiedades del proyecto → Ejecutar*; si el IDE no lo ofrece, use `desplegar.ps1`, que no depende del IDE |
| Las tildes se ven como `Ã³` | Codificación | El proyecto usa UTF-8 en `pom.xml`, `web.xml` y la conexión; guarde los archivos en UTF-8 y no cambie `characterEncoding=UTF-8` |
| Después de poner el logo no aparece | El logo se detecta **al arrancar** | Reinicie Tomcat (vuelva a ejecutar `desplegar.ps1`) |
| Las pruebas DAO fallan con "se niega a correr" | Las pruebas solo corren contra una base llamada `mun_quinua_test` | Ejecute `03_bd_pruebas.sql` y no cambie el nombre en `src/test/resources/db.properties` |
| La cuenta quedó bloqueada | 5 contraseñas incorrectas | Desde otra cuenta ADMIN, use "Desbloquear" en el panel o en Usuarios |

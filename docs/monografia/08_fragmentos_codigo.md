# 08. Fragmentos de código real (Capítulo IV)

Todos los fragmentos de este archivo fueron **copiados automáticamente** del código fuente del proyecto (el rango de líneas
indicado corresponde al archivo tal como está hoy), sin reescribirlos y con un máximo de 25 líneas cada uno. Algunos empiezan
o terminan dentro de una clase o de un bloque (por ejemplo, un método suelto): compilan **dentro de su clase original**, no
como archivo independiente.

> Nota sobre la numeración: el documento del curso no se entregó al generar esto, así que las secciones 4.1 a 4.8 siguen el orden
> pedido en el encargo (entorno, estructura, capa de datos, controladores, vistas, seguridad de acceso, transacciones con Ajax y
> reportes). Si el índice de su monografía difiere, solo cambie los títulos.

---

## 4.1 Entorno de desarrollo y configuración (`pom.xml`)

### 4.1.1 Propiedades del proyecto

**Archivo:** `pom.xml` — líneas 11 a 16 (6 líneas)

```xml
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.release>17</maven.compiler.release>
        <failOnMissingWebXml>false</failOnMissingWebXml>
        <junit.version>5.10.2</junit.version>
    </properties>
```

`maven.compiler.release=17`: aunque el equipo tiene el JDK 25 instalado, el código se compila para Java 17. La codificación es UTF-8 y
`failOnMissingWebXml=false` evita un error de Maven con el empaquetado WAR.

### 4.1.2 Dependencias de acceso a datos y JSON

**Archivo:** `pom.xml` — líneas 35 a 44 (10 líneas)

```xml
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>8.4.0</version>
        </dependency>
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.11.0</version>
        </dependency>
```

Controlador JDBC de MySQL (`mysql-connector-j 8.4.0`) y Gson 2.11.0 para las respuestas JSON de las llamadas Ajax.

---

## 4.2 Estructura del proyecto y configuración de la aplicación

### 4.2.1 Filtros declarados en `web.xml` (el orden de los `filter-mapping` es el orden de ejecución)

**Archivo:** `src/main/webapp/WEB-INF/web.xml` — líneas 34 a 49 (16 líneas)

```xml
    <filter-mapping>
        <filter-name>SecurityHeadersFilter</filter-name>
        <url-pattern>/*</url-pattern>
        <dispatcher>REQUEST</dispatcher>
        <dispatcher>ERROR</dispatcher>
    </filter-mapping>
    <filter-mapping>
        <filter-name>AuthFilter</filter-name>
        <url-pattern>/*</url-pattern>
        <dispatcher>REQUEST</dispatcher>
    </filter-mapping>
    <filter-mapping>
        <filter-name>CsrfFilter</filter-name>
        <url-pattern>/*</url-pattern>
        <dispatcher>REQUEST</dispatcher>
    </filter-mapping>
```

Primero se agregan las cabeceras de seguridad, después se comprueba la sesión y el rol, y al final el token CSRF.

### 4.2.2 Configuración de la sesión

**Archivo:** `src/main/webapp/WEB-INF/web.xml` — líneas 51 a 62 (12 líneas)

```xml
    <session-config>
        <session-timeout>30</session-timeout>
        <cookie-config>
            <http-only>true</http-only>
            <attribute>
                <attribute-name>SameSite</attribute-name>
                <attribute-value>Strict</attribute-value>
            </attribute>
        </cookie-config>
        <!-- El identificador de sesión solo viaja en la cookie (nunca en la dirección). -->
        <tracking-mode>COOKIE</tracking-mode>
    </session-config>
```

Sesión de 30 minutos, cookie `HttpOnly` con `SameSite=Strict` y el identificador de sesión solo en cookie (nunca en la URL).

### 4.2.3 Lectura de `db.properties` y conexión a MySQL (`ConexionBD`)

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/util/ConexionBD.java` — líneas 17 a 30 (14 líneas)

```java
    /** Lee db.properties (try-with-resources cierra el archivo) y carga el driver. */
    private static Properties cargarPropiedades() {
        Properties p = new Properties();
        try (InputStream in = ConexionBD.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("No se encontró db.properties en el classpath");
            }
            p.load(in);
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
        return p;
    }
```

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/util/ConexionBD.java` — líneas 32 a 41 (10 líneas)

```java
    /**
     * Abre una conexión nueva. Quien la reciba debe cerrarla con try-with-resources:
     * try (Connection cn = ConexionBD.obtener()) { ... }
     */
    public static Connection obtener() throws SQLException {
        return DriverManager.getConnection(
                PROPS.getProperty("url"),
                PROPS.getProperty("usuario"),
                PROPS.getProperty("password"));
    }
```

La contraseña de la base de datos vive **solo** en `db.properties` (en el classpath); el código nunca la escribe. Quien pide la
conexión la cierra con `try-with-resources`.

---

## 4.3 Capa de datos (patrón DAO con JDBC puro)

Se muestra el DAO de contribuyentes (`ContribuyenteDAOImpl`) con sus operaciones básicas. Todas usan `PreparedStatement` y
`try-with-resources`; ninguna concatena datos del usuario en el SQL.

### 4.3.1 Listar y paginar con búsqueda (`listar`, `listarPagina`)

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/dao/impl/ContribuyenteDAOImpl.java` — líneas 69 a 78 (10 líneas)

```java
    @Override
    public List<Contribuyente> listar() {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            return mapearLista(rs);
        } catch (SQLException e) {
            throw new DaoException("No se pudo listar los contribuyentes", e);
        }
    }
```

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/dao/impl/ContribuyenteDAOImpl.java` — líneas 80 a 96 (17 líneas)

```java
    @Override
    public Pagina<Contribuyente> listarPagina(String texto, boolean incluirInactivos, int pagina, int tamano,
                                              String orden, String dir) {
        StringBuilder desde = new StringBuilder("FROM contribuyente WHERE 1 = 1");
        List<Object> filtros = new ArrayList<>();
        if (!incluirInactivos) {
            desde.append(" AND estado = 1");
        }
        if (texto != null && !texto.trim().isEmpty()) {
            desde.append(" AND (nombres_razon_social LIKE ? OR dni_ruc LIKE ?)");
            String patron = "%" + escaparLike(texto.trim()) + "%";
            filtros.add(patron);
            filtros.add(patron);
        }
        return Paginador.ejecutar(COLUMNAS, desde.toString(), filtros, resolverOrden(orden, dir).sql(), pagina, tamano,
                this::mapear);
    }
```

`listarPagina` arma el `FROM ... WHERE` con fragmentos fijos y manda el texto buscado como parámetro `?`; el orden sale de la lista
blanca `resolverOrden` (ver 4.8.2).

### 4.3.2 Buscar por id (`buscarPorId`)

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/dao/impl/ContribuyenteDAOImpl.java` — líneas 124 a 133 (10 líneas)

```java
    @Override
    public Optional<Contribuyente> buscarPorId(int id) {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_POR_ID)) {
            ps.setInt(1, id);
            return primero(ps);
        } catch (SQLException e) {
            throw new DaoException("No se pudo buscar el contribuyente", e);
        }
    }
```

### 4.3.3 Insertar y actualizar (`insertar`, `actualizar`)

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/dao/impl/ContribuyenteDAOImpl.java` — líneas 161 a 175 (15 líneas)

```java
    @Override
    public void insertar(Contribuyente c) throws RegistroDuplicadoException {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            asignarDatos(ps, c);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    c.setIdContribuyente(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            lanzarSegunError(e, "No se pudo registrar el contribuyente");
        }
    }
```

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/dao/impl/ContribuyenteDAOImpl.java` — líneas 177 a 187 (11 líneas)

```java
    @Override
    public void actualizar(Contribuyente c) throws RegistroDuplicadoException {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_ACTUALIZAR)) {
            asignarDatos(ps, c);
            ps.setInt(8, c.getIdContribuyente());
            ps.executeUpdate();
        } catch (SQLException e) {
            lanzarSegunError(e, "No se pudo actualizar el contribuyente");
        }
    }
```

Los errores de MySQL se traducen: una violación del `UNIQUE` del DNI/RUC (código 1062) se convierte en `RegistroDuplicadoException`
con mensaje para el usuario; cualquier otro error se relanza como `DaoException`.

### 4.3.4 Baja lógica (`darDeBaja`, `cambiarEstado`)

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/dao/impl/ContribuyenteDAOImpl.java` — líneas 189 a 197 (9 líneas)

```java
    @Override
    public void darDeBaja(int id) {
        cambiarEstado(id, false);
    }

    @Override
    public void reactivar(int id) {
        cambiarEstado(id, true);
    }
```

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/dao/impl/ContribuyenteDAOImpl.java` — líneas 215 a 224 (10 líneas)

```java
    private void cambiarEstado(int id, boolean activo) {
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(SQL_ESTADO)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("No se pudo cambiar el estado del contribuyente", e);
        }
    }
```

No existe `DELETE` físico: dar de baja es un `UPDATE` del campo `estado`; `reactivar` usa el mismo método con `true`.

### 4.3.5 Traducción del error de duplicado

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/dao/impl/ContribuyenteDAOImpl.java` — líneas 241 a 247 (7 líneas)

```java
    /** UNIQUE de DNI/RUC (error 1062) -> RegistroDuplicadoException; cualquier otro -> DaoException. */
    private void lanzarSegunError(SQLException e, String mensajeGeneral) throws RegistroDuplicadoException {
        if (e instanceof SQLIntegrityConstraintViolationException && e.getErrorCode() == MYSQL_DUPLICADO) {
            throw new RegistroDuplicadoException("Ya existe un contribuyente con ese DNI/RUC.", e);
        }
        throw new DaoException(mensajeGeneral, e);
    }
```


---

## 4.4 Controlador (Servlet): validación y Post-Redirect-Get

### 4.4.1 Validador del formulario de contribuyentes (`ContribuyenteValidador`)

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/util/ContribuyenteValidador.java` — líneas 32 a 45 (14 líneas)

```java
    /** DNI (8 dígitos) para NATURAL; RUC (11 dígitos, empieza con 10 o 20) para JURIDICA. */
    private static void validarDocumento(Contribuyente c, List<String> errores) {
        if (Contribuyente.NATURAL.equals(c.getTipoPersona())) {
            if (!Validador.dniValido(c.getDniRuc())) {
                errores.add("El DNI debe tener exactamente 8 dígitos.");
            }
        } else if (Contribuyente.JURIDICA.equals(c.getTipoPersona())) {
            if (!Validador.rucValido(c.getDniRuc())) {
                errores.add("El RUC debe tener 11 dígitos y empezar con 10 o 20.");
            }
        } else {
            errores.add("Seleccione el tipo de persona (natural o jurídica).");
        }
    }
```

### 4.4.2 `ContribuyenteServlet.guardar`: validar → guardar → auditar → redirigir (PRG)

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/controller/ContribuyenteServlet.java` — líneas 131 a 154 (24 líneas)

```java
    private void guardar(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<String> errores = new ArrayList<>();
        Contribuyente c = leerFormulario(req, errores);
        boolean esNuevo = c.getIdContribuyente() == 0;
        Optional<Contribuyente> antes = Optional.empty();   // cómo estaba antes de editar (para la auditoría)
        try {
            if (!esNuevo) {
                antes = restaurarDocumento(c);
                if (antes.isEmpty()) {
                    guardarFlash(req, "danger", "El contribuyente no existe.");
                    resp.sendRedirect(req.getContextPath() + "/contribuyentes");
                    return;
                }
            }
            errores.addAll(ContribuyenteValidador.validar(c));
            if (errores.isEmpty()) {
                persistir(c, esNuevo);
                auditarGuardado(req, antes.orElse(null), c);
                guardarFlash(req, "success", esNuevo
                        ? "Contribuyente registrado correctamente."
                        : "Contribuyente actualizado correctamente.");
                resp.sendRedirect(req.getContextPath() + "/contribuyentes"); // Post-Redirect-Get
                return;
```

Si no hay errores se guarda, se deja el mensaje en la sesión (flash) y se **redirige** (`sendRedirect`): al recargar la página no se
repite el POST. Si hay errores, el Servlet vuelve a mostrar el formulario con los datos escritos.

### 4.4.3 Despacho por acción en `doPost`

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/controller/ContribuyenteServlet.java` — líneas 120 a 129 (10 líneas)

```java
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        switch (texto(req, "accion")) {
            case "guardar" -> guardar(req, resp);
            case "baja" -> darDeBaja(req, resp);
            case "reactivar" -> reactivar(req, resp);
            default -> resp.sendRedirect(req.getContextPath() + "/contribuyentes");
        }
    }
```


---

## 4.5 Vistas (JSP con JSTL y Bootstrap)

### 4.5.1 Listado de contribuyentes (`contribuyentes/listado.jsp`)

**Archivo:** `src/main/webapp/WEB-INF/views/contribuyentes/listado.jsp` — líneas 48 a 69 (22 líneas)

```jsp
    <div class="table-responsive">
        <table class="table table-hover align-middle bg-body shadow-sm">
            <thead class="table-light">
            <tr>
                <s:thOrden columna="dniRuc" etiqueta="DNI/RUC"/>
                <s:thOrden columna="nombre" etiqueta="Nombre o razón social"/>
                <s:thOrden columna="tipo" etiqueta="Tipo"/>
                <s:thOrden columna="telefono" etiqueta="Teléfono"/>
                <s:thOrden columna="email" etiqueta="Email"/>
                <th scope="col" class="text-end">Acciones</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="x" items="${lista}">
                <tr class="${x.estado ? '' : 'table-light text-body-secondary'}">
                    <td><c:out value="${x.dniRuc}"/></td>
                    <td>
                        <a class="fw-semibold text-decoration-none"
                           href="${pageContext.request.contextPath}/contribuyentes?accion=ficha&id=${x.idContribuyente}"
                           title="Ver ficha"><c:out value="${x.nombresRazonSocial}"/></a>
                        <c:if test="${not x.estado}"><span class="badge text-bg-danger ms-1">Dado de baja</span></c:if>
                    </td>
```

Sin scriptlets: solo JSTL (`c:forEach`, `c:out`, `c:if`). Todo texto del usuario pasa por `<c:out>` (escapa HTML, evita XSS). Los
encabezados ordenables usan el tag file `s:thOrden`. Las clases `table`, `badge`, `btn` son de Bootstrap 5 (archivos locales, sin CDN).

---

## 4.6 Autenticación, filtros y listeners

### 4.6.1 Inicio de sesión con control de intentos (`LoginServlet.autenticar`)

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/controller/LoginServlet.java` — líneas 73 a 95 (23 líneas)

```java
    private Usuario autenticar(HttpServletRequest req, String username, String clave)
            throws CredencialesInvalidasException, CuentaBloqueadaException {
        if (!Validador.usuarioValido(username) || clave == null
                || clave.isEmpty() || clave.length() > MAX_CLAVE) {
            registrarFallo(req, username, null, "datos con formato inválido");
            throw new CredencialesInvalidasException();
        }
        Optional<Usuario> opt = usuarioDAO.buscarPorUsername(username.trim());
        String hash = opt.map(Usuario::getPasswordHash).orElse(HASH_FALSO);
        boolean claveOk = Seguridad.verificar(clave, hash) && opt.isPresent();
        if (!claveOk) {
            contarIntentoFallido(req, username, opt);
            throw new CredencialesInvalidasException();
        }
        Usuario u = opt.get();
        if (!u.isEstado()) {
            registrarFallo(req, username, u.getIdUsuario(), "cuenta bloqueada (se usó la contraseña correcta)");
            throw new CuentaBloqueadaException();
        }
        usuarioDAO.registrarAccesoExitoso(u.getIdUsuario());
        migrarHashAntiguo(u, clave);
        return u;
    }
```

Un mensaje único para usuario inexistente o contraseña errónea; si el usuario no existe se verifica contra un hash de relleno para
tardar lo mismo. El aviso de cuenta bloqueada solo aparece cuando la contraseña es correcta.

### 4.6.2 Sesión nueva y token CSRF (`LoginServlet.iniciarSesion`)

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/controller/LoginServlet.java` — líneas 148 a 158 (11 líneas)

```java
    /** Invalida la sesión anterior y crea una NUEVA (evita fijación de sesión) con su token CSRF. */
    private void iniciarSesion(HttpServletRequest req, Usuario u) {
        HttpSession anterior = req.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        u.setPasswordHash(null); // el hash nunca va a la sesión
        HttpSession nueva = req.getSession(true);
        nueva.setAttribute(SESION_USUARIO, u);
        nueva.setAttribute(CsrfFilter.ATRIBUTO_SESION, Seguridad.tokenAleatorio());
    }
```

### 4.6.3 Control de acceso por rol (`AuthFilter`)

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/filter/AuthFilter.java` — líneas 25 a 34 (10 líneas)

```java
    /** Roles permitidos por ruta. Para agregar un módulo basta una línea aquí. */
    private static final Map<String, Set<String>> ROLES_POR_RUTA = Map.of(
            "/contribuyentes", Set.of("ADMIN", "ATENCION"),
            "/almacen", Set.of("ADMIN", "ALMACEN"),
            "/citas", Set.of("ADMIN", "ATENCION"),
            "/dashboard/datos", Set.of("ADMIN", "ATENCION", "ALMACEN"),
            "/dashboard/desbloquear", Set.of("ADMIN"),
            "/usuarios", Set.of("ADMIN"),
            "/auditoria", Set.of("ADMIN"));

```

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/filter/AuthFilter.java` — líneas 36 a 59 (24 líneas)

```java
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String ruta = req.getRequestURI().substring(req.getContextPath().length());

        if (esPublica(ruta)) {
            chain.doFilter(req, resp);
            return;
        }
        evitarCache(resp);
        Usuario u = usuarioDeSesion(req);
        if (u == null && ruta.startsWith("/dashboard/")) {
            sesionExpirada(resp);   // llamadas Ajax del panel: 401 con JSON (no una redirección al login)
        } else if (u == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
        } else if (u.isDebeCambiarPassword() && !esRutaDeCambioDeClave(ruta)) {
            forzarCambioDeClave(req, resp);   // contraseña temporal: primero debe cambiarla
        } else if (!rolPermite(ruta, u.getRol())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } else {
            chain.doFilter(req, resp);
        }
    }
```

Solo `/login` y `/assets/*` son públicos. Sin sesión se redirige al login; con una contraseña temporal solo se permite cambiarla; si
el rol no está permitido para la ruta, responde 403.

### 4.6.4 Protección CSRF (`CsrfFilter`)

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/filter/CsrfFilter.java` — líneas 27 a 41 (15 líneas)

```java
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        if (!"POST".equalsIgnoreCase(req.getMethod()) || esLogin(req) || tokenValido(req)) {
            chain.doFilter(request, response);
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    private boolean esLogin(HttpServletRequest req) {
        return "/login".equals(req.getRequestURI().substring(req.getContextPath().length()));
    }
```

### 4.6.5 Listener de sesiones (`SesionListener`)

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/listener/SesionListener.java` — líneas 8 a 28 (21 líneas)

```java
/** Cuenta las sesiones activas y las deja en el contexto como "sesionesActivas". */
@WebListener
public class SesionListener implements HttpSessionListener {

    public static final String ATRIBUTO = "sesionesActivas";
    private static final AtomicInteger ACTIVAS = new AtomicInteger();

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        actualizar(se, ACTIVAS.incrementAndGet());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        actualizar(se, ACTIVAS.updateAndGet(n -> Math.max(0, n - 1)));
    }

    private void actualizar(HttpSessionEvent se, int total) {
        se.getSession().getServletContext().setAttribute(ATRIBUTO, total);
    }
}
```


---

## 4.7 Transacción con commit/rollback y llamada Ajax con respuesta JSON

### 4.7.1 La transacción de una salida de material (`MovimientoDAOImpl.registrarMovimiento`)

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/dao/impl/MovimientoDAOImpl.java` — líneas 54 a 77 (24 líneas)

```java
        try (Connection cn = ConexionBD.obtener()) {
            try {
                cn.setAutoCommit(false);                                   // 1) inicia la transacción
                int stock = bloquearYLeerStock(cn, idMaterial);            // 2) SELECT ... FOR UPDATE
                if (salida && stock < cantidad) {                          // 3) regla de negocio
                    throw new StockInsuficienteException(stock);
                }
                insertarMovimiento(cn, idMaterial, tipo, cantidad, salida ? idArea : null, idUsuario, observacion);
                actualizarStock(cn, idMaterial, cantidad, salida);         // 4) INSERT + UPDATE
                int nuevoStock = salida ? stock - cantidad : stock + cantidad;
                auditar(cn, idMaterial, tipo, cantidad, idArea, idUsuario, observacion, ip, stock, nuevoStock);   // 4b) misma transacción
                cn.commit();                                               // 5) todo o nada
                return nuevoStock;
            } catch (SQLException | RuntimeException | MaterialNoDisponibleException
                     | StockInsuficienteException e) {
                cn.rollback();                                             // deshace todo lo hecho
                throw e;
            } finally {
                cn.setAutoCommit(true);                                    // la conexión queda como estaba
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo registrar el movimiento de stock", e);
        }
    }
```

Pasos: (1) `setAutoCommit(false)`; (2) `SELECT ... FOR UPDATE` bloquea la fila del material; (3) la salida se rechaza si el stock no
alcanza; (4) `INSERT` del movimiento y `UPDATE` del stock, y la auditoría en la misma conexión; (5) `commit()`. Cualquier error ejecuta
`rollback()` y se restaura el `autoCommit` en el `finally`.

### 4.7.2 Consultas SQL de la transacción

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/dao/impl/MovimientoDAOImpl.java` — líneas 26 a 34 (9 líneas)

```java
    // FOR UPDATE bloquea la fila del material hasta el commit/rollback: nadie más puede
    // leer-modificar ese stock al mismo tiempo, así dos salidas simultáneas no descuentan de más.
    private static final String SQL_BLOQUEAR =
            "SELECT stock FROM material WHERE id_material = ? AND estado = 1 FOR UPDATE";
    private static final String SQL_INSERTAR =
            "INSERT INTO movimiento_stock (id_material, tipo, cantidad, id_area, id_usuario, observacion) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_SUMAR = "UPDATE material SET stock = stock + ? WHERE id_material = ?";
    private static final String SQL_RESTAR = "UPDATE material SET stock = stock - ? WHERE id_material = ?";
```

### 4.7.3 El Servlet responde JSON (`MovimientoServlet`)

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/controller/MovimientoServlet.java` — líneas 154 a 177 (24 líneas)

```java
    private Map<String, Object> exito(String tipo, int stock, boolean critico) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        r.put("mensaje", (MovimientoStock.ENTRADA.equals(tipo) ? "Entrada" : "Salida")
                + " registrada correctamente. Stock actual: " + stock + ".");
        r.put("stockActual", stock);
        r.put("critico", critico);
        return r;
    }

    private Map<String, Object> falla(String mensaje) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", false);
        r.put("mensaje", mensaje);
        return r;
    }

    private void responder(HttpServletResponse resp, int estado, Map<String, Object> cuerpo)
            throws IOException {
        resp.setStatus(estado);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(GSON.toJson(cuerpo));
    }
```

Forma fija de la respuesta: `{"ok":true|false,"mensaje":"..."}` (con `stockActual` y `critico` cuando fue correcto), generada con Gson.

### 4.7.4 La llamada `$.ajax` desde el navegador (`movimientos.js`)

**Archivo:** `src/main/webapp/assets/js/movimientos.js` — líneas 76 a 100 (25 líneas)

```javascript
    $form.on('submit', function (e) {
        e.preventDefault();
        if (!validarFormulario()) { return; }

        var idMaterial = $('#movIdMaterial').val();
        ocupado(true);
        $.ajax({
            url: $form.attr('action'),
            method: 'POST',
            dataType: 'json',
            data: $form.serialize()
        }).done(function (r) {
            if (r.ok) {
                actualizarFila(idMaterial, r);
                bootstrap.Modal.getOrCreateInstance($modal[0]).hide();
                mostrarAviso(r.mensaje);
            } else {
                mostrarError(r.mensaje);
            }
        }).fail(function () {
            mostrarError('No se pudo completar la operación.');
        }).always(function () {
            ocupado(false);
        });
    });
```

El token CSRF viaja en la cabecera `X-CSRF-Token` (se configura con `$.ajaxSetup` al inicio del archivo). La fila de la tabla se
actualiza sin recargar la página y el resultado se muestra como aviso (toast).

---

## 4.8 Reportes con filtros parametrizados, regla de cruce de citas y orden seguro

### 4.8.1 Reporte de movimientos con filtros opcionales (`ReporteAlmacenDAOImpl`)

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/dao/impl/ReporteAlmacenDAOImpl.java` — líneas 96 a 120 (25 líneas)

```java
        StringBuilder sql = new StringBuilder(SQL_MOVIMIENTOS_BASE);
        List<Object> parametros = new ArrayList<>();
        parametros.add(Timestamp.valueOf(desde.atStartOfDay()));
        parametros.add(Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
        if (tipo != null && !tipo.isEmpty()) {
            sql.append(FILTRO_TIPO);
            parametros.add(tipo);
        }
        if (idArea != null) {
            sql.append(FILTRO_AREA);
            parametros.add(idArea);
        }
        if (idMaterial != null) {
            sql.append(FILTRO_MATERIAL);
            parametros.add(idMaterial);
        }
        sql.append(ORDEN_MOVIMIENTOS);
        try (Connection cn = ConexionBD.obtener();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            asignar(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                return mapearMovimientos(rs);
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo generar el reporte de movimientos", e);
```

El SQL base y cada filtro son **texto fijo**; los filtros se agregan solo si el usuario los eligió, y sus valores (fechas, tipo, área,
material) entran siempre como parámetros `?`.

### 4.8.2 Orden por columna con lista blanca (`MovimientoDAOImpl.ORDEN`)

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/dao/impl/MovimientoDAOImpl.java` — líneas 104 a 118 (15 líneas)

```java
    /** Lista blanca de orden: nombre que llega del navegador -> fragmento fijo de SQL. */
    public static final Map<String, String> ORDEN = Map.of("fecha", "mv.fecha", "tipo", "mv.tipo",
            "cantidad", "mv.cantidad", "area", "a.nombre", "usuario", "u.nombres");

    /** Orden efectivo: si la columna no está en la lista blanca se usa el orden por defecto (fecha, descendente). */
    public static OrdenSql.Orden resolverOrden(String orden, String dir) {
        return OrdenSql.resolver(ORDEN, orden, dir, "fecha", "desc", "mv.id_movimiento DESC");
    }

    @Override
    public Pagina<MovimientoStock> listarPorMaterialPagina(int idMaterial, int pagina, int tamano,
                                                           String orden, String dir) {
        return Paginador.ejecutar(COLUMNAS_PAGINA, DESDE_PAGINA, List.of(idMaterial), resolverOrden(orden, dir).sql(),
                pagina, tamano, this::mapear);
    }
```

`ORDER BY` no admite parámetros `?`: por eso el nombre que llega del navegador solo se **traduce** con un `Map` a un fragmento fijo.
Un nombre que no está en el mapa (o con `;`, `--`, comillas) usa el orden por defecto: el texto del usuario nunca llega al SQL.

### 4.8.3 Regla de no cruce de horarios de citas (doble defensa: consulta previa + UNIQUE de la base)

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/dao/impl/CitaDAOImpl.java` — líneas 85 a 87 (3 líneas)

```java
    private static final String SQL_CRUCE =
            "SELECT COUNT(*) FROM cita WHERE id_funcionario = ? AND fecha = ? AND hora = ? "
            + "AND estado <> 'CANCELADA' AND id_cita <> ?";
```

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/dao/impl/CitaDAOImpl.java` — líneas 94 a 118 (25 líneas)

```java
    public void insertar(Cita c) throws CitaCruzadaException {
        try (Connection cn = ConexionBD.obtener()) {
            if (hayCruce(cn, c.getIdFuncionario(), c.getFecha(), c.getHora(), 0)) {   // defensa 1
                throw new CitaCruzadaException(c.getFecha(), c.getHora());
            }
            try (PreparedStatement ps = cn.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, c.getIdContribuyente());
                ps.setInt(2, c.getIdArea());
                ps.setInt(3, c.getIdFuncionario());
                ps.setInt(4, c.getVentanilla());
                ps.setDate(5, Date.valueOf(c.getFecha()));
                ps.setTime(6, Time.valueOf(c.getHora()));
                ps.setString(7, c.getMotivo());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        c.setIdCita(keys.getInt(1));
                    }
                }
                c.setEstado(Cita.PROGRAMADA);
            }
        } catch (SQLException e) {
            traducirError(e, c.getFecha(), c.getHora(), "No se pudo registrar la cita");  // defensa 2
        }
    }
```

**Archivo:** `src/main/java/pe/gob/munquinua/sigmud/dao/impl/CitaDAOImpl.java` — líneas 322 a 329 (8 líneas)

```java
    /** UNIQUE violado (error 1062) = otro usuario tomó ese horario a la vez -> CitaCruzadaException. */
    private void traducirError(SQLException e, LocalDate fecha, LocalTime hora, String mensajeGeneral)
            throws CitaCruzadaException {
        if (e instanceof SQLIntegrityConstraintViolationException && e.getErrorCode() == MYSQL_DUPLICADO) {
            throw new CitaCruzadaException(fecha, hora, e);
        }
        throw new DaoException(mensajeGeneral, e);
    }
```

Defensa 1: antes de insertar se consulta si el funcionario ya tiene una cita no cancelada a esa fecha y hora. Defensa 2: aunque dos
personas guarden a la vez, el índice `UNIQUE (id_funcionario, fecha, hora_activa)` de la base rechaza al segundo (error 1062), que se
traduce a `CitaCruzadaException`. La columna generada `hora_activa` es `NULL` cuando la cita está cancelada, y por eso una cita
cancelada no bloquea el horario.

# pruebas-seguridad.ps1 - Pruebas de seguridad EN VIVO contra SIGMUD (con Tomcat encendido).
# Uso:  .\pruebas-seguridad.ps1
#       .\pruebas-seguridad.ps1 -UrlBase http://localhost:8080/sigmud
# Muestra una tabla APROBADO / FALLIDO y termina con codigo 1 si algo fallo.
# Crea UN contribuyente de prueba (DNI 99999990, nombre con <script>) y lo borra al final.
param(
    [string]$UrlBase = "http://localhost:8080/sigmud",
    [string]$ClaveAdmin = "Admin123",
    [string]$ClaveAtencion = "Atencion123",
    [string]$ClaveAlmacen = "Almacen123",
    # Archivo db.properties que usa la aplicacion (para borrar el dato de prueba al final)
    [string]$PropiedadesBd = (Join-Path $PSScriptRoot "src\main\resources\db.properties"),
    [string]$ConectorJar = (Join-Path $env:USERPROFILE ".m2\repository\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar"),
    [string]$Java = "java"
)

# ===== Configuracion =====
$DniPrueba = "99999990"
# Usuario de prueba que crea el script (la aplicacion no borra usuarios: lo borra la limpieza final)
$UsuarioPrueba = "segpru" + (Get-Random -Minimum 100 -Maximum 999)
$NombreXss = "<script>alert(1)</script>"
# =========================

Add-Type -AssemblyName System.Net.Http
$resultados = New-Object System.Collections.Generic.List[object]

function Nuevo-Cliente {
    $h = New-Object System.Net.Http.HttpClientHandler
    $h.AllowAutoRedirect = $false          # queremos ver las redirecciones (302), no seguirlas
    $h.UseCookies = $true
    $h.CookieContainer = New-Object System.Net.CookieContainer
    return New-Object System.Net.Http.HttpClient($h)
}

# Hace una peticion y devuelve Estado, Location, Cuerpo, Cabeceras y Set-Cookie
function Pedir($cliente, [string]$metodo, [string]$ruta, $form = $null, $cabeceras = @{}) {
    $req = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::new($metodo), ($UrlBase + $ruta))
    if ($form -ne $null) {
        $d = New-Object 'System.Collections.Generic.Dictionary[string,string]'
        foreach ($k in $form.Keys) { $d[$k] = [string]$form[$k] }
        $req.Content = New-Object System.Net.Http.FormUrlEncodedContent($d)
    }
    foreach ($k in $cabeceras.Keys) { [void]$req.Headers.TryAddWithoutValidation($k, [string]$cabeceras[$k]) }
    $resp = $cliente.SendAsync($req).GetAwaiter().GetResult()
    $cuerpo = $resp.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    $cookies = @()
    if ($resp.Headers.Contains("Set-Cookie")) { $cookies = @($resp.Headers.GetValues("Set-Cookie")) }
    $loc = ""
    if ($resp.Headers.Location -ne $null) { $loc = $resp.Headers.Location.ToString() }
    return [pscustomobject]@{
        Estado = [int]$resp.StatusCode; Location = $loc; Cuerpo = $cuerpo
        Cabeceras = $resp.Headers; Cookies = $cookies
    }
}

function Registrar([string]$prueba, [string]$esperado, [string]$obtenido, [bool]$ok) {
    $resultados.Add([pscustomobject]@{
        Prueba = $prueba; Esperado = $esperado; Obtenido = $(if ($obtenido.Length -gt 90) { $obtenido.Substring(0, 90) + "..." } else { $obtenido })
        Resultado = $(if ($ok) { "APROBADO" } else { "FALLIDO" })
    })
}

function Iniciar-Sesion([string]$usuario, [string]$clave) {
    $c = Nuevo-Cliente
    $r = Pedir $c "POST" "/login" @{ username = $usuario; clave = $clave }
    return [pscustomobject]@{ Cliente = $c; Respuesta = $r; Entro = ($r.Estado -eq 302 -and $r.Location -like "*/menu") }
}

# Lee el token CSRF (campo oculto) de una pagina con formulario
function Leer-Token($cliente, [string]$ruta = "/contribuyentes?accion=nuevo") {
    $r = Pedir $cliente "GET" $ruta
    if ($r.Cuerpo -match 'name="csrf" value="([^"]+)"') { return $Matches[1] }
    return $null
}

# ---------------------------------------------------------------- comprobar que el servidor responde
try {
    $anonimo = Nuevo-Cliente
    $r0 = Pedir $anonimo "GET" "/login"
    if ($r0.Estado -ne 200) { throw "estado $($r0.Estado)" }
} catch {
    Write-Host "No se pudo conectar a $UrlBase ($_). Encienda Tomcat (.\desplegar.ps1) y vuelva a intentar." -ForegroundColor Red
    exit 2
}

try {
    # ------------------------------------------------------------ 1. Rutas protegidas sin sesion
    foreach ($ruta in @("/menu", "/contribuyentes", "/almacen", "/citas")) {
        $r = Pedir (Nuevo-Cliente) "GET" $ruta
        Registrar "Sin sesion: GET $ruta" "302 hacia /login" "$($r.Estado) -> $($r.Location)" `
            ($r.Estado -eq 302 -and $r.Location -like "*/login")
    }

    # ------------------------------------------------------------ 2. Inyeccion SQL y credenciales
    $r = Pedir (Nuevo-Cliente) "POST" "/login" @{ username = "' OR '1'='1"; clave = "cualquiera" }
    Registrar "Login con ' OR '1'='1 como usuario" "Falla (401), sin sesion ni redireccion a /menu" `
        "$($r.Estado) location='$($r.Location)'" ($r.Estado -eq 401 -and $r.Location -eq "")

    $r = Pedir (Nuevo-Cliente) "POST" "/login" @{ username = "admin' --"; clave = "x" }
    Registrar "Login con admin' -- como usuario" "Falla (401)" "$($r.Estado)" ($r.Estado -eq 401)

    $malaClave = Pedir (Nuevo-Cliente) "POST" "/login" @{ username = "admin"; clave = "ClaveIncorrecta999" }
    $noExiste = Pedir (Nuevo-Cliente) "POST" "/login" @{ username = "usuario_que_no_existe"; clave = "ClaveIncorrecta999" }
    $generico = 'Usuario o contrase.a incorrectos'
    Registrar "Login con contrasena incorrecta" "401 y mensaje generico" "$($malaClave.Estado)" `
        ($malaClave.Estado -eq 401 -and $malaClave.Cuerpo -match $generico)
    Registrar "Usuario inexistente da el MISMO mensaje" "401 y el mismo mensaje (no revela si el usuario existe)" "$($noExiste.Estado)" `
        ($noExiste.Estado -eq 401 -and $noExiste.Cuerpo -match $generico)

    # ------------------------------------------------------------ 3. Cabeceras y cookie
    $h = $r0.Cabeceras
    function Cab([string]$n) { if ($h.Contains($n)) { return ($h.GetValues($n) -join " ") } else { return "" } }
    Registrar "Cabecera X-Content-Type-Options" "nosniff" (Cab "X-Content-Type-Options") ((Cab "X-Content-Type-Options") -eq "nosniff")
    Registrar "Cabecera X-Frame-Options" "DENY" (Cab "X-Frame-Options") ((Cab "X-Frame-Options") -eq "DENY")
    Registrar "Cabecera Referrer-Policy" "same-origin" (Cab "Referrer-Policy") ((Cab "Referrer-Policy") -eq "same-origin")
    $csp = Cab "Content-Security-Policy"
    Registrar "Cabecera Content-Security-Policy" "script-src 'self' y sin 'unsafe-inline' en scripts" $csp `
        ($csp -match "script-src 'self'" -and $csp -notmatch "script-src[^;]*unsafe")

    # ------------------------------------------------------------ 4. Sesion de admin
    $admin = Iniciar-Sesion "admin" $ClaveAdmin
    Registrar "Login correcto (admin)" "302 hacia /menu" "$($admin.Respuesta.Estado) -> $($admin.Respuesta.Location)" $admin.Entro
    if (-not $admin.Entro) { throw "No se pudo iniciar sesion como admin; se cancelan las pruebas siguientes." }

    $cookie = ($admin.Respuesta.Cookies | Where-Object { $_ -like "JSESSIONID*" } | Select-Object -First 1)
    Registrar "Cookie de sesion HttpOnly y SameSite=Strict" "HttpOnly y SameSite=Strict" "$cookie" `
        ($cookie -match "HttpOnly" -and $cookie -match "SameSite=Strict")

    # ------------------------------------------------------------ 5. CSRF
    $token = Leer-Token $admin.Cliente
    Registrar "El formulario trae el token CSRF" "campo oculto csrf de 43 caracteres" "$token" ($token -match '^[A-Za-z0-9_-]{43}$')

    $r = Pedir $admin.Cliente "POST" "/contribuyentes" @{ accion = "baja"; id = "999999" }
    Registrar "POST sin token CSRF (sesion valida)" "403" "$($r.Estado)" ($r.Estado -eq 403)

    $r = Pedir $admin.Cliente "POST" "/contribuyentes" @{ accion = "baja"; id = "999999"; csrf = "token-falso" }
    Registrar "POST con token CSRF falso" "403" "$($r.Estado)" ($r.Estado -eq 403)

    $r = Pedir $admin.Cliente "POST" "/contribuyentes" @{ accion = "baja"; id = "999999"; csrf = $token }
    Registrar "POST con token CSRF correcto" "302 (no 403)" "$($r.Estado)" ($r.Estado -eq 302)

    $r = Pedir $admin.Cliente "POST" "/almacen/movimiento" @{ accion = "registrar"; idMaterial = "999999"; tipo = "ENTRADA"; cantidad = "1" }
    Registrar "Ajax POST sin cabecera X-CSRF-Token" "403" "$($r.Estado)" ($r.Estado -eq 403)

    $r = Pedir $admin.Cliente "POST" "/almacen/movimiento" @{ accion = "registrar"; idMaterial = "999999"; tipo = "ENTRADA"; cantidad = "1" } @{ "X-CSRF-Token" = $token }
    Registrar "Ajax POST con cabecera X-CSRF-Token" "200 JSON ok:false (material inexistente, no 403)" "$($r.Estado) $($r.Cuerpo)" `
        ($r.Estado -eq 200 -and $r.Cuerpo -match '"ok":false')

    # ------------------------------------------------------------ 6. XSS almacenado
    $r = Pedir $admin.Cliente "POST" "/contribuyentes" @{
        accion = "guardar"; tipoPersona = "NATURAL"; dniRuc = $DniPrueba; nombresRazonSocial = $NombreXss; csrf = $token }
    Registrar "Crear contribuyente con nombre <script>alert(1)</script>" "302 (guardado)" "$($r.Estado) $($r.Location)" ($r.Estado -eq 302)

    $r = Pedir $admin.Cliente "GET" "/contribuyentes?q=$DniPrueba"
    Registrar "XSS: el nombre aparece ESCAPADO en el listado" "&lt;script&gt; y nunca <script>alert(1)</script>" `
        "escapado=$($r.Cuerpo.Contains('&lt;script&gt;alert(1)&lt;/script&gt;')) crudo=$($r.Cuerpo.Contains('<script>alert(1)</script>'))" `
        ($r.Cuerpo.Contains('&lt;script&gt;alert(1)&lt;/script&gt;') -and -not $r.Cuerpo.Contains('<script>alert(1)</script>'))

    $r = Pedir $admin.Cliente "GET" ("/contribuyentes?q=" + [uri]::EscapeDataString('"><script>alert(2)</script>'))
    Registrar "XSS reflejado: el texto buscado se escapa" "sin <script>alert(2)</script> en la pagina" "crudo=$($r.Cuerpo.Contains('<script>alert(2)</script>'))" `
        (-not $r.Cuerpo.Contains('<script>alert(2)</script>'))

    # ------------------------------------------------------------ 7. Paginas de error
    $r = Pedir $admin.Cliente "GET" "/ruta-que-no-existe"
    Registrar "Error 404 sin datos tecnicos" "pagina propia, sin 'Apache Tomcat' ni 'Exception'" "$($r.Estado)" `
        ($r.Estado -eq 404 -and $r.Cuerpo -notmatch "Apache Tomcat|Exception|at org\.|at pe\.")
    $r = Pedir $admin.Cliente "POST" "/citas/ajax" @{ x = "1" } @{ "X-CSRF-Token" = $token }
    Registrar "Error 405 (metodo no permitido) sin datos tecnicos" "pagina propia, sin version del servidor" "$($r.Estado)" `
        ($r.Estado -eq 405 -and $r.Cuerpo -notmatch "Apache Tomcat|Exception")

    # ------------------------------------------------------------ 8. Roles
    $almacen = Iniciar-Sesion "almacen1" $ClaveAlmacen
    Registrar "Login almacen1" "302 hacia /menu" "$($almacen.Respuesta.Estado)" $almacen.Entro
    if ($almacen.Entro) {
        foreach ($ruta in @("/contribuyentes", "/citas")) {
            $r = Pedir $almacen.Cliente "GET" $ruta
            Registrar "almacen1 entra a $ruta" "403" "$($r.Estado)" ($r.Estado -eq 403)
        }
        $r = Pedir $almacen.Cliente "GET" "/almacen"
        Registrar "almacen1 entra a /almacen (su modulo)" "200" "$($r.Estado)" ($r.Estado -eq 200)
    }
    $atencion = Iniciar-Sesion "atencion1" $ClaveAtencion
    Registrar "Login atencion1" "302 hacia /menu" "$($atencion.Respuesta.Estado)" $atencion.Entro
    if ($atencion.Entro) {
        $r = Pedir $atencion.Cliente "GET" "/almacen"
        Registrar "atencion1 entra a /almacen" "403" "$($r.Estado)" ($r.Estado -eq 403)
        $r = Pedir $atencion.Cliente "GET" "/citas"
        Registrar "atencion1 entra a /citas (su modulo)" "200" "$($r.Estado)" ($r.Estado -eq 200)
    }

    # ------------------------------------------------------------ 8b. Paneles (dashboards)
    $r = Pedir (Nuevo-Cliente) "GET" "/dashboard/datos"
    Registrar "Sin sesion: GET /dashboard/datos" "401 (JSON, no redireccion)" "$($r.Estado) $($r.Cuerpo)" ($r.Estado -eq 401)
    $r = Pedir (Nuevo-Cliente) "POST" "/dashboard/desbloquear" @{ id = "1" }
    Registrar "Sin sesion: POST /dashboard/desbloquear" "401" "$($r.Estado)" ($r.Estado -eq 401)

    # Cada rol ve SU panel (el marcador data-panel lo pone la vista segun el rol de la sesion)
    $r = Pedir $admin.Cliente "GET" "/menu"
    Registrar "admin: /menu muestra el panel ADMIN" 'data-panel="ADMIN"' "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo.Contains('data-panel="ADMIN"'))
    $datosAdmin = Pedir $admin.Cliente "GET" "/dashboard/datos"
    Registrar "admin: /dashboard/datos trae los datos de ADMIN" 'rol ADMIN con "usuarios" y "bloqueadas"' "$($datosAdmin.Estado)" `
        ($datosAdmin.Estado -eq 200 -and $datosAdmin.Cuerpo -match '"rol":"ADMIN"' -and $datosAdmin.Cuerpo.Contains('"usuarios"') -and $datosAdmin.Cuerpo.Contains('"bloqueadas"'))

    if ($atencion.Entro) {
        $r = Pedir $atencion.Cliente "GET" "/menu"
        Registrar "atencion1: /menu muestra el panel ATENCION" 'data-panel="ATENCION" y sin panel ADMIN' "$($r.Estado)" `
            ($r.Estado -eq 200 -and $r.Cuerpo.Contains('data-panel="ATENCION"') -and -not $r.Cuerpo.Contains('data-panel="ADMIN"'))
        $propio = Pedir $atencion.Cliente "GET" "/dashboard/datos"
        $manipulado = Pedir $atencion.Cliente "GET" "/dashboard/datos?rol=ADMIN&id=1&idUsuario=1&funcionario=3&idFuncionario=3&panel=ADMIN"
        $rolPropio = ($propio.Cuerpo -match '"rol":"ATENCION"')
        $sinDatosAdmin = ($propio.Cuerpo -notmatch '"usuarios"|"bloqueadas"|"accesos"|"contribuyentes"|"entradasMes"')
        Registrar "atencion1 NO recibe datos de ADMIN" 'rol ATENCION y ninguna clave de ADMIN/ALMACEN' "$($propio.Estado) rol ATENCION=$rolPropio" `
            ($propio.Estado -eq 200 -and $rolPropio -and $sinDatosAdmin)
        $kpisPropios = ($propio.Cuerpo | ConvertFrom-Json).kpis | ConvertTo-Json -Compress
        $kpisManipulados = ($manipulado.Cuerpo | ConvertFrom-Json).kpis | ConvertTo-Json -Compress
        Registrar "atencion1 cambiando parametros (rol=ADMIN, id=1, funcionario=3) recibe lo MISMO" "misma respuesta; los parametros se ignoran" `
            "rol=$((($manipulado.Cuerpo | ConvertFrom-Json).rol)) kpis iguales=$($kpisPropios -eq $kpisManipulados)" `
            ((($manipulado.Cuerpo | ConvertFrom-Json).rol -eq "ATENCION") -and ($kpisPropios -eq $kpisManipulados))

        $tokenAtencion = Leer-Token $atencion.Cliente
        $r = Pedir $atencion.Cliente "POST" "/dashboard/desbloquear" @{ id = "1"; csrf = $tokenAtencion }
        Registrar "atencion1 (con token CSRF valido) usa /dashboard/desbloquear" "403 (solo ADMIN)" "$($r.Estado)" ($r.Estado -eq 403)
    }
    if ($almacen.Entro) {
        $r = Pedir $almacen.Cliente "GET" "/menu"
        Registrar "almacen1: /menu muestra el panel ALMACEN" 'data-panel="ALMACEN"' "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo.Contains('data-panel="ALMACEN"'))
        $d = Pedir $almacen.Cliente "GET" "/dashboard/datos"
        Registrar "almacen1 NO recibe datos de ADMIN ni de ATENCION" 'rol ALMACEN, con "entradasMes" y sin "bloqueadas"/"citasHoy"' "$($d.Estado)" `
            ($d.Estado -eq 200 -and $d.Cuerpo -match '"rol":"ALMACEN"' -and $d.Cuerpo.Contains('"entradasMes"') -and $d.Cuerpo -notmatch '"bloqueadas"|"citasHoy"|"proximas"')
        $tokenAlmacen = Leer-Token $almacen.Cliente "/almacen?accion=nuevo"
        $r = Pedir $almacen.Cliente "POST" "/dashboard/desbloquear" @{ id = "1"; csrf = $tokenAlmacen }
        Registrar "almacen1 (con token CSRF valido) usa /dashboard/desbloquear" "403 (solo ADMIN)" "$($r.Estado)" ($r.Estado -eq 403)
    }
    $r = Pedir $admin.Cliente "POST" "/dashboard/desbloquear" @{ id = "999999" }
    Registrar "admin: /dashboard/desbloquear SIN token CSRF" "403" "$($r.Estado)" ($r.Estado -eq 403)
    $r = Pedir $admin.Cliente "POST" "/dashboard/desbloquear" @{ id = "999999"; csrf = $token }
    Registrar "admin: /dashboard/desbloquear con token (usuario inexistente)" "200 JSON ok:false (no 403)" "$($r.Estado) $($r.Cuerpo)" `
        ($r.Estado -eq 200 -and $r.Cuerpo -match '"ok":false')

    # ------------------------------------------------------------ 8c. Gestion de usuarios y Mi perfil
    foreach ($par in @(@("atencion1", $atencion), @("almacen1", $almacen))) {
        if ($par[1].Entro) {
            $r = Pedir $par[1].Cliente "GET" "/usuarios"
            Registrar "$($par[0]) entra a /usuarios" "403 (solo ADMIN)" "$($r.Estado)" ($r.Estado -eq 403)
            $tk = Leer-Token $par[1].Cliente "/perfil/password"
            $r = Pedir $par[1].Cliente "POST" "/usuarios" @{ accion = "guardar"; username = "intruso"; nombres = "Intruso"; rol = "ADMIN"; csrf = $tk }
            Registrar "$($par[0]) intenta crear un usuario ADMIN (con token valido)" "403" "$($r.Estado)" ($r.Estado -eq 403)
            $r = Pedir $par[1].Cliente "POST" "/usuarios/restablecer" @{ id = "1" } @{ "X-CSRF-Token" = $tk }
            Registrar "$($par[0]) intenta restablecer una clave (con token valido)" "403" "$($r.Estado)" ($r.Estado -eq 403)
        }
    }
    $r = Pedir $admin.Cliente "GET" "/usuarios"
    Registrar "admin entra a /usuarios" "200" "$($r.Estado)" ($r.Estado -eq 200)
    $idAdmin = if ($r.Cuerpo -match '<tr data-id="(\d+)" data-username="admin"') { $Matches[1] } else { "" }
    Registrar "El listado trae el id del admin" "numero" "$idAdmin" ($idAdmin -match '^\d+$')

    $r = Pedir $admin.Cliente "POST" "/usuarios" @{ accion = "guardar"; username = $UsuarioPrueba; nombres = "Usuario de prueba de seguridad"; rol = "ALMACEN" }
    Registrar "POST /usuarios sin token CSRF" "403" "$($r.Estado)" ($r.Estado -eq 403)

    # Crear un usuario: el sistema genera la contraseña temporal y la muestra UNA vez
    $r = Pedir $admin.Cliente "POST" "/usuarios" @{ accion = "guardar"; username = $UsuarioPrueba; nombres = "Usuario de prueba de seguridad"; rol = "ALMACEN"; csrf = $token }
    Registrar "Crear usuario de prueba ($UsuarioPrueba)" "302 (creado)" "$($r.Estado)" ($r.Estado -eq 302)
    $r = Pedir $admin.Cliente "GET" "/usuarios"
    $temporal = if ($r.Cuerpo -match '(?s)id="credencialValor".*?value="([^"]+)"') { $Matches[1] } else { "" }
    Registrar "La contrasena temporal se muestra (12 caracteres, cumple la politica)" "12 caracteres con mayuscula, minuscula y numero" "longitud $($temporal.Length)" `
        ($temporal.Length -eq 12 -and $temporal -cmatch '[A-Z]' -and $temporal -cmatch '[a-z]' -and $temporal -match '\d')
    $r = Pedir $admin.Cliente "GET" "/usuarios"
    Registrar "La contrasena temporal se muestra UNA sola vez" "no aparece al recargar" "aparece=$($r.Cuerpo.Contains('credencialValor'))" (-not $r.Cuerpo.Contains('credencialValor'))
    $r = Pedir $admin.Cliente "POST" "/usuarios" @{ accion = "guardar"; username = $UsuarioPrueba; nombres = "Otro nombre"; rol = "ALMACEN"; csrf = $token }
    Registrar "Crear un usuario con nombre repetido" "200 con el error (no se crea)" "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo -match 'Ya existe un usuario')
    $r = Pedir $admin.Cliente "POST" "/usuarios" @{ accion = "guardar"; username = "segpru_atencion"; nombres = "Sin area"; rol = "ATENCION"; csrf = $token }
    Registrar "Crear un usuario ATENCION sin area" "200 con el error (no se crea)" "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo -match 'debe tener un .rea asignada')

    # Reglas del ADMIN sobre su propia cuenta
    $r = Pedir $admin.Cliente "POST" "/usuarios" @{ accion = "estado"; id = $idAdmin; activo = "0"; csrf = $token }
    $r2 = Pedir $admin.Cliente "GET" "/usuarios"
    Registrar "Un ADMIN intenta desactivarse a si mismo" "rechazado: 'No puede desactivar su propia cuenta'" "$($r.Estado) mensaje=$($r2.Cuerpo -match 'No puede desactivar su propia cuenta')" `
        ($r2.Cuerpo -match 'No puede desactivar su propia cuenta')
    $r = Pedir $admin.Cliente "POST" "/usuarios" @{ accion = "guardar"; id = $idAdmin; nombres = "Administrador del Sistema"; rol = "ATENCION"; idArea = "1"; csrf = $token }
    Registrar "Un ADMIN intenta cambiarse su propio rol" "200 con el error 'No puede cambiar su propio rol'" "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo -match 'No puede cambiar su propio rol')
    $r = Pedir $admin.Cliente "GET" "/usuarios"
    Registrar "El ADMIN sigue siendo ADMIN y activo" "sigue entrando a /usuarios" "$($r.Estado)" ($r.Estado -eq 200)

    # Restablecer contrasena (Ajax)
    $idNuevo = if ($r.Cuerpo -match "<tr data-id=""(\d+)"" data-username=""$UsuarioPrueba""") { $Matches[1] } else { "" }
    $r = Pedir $admin.Cliente "POST" "/usuarios/restablecer" @{ id = $idNuevo }
    Registrar "Restablecer clave sin token CSRF" "403" "$($r.Estado)" ($r.Estado -eq 403)
    $r = Pedir $admin.Cliente "POST" "/usuarios/restablecer" @{ id = $idNuevo } @{ "X-CSRF-Token" = $token }
    $temporal2 = if ($r.Cuerpo -match '"passwordTemporal":"([^"]+)"') { $Matches[1] } else { "" }
    Registrar "Restablecer clave (admin, con token)" "200 JSON con la temporal nueva (12 caracteres)" "$($r.Estado) longitud $($temporal2.Length)" `
        ($r.Estado -eq 200 -and $temporal2.Length -eq 12 -and $temporal2 -ne $temporal)
    $r = Pedir $admin.Cliente "POST" "/usuarios/restablecer" @{ id = $idAdmin } @{ "X-CSRF-Token" = $token }
    Registrar "El admin no puede restablecer su propia clave desde aqui" "200 JSON ok:false" "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo -match '"ok":false')

    # Contrasena temporal: cambio obligatorio
    Registrar "La contrasena temporal anterior ya no sirve" "401" "$((Pedir (Nuevo-Cliente) 'POST' '/login' @{ username = $UsuarioPrueba; clave = $temporal }).Estado)" `
        ((Pedir (Nuevo-Cliente) 'POST' '/login' @{ username = $UsuarioPrueba; clave = $temporal }).Estado -eq 401)
    $nuevoU = Iniciar-Sesion $UsuarioPrueba $temporal2
    Registrar "Login del usuario nuevo con su contrasena temporal" "302 hacia /menu" "$($nuevoU.Respuesta.Estado)" $nuevoU.Entro
    if ($nuevoU.Entro) {
        foreach ($ruta in @("/menu", "/almacen", "/perfil", "/dashboard/datos")) {
            $r = Pedir $nuevoU.Cliente "GET" $ruta
            Registrar "Clave temporal: GET $ruta" "302 hacia /perfil/password" "$($r.Estado) -> $($r.Location)" ($r.Estado -eq 302 -and $r.Location -like "*/perfil/password")
        }
        $r = Pedir $nuevoU.Cliente "GET" "/assets/css/sigmud.css"
        Registrar "Clave temporal: /assets sigue accesible" "200" "$($r.Estado)" ($r.Estado -eq 200)
        $r = Pedir $nuevoU.Cliente "GET" "/perfil/password"
        Registrar "Clave temporal: /perfil/password accesible con el mensaje explicativo" "200 con aviso de contrasena temporal" "$($r.Estado)" `
            ($r.Estado -eq 200 -and $r.Cuerpo -match 'temporal')
        $tkN = Leer-Token $nuevoU.Cliente "/perfil/password"
        $r = Pedir $nuevoU.Cliente "POST" "/perfil/password" @{ actual = $temporal2; nueva = "abc"; confirmacion = "abc"; csrf = $tkN }
        Registrar "Contrasena debil rechazada" "200 con los errores de la politica" "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo -match 'al menos 8 caracteres' -and $r.Cuerpo -match 'may.scula')
        $r = Pedir $nuevoU.Cliente "POST" "/perfil/password" @{ actual = $temporal2; nueva = $UsuarioPrueba; confirmacion = $UsuarioPrueba; csrf = $tkN }
        Registrar "Contrasena igual al nombre de usuario rechazada" "200 con el error" "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo -match 'igual al nombre de usuario')
        $r = Pedir $nuevoU.Cliente "POST" "/perfil/password" @{ actual = "ClaveActual999"; nueva = "Definitiva2026x"; confirmacion = "Definitiva2026x"; csrf = $tkN }
        Registrar "Contrasena actual incorrecta rechazada" "200 con el error (cuenta como intento fallido)" "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo -match 'actual no es correcta')
        $r = Pedir $nuevoU.Cliente "POST" "/perfil/password" @{ actual = $temporal2; nueva = "Definitiva2026x"; confirmacion = "Distinta2026x"; csrf = $tkN }
        Registrar "Confirmacion distinta rechazada" "200 con el error" "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo -match 'confirmaci.n no coincide')
        $r = Pedir $nuevoU.Cliente "POST" "/perfil/password" @{ actual = $temporal2; nueva = $temporal2; confirmacion = $temporal2; csrf = $tkN }
        Registrar "Nueva igual a la actual rechazada" "200 con el error" "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo -match 'distinta de la actual')
        $r = Pedir $nuevoU.Cliente "POST" "/perfil/password" @{ actual = $temporal2; nueva = "Definitiva2026x"; confirmacion = "Definitiva2026x" }
        Registrar "Cambio de clave sin token CSRF" "403" "$($r.Estado)" ($r.Estado -eq 403)
        $r = Pedir $nuevoU.Cliente "POST" "/perfil/password" @{ actual = $temporal2; nueva = "Definitiva2026x"; confirmacion = "Definitiva2026x"; csrf = $tkN }
        Registrar "Cambio de clave correcto" "302 hacia /perfil" "$($r.Estado) -> $($r.Location)" ($r.Estado -eq 302 -and $r.Location -like "*/perfil")
        $cookieNueva = @($r.Cookies | Where-Object { $_ -like "JSESSIONID*" }).Count
        Registrar "El cambio de clave cambia el id de sesion" "Set-Cookie con un JSESSIONID nuevo" "cookies nuevas: $cookieNueva" ($cookieNueva -ge 1)
        $r = Pedir $nuevoU.Cliente "GET" "/menu"
        Registrar "Tras cambiar la clave el usuario ya usa el sistema" "200 (panel)" "$($r.Estado)" ($r.Estado -eq 200)
        $r = Pedir $nuevoU.Cliente "GET" "/perfil"
        Registrar "Mi perfil muestra los datos del usuario" "200 con usuario, rol e inicio de sesion" "$($r.Estado)" `
            ($r.Estado -eq 200 -and $r.Cuerpo.Contains($UsuarioPrueba) -and $r.Cuerpo -match 'ALMACEN' -and $r.Cuerpo -match 'Inicio de esta sesi')
        $r = Pedir (Nuevo-Cliente) "POST" "/login" @{ username = $UsuarioPrueba; clave = $temporal2 }
        Registrar "La contrasena temporal ya no sirve despues del cambio" "401" "$($r.Estado)" ($r.Estado -eq 401)
        $reingreso = Iniciar-Sesion $UsuarioPrueba "Definitiva2026x"
        Registrar "Entra con la contrasena nueva (sin cambio obligatorio)" "302 hacia /menu y /menu = 200" "$($reingreso.Respuesta.Estado)" `
            ($reingreso.Entro -and (Pedir $reingreso.Cliente "GET" "/menu").Estado -eq 200)
    }

    # Desactivar al usuario de prueba: ya no puede entrar
    if ($idNuevo -ne "") {
        $r = Pedir $admin.Cliente "POST" "/usuarios" @{ accion = "estado"; id = $idNuevo; activo = "0"; csrf = $token }
        $r = Pedir (Nuevo-Cliente) "POST" "/login" @{ username = $UsuarioPrueba; clave = "Definitiva2026x" }
        Registrar "Un usuario desactivado no puede entrar" "401 (cuenta bloqueada o inactiva)" "$($r.Estado)" ($r.Estado -eq 401)
    }

    # ------------------------------------------------------------ 8d. Auditoria (solo ADMIN, solo lectura)
    foreach ($par in @(@("atencion1", $atencion), @("almacen1", $almacen))) {
        if ($par[1].Entro) {
            $r = Pedir $par[1].Cliente "GET" "/auditoria"
            Registrar "$($par[0]) entra a /auditoria" "403 (solo ADMIN)" "$($r.Estado)" ($r.Estado -eq 403)
            $r = Pedir $par[1].Cliente "GET" "/auditoria?formato=csv"
            Registrar "$($par[0]) intenta exportar la auditoria en CSV" "403" "$($r.Estado)" ($r.Estado -eq 403)
        }
    }
    $r = Pedir (Nuevo-Cliente) "GET" "/auditoria"
    Registrar "Sin sesion: GET /auditoria" "302 hacia /login" "$($r.Estado) -> $($r.Location)" ($r.Estado -eq 302 -and $r.Location -like "*/login")
    $r = Pedir $admin.Cliente "GET" "/auditoria"
    Registrar "admin entra a /auditoria" "200" "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo -match 'Auditor')
    $r = Pedir $admin.Cliente "GET" "/auditoria?formato=csv"
    Registrar "admin exporta la auditoria en CSV" "200 text/csv" "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo -match 'Fecha y hora')
    $r = Pedir $admin.Cliente "POST" "/auditoria" @{ accion = "borrar"; csrf = $token }
    Registrar "La auditoria es de solo lectura: POST /auditoria (con token)" "405 (no hay nada que modificar)" "$($r.Estado)" ($r.Estado -eq 405)
    $r = Pedir $admin.Cliente "GET" "/auditoria?desde=2020-01-01&hasta=2026-12-31"
    Registrar "Periodo de mas de 366 dias rechazado" "200 con el error (sin consultar)" "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo -match '366')
    $r = Pedir $admin.Cliente "GET" ("/auditoria?usuario=" + [uri]::EscapeDataString("' OR '1'='1") + "&q=" + [uri]::EscapeDataString("'; DROP TABLE auditoria; --"))
    Registrar "Inyeccion SQL en los filtros de auditoria" "200 sin error y sin resultados" "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo -match '<strong>0</strong>')

    # Un login fallido queda registrado y aparece en /auditoria
    $UsuarioFallido = "segaud" + (Get-Random -Minimum 100 -Maximum 999)
    $r = Pedir (Nuevo-Cliente) "POST" "/login" @{ username = $UsuarioFallido; clave = "ClaveIncorrecta999" }
    Registrar "Login fallido con un usuario nuevo ($UsuarioFallido)" "401" "$($r.Estado)" ($r.Estado -eq 401)
    $r = Pedir $admin.Cliente "GET" "/auditoria?usuario=$UsuarioFallido&accion=LOGIN_FALLIDO"
    Registrar "El login fallido aparece en /auditoria" "una fila LOGIN_FALLIDO de $UsuarioFallido" "estado $($r.Estado)" `
        ($r.Estado -eq 200 -and $r.Cuerpo.Contains("data-usuario=""$UsuarioFallido""") -and $r.Cuerpo -match 'LOGIN_FALLIDO')
    Registrar "El registro no contiene la contrasena escrita" "sin 'ClaveIncorrecta999' en la pagina" "aparece=$($r.Cuerpo.Contains('ClaveIncorrecta999'))" `
        (-not $r.Cuerpo.Contains('ClaveIncorrecta999'))

    # Usuario con saltos de linea y <script>: se sanea al guardar y se escapa al mostrar
    $UsuarioMalo = "segaud" + (Get-Random -Minimum 100 -Maximum 999)
    $r = Pedir (Nuevo-Cliente) "POST" "/login" @{ username = ($UsuarioMalo + "`r`nFALSO <script>alert(7)</script>"); clave = "ClaveIncorrecta999" }
    Registrar "Login fallido con saltos de linea y <script> en el usuario" "401" "$($r.Estado)" ($r.Estado -eq 401)
    $r = Pedir $admin.Cliente "GET" "/auditoria?usuario=$UsuarioMalo"
    Registrar "XSS en auditoria: el usuario con <script> aparece ESCAPADO" "&lt;script&gt; y nunca <script>alert(7)" `
        "escapado=$($r.Cuerpo.Contains('&lt;script&gt;alert(7)')) crudo=$($r.Cuerpo.Contains('<script>alert(7)'))" `
        ($r.Cuerpo.Contains('&lt;script&gt;alert(7)') -and -not $r.Cuerpo.Contains('<script>alert(7)'))
    Registrar "Los saltos de linea se eliminan al guardar" "una sola linea: '$UsuarioMalo FALSO' dentro del mismo atributo" "revisado en el HTML" `
        ($r.Cuerpo.Contains("data-usuario=""$UsuarioMalo FALSO &lt;script&gt;"))

    # El panel del admin trae el indicador y la actividad reciente
    $d = Pedir $admin.Cliente "GET" "/dashboard/datos"
    Registrar "Panel ADMIN: indicador de accesos fallidos y actividad reciente" 'claves "loginFallidos" y "actividad"' "$($d.Estado)" `
        ($d.Estado -eq 200 -and $d.Cuerpo.Contains('"loginFallidos"') -and $d.Cuerpo.Contains('"actividad"'))
    $dAt = if ($atencion.Entro) { Pedir $atencion.Cliente "GET" "/dashboard/datos" } else { $null }
    if ($dAt) {
        Registrar "atencion1 NO recibe la actividad ni los accesos fallidos" 'sin "loginFallidos" ni "actividad"' "$($dAt.Estado)" `
            ($dAt.Estado -eq 200 -and $dAt.Cuerpo -notmatch '"loginFallidos"|"actividad"')
    }

    # ------------------------------------------------------------ 8e. Ficha del contribuyente
    $r = Pedir $admin.Cliente "GET" "/contribuyentes?q=$DniPrueba"
    $idFicha = if ($r.Cuerpo -match 'accion=ficha&id=(\d+)') { $Matches[1] } else { "" }
    Registrar "El listado enlaza el nombre a la ficha" "enlace accion=ficha&id=N" "id=$idFicha" ($idFicha -match '^\d+$')
    $rutaFicha = "/contribuyentes?accion=ficha&id=$idFicha"

    $r = Pedir (Nuevo-Cliente) "GET" $rutaFicha
    Registrar "Sin sesion: GET ficha" "302 hacia /login" "$($r.Estado) -> $($r.Location)" ($r.Estado -eq 302 -and $r.Location -like "*/login")
    if ($almacen.Entro) {
        $r = Pedir $almacen.Cliente "GET" $rutaFicha
        Registrar "almacen1 abre la ficha" "403" "$($r.Estado)" ($r.Estado -eq 403)
        $tkAl = Leer-Token $almacen.Cliente "/almacen?accion=nuevo"
        $r = Pedir $almacen.Cliente "POST" "/contribuyentes" @{ accion = "reactivar"; id = $idFicha; csrf = $tkAl }
        Registrar "almacen1 intenta reactivar (con token valido)" "403" "$($r.Estado)" ($r.Estado -eq 403)
    }

    $r = Pedir $admin.Cliente "GET" $rutaFicha
    Registrar "admin abre la ficha" "200 con Datos personales, indicadores e historial" "$($r.Estado)" `
        ($r.Estado -eq 200 -and $r.Cuerpo.Contains('Datos personales') -and $r.Cuerpo.Contains('Total de citas') -and $r.Cuerpo.Contains('Historial de citas'))
    Registrar "XSS: el nombre con <script> se ve ESCAPADO en la ficha" "&lt;script&gt; y nunca <script>alert(1)" `
        "escapado=$($r.Cuerpo.Contains('&lt;script&gt;alert(1)')) crudo=$($r.Cuerpo.Contains('<script>alert(1)'))" `
        ($r.Cuerpo.Contains('&lt;script&gt;alert(1)') -and -not $r.Cuerpo.Contains('<script>alert(1)'))
    Registrar "admin ve la pestana Actividad" "panel-actividad presente" "$($r.Cuerpo.Contains('panel-actividad'))" ($r.Cuerpo.Contains('panel-actividad'))

    if ($atencion.Entro) {
        $r = Pedir $atencion.Cliente "GET" $rutaFicha
        Registrar "atencion1 abre la ficha" "200" "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo.Contains('Datos personales'))
        Registrar "atencion1 NO ve la pestana Actividad" "sin panel-actividad ni tab-actividad" "panel=$($r.Cuerpo.Contains('panel-actividad'))" `
            (-not $r.Cuerpo.Contains('panel-actividad') -and -not $r.Cuerpo.Contains('tab-actividad'))
        $r = Pedir $atencion.Cliente "GET" ($rutaFicha + "&tab=actividad&actividad=1&verActividad=true&rol=ADMIN")
        Registrar "atencion1 NO puede pedir la Actividad con parametros" "200 sin panel-actividad ni eventos de auditoria" "panel=$($r.Cuerpo.Contains('panel-actividad'))" `
            ($r.Estado -eq 200 -and -not $r.Cuerpo.Contains('panel-actividad') -and -not $r.Cuerpo.Contains('Consulta de ficha'))
        $r = Pedir $atencion.Cliente "GET" ($rutaFicha + "&estado=" + [uri]::EscapeDataString("X' OR '1'='1"))
        Registrar "Filtro de estado con texto invalido" "200 (se ignora, sin error 500)" "$($r.Estado)" ($r.Estado -eq 200)
    }

    # Identificadores invalidos: vuelven al listado con mensaje, nunca error 500 ni datos
    $malos = @("abc", "1 OR 1=1", "999999", "-5", "", "0", "99999999999999999999")
    foreach ($m in $malos) {
        $cli = (Iniciar-Sesion "atencion1" $ClaveAtencion).Cliente
        $r = Pedir $cli "GET" ("/contribuyentes?accion=ficha&id=" + [uri]::EscapeDataString($m))
        $lista = Pedir $cli "GET" "/contribuyentes"
        Registrar "Ficha con id '$m'" "302 al listado con mensaje (no 500, sin datos)" "$($r.Estado) -> $($r.Location)" `
            ($r.Estado -eq 302 -and $r.Location -like "*/contribuyentes" -and $r.Cuerpo -notmatch 'Datos personales' -and $lista.Cuerpo.Contains('id="datosFlash"') -and $lista.Cuerpo.Contains('data-tipo="error"'))
    }
    $r = Pedir $admin.Cliente "GET" "/contribuyentes?accion=ficha"
    Registrar "Ficha sin id" "302 al listado" "$($r.Estado)" ($r.Estado -eq 302)

    # La consulta de la ficha queda en la auditoria (datos personales, Ley 29733)
    $r = Pedir $admin.Cliente "GET" "/auditoria?accion=CONSULTA_FICHA&entidad=CONTRIBUYENTE"
    Registrar "CONSULTA_FICHA aparece en /auditoria" "al menos un evento CONSULTA_FICHA" "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo.Contains('CONSULTA_FICHA') -and $r.Cuerpo -match 'Consulta de ficha')

    # Baja y reactivacion desde la ficha (POST con token y Post-Redirect-Get)
    $r = Pedir $admin.Cliente "POST" "/contribuyentes" @{ accion = "baja"; id = $idFicha; volver = "ficha" }
    Registrar "Baja desde la ficha sin token CSRF" "403" "$($r.Estado)" ($r.Estado -eq 403)
    $r = Pedir $admin.Cliente "POST" "/contribuyentes" @{ accion = "reactivar"; id = $idFicha; volver = "ficha" }
    Registrar "Reactivar sin token CSRF" "403" "$($r.Estado)" ($r.Estado -eq 403)
    $r = Pedir $admin.Cliente "POST" "/contribuyentes" @{ accion = "baja"; id = $idFicha; volver = "ficha"; csrf = $token }
    Registrar "Baja desde la ficha (con token)" "302 de vuelta a la ficha" "$($r.Estado) -> $($r.Location)" ($r.Estado -eq 302 -and $r.Location -like "*accion=ficha&id=$idFicha")
    $r = Pedir $admin.Cliente "GET" $rutaFicha
    Registrar "La ficha de un dado de baja se puede abrir" "200 con insignia 'Dado de baja', Editar y Nueva cita deshabilitados" "$($r.Estado)" `
        ($r.Estado -eq 200 -and $r.Cuerpo.Contains('Dado de baja') -and $r.Cuerpo -match 'disabled aria-disabled="true"\s+title="Un contribuyente dado de baja no se puede editar' -and $r.Cuerpo -match 'Reactivar')
    $r = Pedir $admin.Cliente "GET" "/contribuyentes?q=$DniPrueba"
    Registrar "El listado normal NO muestra al dado de baja" "sin resultados" "$($r.Cuerpo -match 'No se encontraron resultados')" ($r.Cuerpo -match 'No se encontraron resultados')
    $r = Pedir $admin.Cliente "GET" "/contribuyentes?q=$DniPrueba&inactivos=1"
    Registrar "Con 'Ver tambien dados de baja' aparece con su insignia" "fila con 'Dado de baja' y boton Reactivar" "$($r.Estado)" `
        ($r.Estado -eq 200 -and $r.Cuerpo.Contains('Dado de baja') -and $r.Cuerpo.Contains('modalReactivar'))
    if ($atencion.Entro) {
        $r = Pedir $atencion.Cliente "GET" "/citas?accion=nuevo&contribuyente=$idFicha"
        Registrar "Nueva cita para un contribuyente dado de baja" "200 y el contribuyente NO viene preseleccionado" "$($r.Estado)" `
            ($r.Estado -eq 200 -and -not $r.Cuerpo.Contains('elegido desde su ficha'))
    }
    $r = Pedir $admin.Cliente "POST" "/contribuyentes" @{ accion = "reactivar"; id = $idFicha; volver = "ficha"; csrf = $token }
    Registrar "Reactivar (con token)" "302 de vuelta a la ficha" "$($r.Estado) -> $($r.Location)" ($r.Estado -eq 302 -and $r.Location -like "*accion=ficha&id=$idFicha")
    $r = Pedir $admin.Cliente "GET" $rutaFicha
    Registrar "Tras reactivar la ficha ya no dice 'Dado de baja'" "sin la insignia y con mensaje de exito" "$($r.Estado)" `
        ($r.Estado -eq 200 -and -not $r.Cuerpo.Contains('Dado de baja') -and $r.Cuerpo.Contains('reactivado correctamente'))
    $r = Pedir $admin.Cliente "GET" "/auditoria?accion=REACTIVAR"
    Registrar "REACTIVAR aparece en /auditoria" "evento REACTIVAR" "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo.Contains('REACTIVAR'))

    # Nueva cita desde la ficha: contribuyente preseleccionado y bloqueado (si es valido y esta activo)
    if ($atencion.Entro) {
        $r = Pedir $atencion.Cliente "GET" "/citas?accion=nuevo&contribuyente=$idFicha"
        Registrar "Nueva cita desde la ficha: contribuyente ya elegido y bloqueado" "readonly con el DNI y el aviso 'elegido desde su ficha'" "$($r.Estado)" `
            ($r.Estado -eq 200 -and $r.Cuerpo.Contains('elegido desde su ficha') -and $r.Cuerpo.Contains($DniPrueba) -and $r.Cuerpo -match 'readonly')
        foreach ($malo in @("abc", "999999", "1 OR 1=1")) {
            $r = Pedir $atencion.Cliente "GET" ("/citas?accion=nuevo&contribuyente=" + [uri]::EscapeDataString($malo))
            Registrar "Nueva cita con contribuyente='$malo'" "200, se ignora y se muestra el formulario normal" "$($r.Estado)" `
                ($r.Estado -eq 200 -and -not $r.Cuerpo.Contains('elegido desde su ficha') -and $r.Cuerpo.Contains('listaContribuyentes'))
        }
    }

    # Hoja de estilos de impresion publica y solo para impresion
    $r = Pedir $admin.Cliente "GET" "/assets/css/impresion.css"
    Registrar "impresion.css se sirve y es solo @media print" "200 con @media print" "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo.Contains('@media print'))

    # ------------------------------------------------------------ 8f. Paquete visual: paginacion, orden, toasts, CSP
    # Parametros hostiles de pagina, tamano y orden: usan los valores por defecto (nunca error 500 ni datos extraños)
    $hostiles = @(
        @("orden=" + [uri]::EscapeDataString("nombre;DROP TABLE contribuyente"), "orden con ;DROP TABLE"),
        @("orden=" + [uri]::EscapeDataString("1 OR 1=1"), "orden=1 OR 1=1"),
        @("orden=" + [uri]::EscapeDataString("nombre--"), "orden con --"),
        @("orden=" + [uri]::EscapeDataString("' OR '1'='1"), "orden con comillas"),
        @("dir=" + [uri]::EscapeDataString("desc; DROP TABLE contribuyente"), "dir con ;DROP TABLE"),
        @("pagina=-5", "pagina=-5"), @("pagina=999999", "pagina=999999"), @("pagina=abc", "pagina=abc"),
        @("pagina=99999999999999", "pagina enorme"), @("tamano=100000", "tamano=100000"), @("tamano=-1", "tamano=-1"), @("tamano=7", "tamano=7")
    )
    foreach ($ruta in @("/contribuyentes", "/almacen", "/usuarios")) {
        foreach ($h in $hostiles) {
            $r = Pedir $admin.Cliente "GET" ($ruta + "?" + $h[0])
            Registrar "GET $ruta con $($h[1])" "200 con el listado normal (valores por defecto), sin error 500" "$($r.Estado)" `
                ($r.Estado -eq 200 -and $r.Cuerpo.Contains('Mostrando') -and $r.Cuerpo -notmatch 'Exception|SQLSyntax|syntax error' -and $r.Cuerpo.Contains('aria-sort'))
        }
    }
    $r = Pedir $admin.Cliente "GET" "/contribuyentes?tamano=100000"
    Registrar "tamano=100000 se normaliza a 10" "selector con 10 marcado" "$($r.Cuerpo -match '<option value="10"\s+selected')" ($r.Cuerpo -match '<option value="10"\s+selected')
    $r = Pedir $admin.Cliente "GET" "/contribuyentes?tamano=50"
    Registrar "tamano=50 es valido" "selector con 50 marcado" "$($r.Cuerpo -match '<option value="50"\s+selected')" ($r.Cuerpo -match '<option value="50"\s+selected')
    $r = Pedir $admin.Cliente "GET" "/contribuyentes?orden=nombre&dir=desc"
    Registrar "Orden por columna valida (nombre, descendente)" "aria-sort=descending en el encabezado" "$($r.Cuerpo.Contains('aria-sort="descending"'))" ($r.Cuerpo.Contains('aria-sort="descending"'))
    $r = Pedir $admin.Cliente "GET" "/contribuyentes?orden=nombre;DROP"
    Registrar "Orden invalido: el encabezado marca el orden por defecto" "aria-sort=ascending en el nombre" "$($r.Cuerpo.Contains('aria-sort="ascending"'))" ($r.Cuerpo.Contains('aria-sort="ascending"'))
    $r = Pedir $admin.Cliente "GET" "/contribuyentes?q=$DniPrueba"
    Registrar "La tabla de contribuyentes sigue existiendo y busca" "encuentra el contribuyente de prueba" "$($r.Cuerpo.Contains($DniPrueba))" ($r.Cuerpo.Contains($DniPrueba))
    $r = Pedir $admin.Cliente "GET" "/almacen/movimiento?accion=historial&id=1&orden=x%3BDROP&pagina=-5&tamano=100000"
    Registrar "Historial de movimientos con parametros hostiles" "200 o vuelta al listado, nunca 500" "$($r.Estado)" ($r.Estado -eq 200 -or $r.Estado -eq 302)
    $r = Pedir $admin.Cliente "GET" ($rutaFicha + "&orden=x%3BDROP&pagina=999999&tamano=7")
    Registrar "Ficha con parametros hostiles de orden y pagina" "200 con el historial de citas" "$($r.Estado)" ($r.Estado -eq 200 -and $r.Cuerpo.Contains('Historial de citas'))

    # Toasts: el mensaje de una sola vez va en data-* de un elemento oculto (nunca en un script ni en un cuadro fijo)
    $r = Pedir $admin.Cliente "POST" "/contribuyentes" @{ accion = "baja"; id = "999999"; csrf = $token }
    $r = Pedir $admin.Cliente "GET" "/contribuyentes"
    Registrar "El mensaje flash llega como toast (data-*)" 'id="datosFlash" con data-tipo="error" y data-mensaje' "$($r.Cuerpo.Contains('id=""datosFlash""'))" `
        ($r.Cuerpo.Contains('id="datosFlash"') -and $r.Cuerpo.Contains('data-tipo="error"') -and $r.Cuerpo.Contains('data-mensaje="') -and $r.Cuerpo -notmatch 'alert-dismissible')
    Registrar "El contenedor de toasts es accesible (aria-live)" 'contenedorToasts con aria-live' "$($r.Cuerpo -match 'contenedorToasts[^>]*aria-live')" ($r.Cuerpo -match 'id="contenedorToasts"[^>]*aria-live="polite"')

    # Pagina comun: favicon, theme-color, tema, enlace "Saltar al contenido"
    $r = Pedir (Nuevo-Cliente) "GET" "/login"
    Registrar "Login: favicon, theme-color y boton de tema" "link rel=icon, meta theme-color y btn-tema" "$($r.Estado)" `
        ($r.Cuerpo -match 'rel="icon"' -and $r.Cuerpo -match 'name="theme-color"' -and $r.Cuerpo.Contains('btn-tema') -and $r.Cuerpo.Contains('assets/js/tema.js'))
    Registrar "Login: enlace Saltar al contenido y main con id" "saltar-contenido y id=contenido" "ok" ($r.Cuerpo.Contains('saltar-contenido') -and $r.Cuerpo -match '<main id="contenido"')
    Registrar "Login sin logo: icono generico (no se inventa un escudo)" "icono bi-building o logo.png" "ok" ($r.Cuerpo.Contains('bi-building') -or $r.Cuerpo.Contains('assets/img/logo.png'))
    $r = Pedir $admin.Cliente "GET" "/menu"
    Registrar "Menu: favicon, tema, saltar, toasts" "icon, tema.js, btn-tema, saltar-contenido, contenedorToasts" "$($r.Estado)" `
        ($r.Cuerpo -match 'rel="icon"' -and $r.Cuerpo.Contains('tema.js') -and $r.Cuerpo.Contains('btn-tema') -and $r.Cuerpo.Contains('saltar-contenido') -and $r.Cuerpo.Contains('contenedorToasts'))
    foreach ($est in @("/assets/img/favicon.svg", "/assets/js/tema.js", "/assets/js/toast.js", "/assets/js/paginacion.js")) {
        $r = Pedir (Nuevo-Cliente) "GET" $est
        Registrar "Recurso estatico $est" "200 (publico)" "$($r.Estado)" ($r.Estado -eq 200)
    }

    # CSP: sigue presente en login, listados y ficha, y no permite scripts en linea ni de otros origenes
    foreach ($ruta in @("/login", "/menu", "/contribuyentes", "/almacen", "/usuarios", "/auditoria", $rutaFicha)) {
        $cli = if ($ruta -eq "/login") { Nuevo-Cliente } else { $admin.Cliente }
        $r = Pedir $cli "GET" $ruta
        $csp = if ($r.Cabeceras.Contains("Content-Security-Policy")) { ($r.Cabeceras.GetValues("Content-Security-Policy") -join " ") } else { "" }
        Registrar "CSP presente en $ruta" "script-src 'self' sin unsafe-inline ni CDN" "$($csp.Length) caracteres" `
            ($csp -match "script-src 'self'" -and $csp -notmatch "script-src[^;]*unsafe" -and $csp -notmatch "https?://")
    }

    # Ningun JSP contiene scripts en linea ni manejadores onclick/onerror/... (busca en el codigo fuente)
    $vistas = Join-Path $PSScriptRoot "src\main\webapp"
    if (Test-Path $vistas) {
        $archivos = @(Get-ChildItem -Path $vistas -Recurse -Include *.jsp, *.tag | Where-Object { $_.FullName -notmatch '\\assets\\' })
        $enLinea = @($archivos | Select-String -Pattern '<script\b(?![^>]*\bsrc\s*=)' | Where-Object { $_.Line -notmatch '<%--' })
        $manejadores = @($archivos | Select-String -Pattern '\son(click|change|submit|load|error|input|focus|blur|key[a-z]*|mouse[a-z]*|dblclick|select|reset|toggle|unload)\s*=')
        $javascriptUrl = @($archivos | Select-String -Pattern 'javascript\s*:' )
        $estiloLinea = @($archivos | Select-String -Pattern '<style\b')
        Registrar "JSP/tags revisados: $($archivos.Count) archivos" "se encontraron archivos para revisar" "$($archivos.Count)" ($archivos.Count -gt 10)
        Registrar "Ningun JSP tiene <script> en linea" "0 coincidencias" "$($enLinea.Count) $(($enLinea | Select-Object -First 2 | ForEach-Object { $_.Path + ':' + $_.LineNumber }) -join ' ')" ($enLinea.Count -eq 0)
        Registrar "Ningun JSP tiene onclick/onerror/onchange/onload..." "0 coincidencias" "$($manejadores.Count) $(($manejadores | Select-Object -First 2 | ForEach-Object { $_.Path + ':' + $_.LineNumber }) -join ' ')" ($manejadores.Count -eq 0)
        Registrar "Ningun JSP usa javascript: en enlaces" "0 coincidencias" "$($javascriptUrl.Count)" ($javascriptUrl.Count -eq 0)
        Registrar "Ningun JSP tiene <style> en linea" "0 coincidencias" "$($estiloLinea.Count)" ($estiloLinea.Count -eq 0)
        $cdn = @($archivos | Select-String -Pattern '(src|href)\s*=\s*"https?://')
        Registrar "Ningun JSP enlaza a CDN ni fuentes externas" "0 coincidencias" "$($cdn.Count)" ($cdn.Count -eq 0)
    } else {
        Registrar "Revision de JSP (grep)" "carpeta src\main\webapp disponible" "no se encontro $vistas" $false
    }

    # ------------------------------------------------------------ 9. Cierre de sesion
    $r = Pedir $admin.Cliente "GET" "/logout"
    $r = Pedir $admin.Cliente "GET" "/menu"
    Registrar "Despues de cerrar sesion, /menu pide login" "302 hacia /login" "$($r.Estado) -> $($r.Location)" `
        ($r.Estado -eq 302 -and $r.Location -like "*/login")
    $r = Pedir $admin.Cliente "GET" "/auditoria"
    Registrar "Despues de cerrar sesion, /auditoria pide login" "302 hacia /login" "$($r.Estado) -> $($r.Location)" `
        ($r.Estado -eq 302 -and $r.Location -like "*/login")
}
catch {
    Registrar "Ejecucion del script" "sin errores" "$_" $false
}
finally {
    # ---------------------------------------------------------------- limpieza del dato de prueba
    $limpieza = "no se ejecuto"
    try {
        if ((Test-Path $ConectorJar) -and (Test-Path $PropiedadesBd)) {
            $limpieza = (& $Java -cp $ConectorJar (Join-Path $PSScriptRoot "tools\LimpiarPruebaSeguridad.java") $PropiedadesBd $DniPrueba $UsuarioPrueba 2>&1 | Out-String).Trim()
        } else {
            $limpieza = "no se encontro el conector MySQL o db.properties; borre a mano el contribuyente con DNI $DniPrueba y el usuario $UsuarioPrueba"
        }
    } catch { $limpieza = "error al limpiar: $_" }
    Write-Host ""
    Write-Host "Limpieza de los datos de prueba (contribuyente DNI $DniPrueba y usuario $UsuarioPrueba): $limpieza"
}

# ---------------------------------------------------------------- resultados
Write-Host ""
$resultados | Format-Table -AutoSize -Wrap Resultado, Prueba, Esperado, Obtenido | Out-String -Width 250 | Write-Host
$aprobadas = @($resultados | Where-Object { $_.Resultado -eq "APROBADO" }).Count
$fallidas = @($resultados | Where-Object { $_.Resultado -eq "FALLIDO" }).Count
$color = $(if ($fallidas -eq 0) { "Green" } else { "Red" })
Write-Host "Resumen: $aprobadas APROBADAS, $fallidas FALLIDAS de $($resultados.Count) pruebas." -ForegroundColor $color
if ($fallidas -gt 0) { exit 1 }

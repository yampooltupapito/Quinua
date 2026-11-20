# desplegar.ps1 - Compila SIGMUD, lo despliega en Tomcat y arranca Tomcat.
# Uso (desde la raiz del proyecto):
#   .\desplegar.ps1             compila (sin correr pruebas), despliega y arranca Tomcat
#   .\desplegar.ps1 -Pruebas    antes de desplegar corre las pruebas JUnit (mvn test); si alguna falla, no despliega
# Cierre Tomcat con Ctrl+C en esta ventana.
param([switch]$Pruebas)

# ===== RUTAS (cambie solo esta seccion) =====
$JavaHome    = "C:\Program Files\Java\jdk-25"
$Mvn         = "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd"
$TomcatHome  = "C:\Users\ASUS\Documents\toncat\apache-tomcat-10.1.61\apache-tomcat-10.1.61"
$NombreApp   = "sigmud"
$PuertoHttp  = 8080
# ============================================

$ErrorActionPreference = "Stop"
$Proyecto = $PSScriptRoot
$War      = Join-Path $Proyecto "target\$NombreApp.war"
$Webapps  = Join-Path $TomcatHome "webapps"
$Work     = Join-Path $TomcatHome "work\Catalina\localhost"

function Paso($texto) { Write-Host "`n==> $texto" -ForegroundColor Green }

# 0. Verificaciones previas
foreach ($ruta in @($JavaHome, $Mvn, $TomcatHome)) {
    if (-not (Test-Path $ruta)) { throw "No existe la ruta: $ruta (revise las variables al inicio del script)" }
}
if (Get-NetTCPConnection -LocalPort $PuertoHttp -State Listen -ErrorAction SilentlyContinue) {
    throw "El puerto $PuertoHttp ya esta en uso. Cierre el Tomcat que esta corriendo (o NetBeans) e intente de nuevo."
}

# 1. (opcional) Pruebas JUnit: -Pruebas
$env:JAVA_HOME = $JavaHome
Push-Location $Proyecto
try {
    if ($Pruebas) {
        Paso "Ejecutando las pruebas JUnit (mvn test)"
        & $Mvn --no-transfer-progress clean test
        if ($LASTEXITCODE -ne 0) { throw "Fallaron las pruebas (codigo $LASTEXITCODE): no se despliega" }
    }

    # 2. Compilar y empaquetar (las pruebas ya se corrieron arriba si se pidio -Pruebas)
    Paso "Compilando con Maven"
    & $Mvn --no-transfer-progress clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "Fallo la compilacion (codigo $LASTEXITCODE)" }
} finally {
    Pop-Location
}
if (-not (Test-Path $War)) { throw "No se genero $War" }

# 3. Limpiar despliegue anterior
Paso "Borrando despliegue anterior"
Get-ChildItem -Path $Webapps -Filter "$NombreApp*" -ErrorAction SilentlyContinue |
    Remove-Item -Recurse -Force
if (Test-Path (Join-Path $Work $NombreApp)) {
    Remove-Item (Join-Path $Work $NombreApp) -Recurse -Force
}

# 4. Copiar WAR
Paso "Copiando $NombreApp.war a webapps"
Copy-Item $War $Webapps

# 5. Arrancar Tomcat en esta ventana
Paso "Iniciando Tomcat: http://localhost:$PuertoHttp/$NombreApp/"
$env:CATALINA_HOME = $TomcatHome
& (Join-Path $TomcatHome "bin\catalina.bat") run

$ErrorActionPreference = 'Stop'

$jar = Join-Path $PSScriptRoot 'target/backend-1.0.0.jar'
if (!(Test-Path -LiteralPath $jar)) { throw 'Primero compila el backend según README.md.' }
$java = Get-Command java.exe -ErrorAction Stop
$schema = Read-Host 'Nombre de la base MySQL [llaqtamasi_ayni]'
if (!$schema) { $schema = 'llaqtamasi_ayni' }
if ($schema -notmatch '^[a-zA-Z0-9_]+$') { throw 'Usa un nombre de base con letras, números o guiones bajos.' }

$env:DB_URL = "jdbc:mysql://localhost:3306/${schema}?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"
$env:DB_USER = Read-Host 'Usuario de MySQL'
$env:DB_PASSWORD = Read-Host 'Contraseña de MySQL' -MaskInput
$env:ADMIN_LOGIN = Read-Host 'Nuevo usuario administrador de prueba [demo_android]'
if (!$env:ADMIN_LOGIN) { $env:ADMIN_LOGIN = 'demo_android' }
$env:ADMIN_PASSWORD = Read-Host 'Contraseña para entrar a Ayni (mínimo 12 caracteres)' -MaskInput
if ($env:ADMIN_PASSWORD.Length -lt 12 -or [Text.Encoding]::UTF8.GetByteCount($env:ADMIN_PASSWORD) -gt 72) {
    throw 'La contraseña debe tener al menos 12 caracteres y ocupar como máximo 72 bytes.'
}
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
$env:PORT = '18080'
$api = 'http://127.0.0.1:18080/api'
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 18080)
try { $listener.Start() }
catch { throw 'El puerto 18080 está ocupado. Cierra el backend anterior antes de arrancar la demo.' }
finally { $listener.Stop() }

$log = Join-Path $PSScriptRoot 'demo.log'
$errorLog = Join-Path $PSScriptRoot 'demo-error.log'
$backend = Start-Process -FilePath $java.Source -ArgumentList '-jar', ('"' + $jar + '"'), '--server.address=127.0.0.1' -WindowStyle Hidden -RedirectStandardOutput $log -RedirectStandardError $errorLog -PassThru
try {
    $sesion = $null
    for ($intento = 0; $intento -lt 40; $intento++) {
        if ($backend.HasExited) { throw "El backend se detuvo. Revisa $log y $errorLog." }
        try {
            $sesion = Invoke-RestMethod "$api/auth/login" -Method Post -ContentType 'application/json' -TimeoutSec 3 -Body (@{
                nombreAcceso = $env:ADMIN_LOGIN
                password = $env:ADMIN_PASSWORD
            } | ConvertTo-Json)
            break
        } catch {
            if ($_.Exception.Response.StatusCode -eq 401 -and $intento -gt 15) {
                throw 'El usuario ya existe con otras credenciales. Usa su contraseña o elige un nombre nuevo.'
            }
            Start-Sleep -Seconds 1
        }
    }
    if (!$sesion.accessToken) { throw "No se pudo iniciar sesión. Revisa $log." }
    $headers = @{ Authorization = 'Bearer ' + $sesion.accessToken }
    $zona = 'aa181e35-7d87-4961-98dd-990f52f36111'
    $centro = '8cd61a01-f5f1-4645-b293-9750449e36be'
    $zonas = Invoke-RestMethod "$api/zonas" -Headers $headers
    if ($zona -notin $zonas.id) {
        Invoke-RestMethod "$api/zonas" -Method Post -Headers $headers -ContentType 'application/json' -Body (@{
            id = $zona
            nombre = 'Arequipa - Demo Android'
            limiteWkt = 'MULTIPOLYGON(((-71.56 -16.43,-71.50 -16.43,-71.50 -16.37,-71.56 -16.37,-71.56 -16.43)))'
        } | ConvertTo-Json) | Out-Null
    }
    $centros = Invoke-RestMethod "$api/centros?zonaId=$zona" -Headers $headers
    if ($centro -notin $centros.id) {
        Invoke-RestMethod "$api/centros" -Method Post -Headers $headers -ContentType 'application/json' -Body (@{
            id = $centro
            zonaId = $zona
            nombre = 'Centro de acopio de prueba'
            tipo = 'ACOPIO'
            lat = -16.3989
            lng = -71.5369
            horario = 'Datos de demostración'
        } | ConvertTo-Json) | Out-Null
    }
    Write-Host "Abre Mapa en el emulador e inicia sesión como $env:ADMIN_LOGIN con la contraseña que elegiste."
    Write-Host 'Selecciona Arequipa - Demo Android. Mantén esta ventana abierta durante la prueba.'
    $backend.WaitForExit()
} finally {
    if (!$backend.HasExited) { Stop-Process -Id $backend.Id }
}

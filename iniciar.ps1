$ErrorActionPreference = 'Stop'
foreach ($name in @('DB_USER', 'DB_PASSWORD', 'JWT_SECRET')) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Configura $name en esta sesion de PowerShell. Consulta README.md."
    }
}
$jar = Join-Path $PSScriptRoot 'target/backend-1.0.0.jar'
if (-not (Test-Path -LiteralPath $jar)) {
    throw 'Primero compila con .\mvnw.cmd package'
}
& java -jar $jar
exit $LASTEXITCODE

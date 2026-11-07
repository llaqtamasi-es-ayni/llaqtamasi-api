@echo off
setlocal
set "MAVEN=%~dp0.tools\apache-maven-3.9.9\bin\mvn.cmd"
if not exist "%MAVEN%" (
  echo Maven no esta descargado. Ejecuta preparar.ps1 primero.
  exit /b 1
)
call "%MAVEN%" %*

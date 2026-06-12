@echo off
setlocal
chcp 65001

set "ROOT=%~dp0"
set "JAVA_HOME=%ROOT%..\BeiDou-Server-1.9\jdk-21.0.2"
set "JAR=%ROOT%gms-server\target\BeiDou.jar"
set "CONFIG=%ROOT%gms-server\src\main\resources\application.yml"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo JDK 21 not found: "%JAVA_HOME%"
  exit /b 1
)

if not exist "%JAR%" (
  echo Built jar not found: "%JAR%"
  echo Run build-windows.bat first.
  exit /b 1
)

pushd "%ROOT%gms-server"
"%JAVA_HOME%\bin\java.exe" -Dspring.config.location="%CONFIG%" -jar "%JAR%"
popd


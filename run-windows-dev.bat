@echo off
setlocal
chcp 65001

set "ROOT=%~dp0"
set "JAVA_HOME=%ROOT%jdk-21.0.2"
set "JAR=%ROOT%gms-server\target\BeiDou.jar"
set "CONFIG=%ROOT%gms-server\src\main\resources\application.yml"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo JDK 21 not found: "%JAVA_HOME%"
  goto :fail
)

if not exist "%JAR%" (
  echo Built jar not found: "%JAR%"
  echo Run build-windows.bat first.
  goto :fail
)

pushd "%ROOT%gms-server"
"%JAVA_HOME%\bin\java.exe" -Dspring.config.location="%CONFIG%" -jar "%JAR%"
set "EXIT_CODE=%ERRORLEVEL%"
popd
if not "%EXIT_CODE%"=="0" goto :fail

exit /b 0

:fail
echo.
echo run-windows-dev.bat failed.
pause
exit /b 1

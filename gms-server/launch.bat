@echo off
@title BeiDou
chcp 65001

set "SCRIPT_DIR=%~dp0"
set "ROOT=%SCRIPT_DIR%.."

"%ROOT%\jdk-21.0.2\bin\java.exe" -Dspring.config.location="%SCRIPT_DIR%application.yml" -jar "%SCRIPT_DIR%BeiDou.jar"
pause

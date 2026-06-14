@echo off
setlocal

set "ROOT=%~dp0"
set "JAVA_HOME=%ROOT%..\BeiDou-Server-1.9\jdk-21.0.2"
set "MAVEN=%ProgramFiles%\JetBrains\IntelliJ IDEA Community Edition 2025.2\plugins\maven\lib\maven3\bin\mvn.cmd"
set "STATIC_DIR=%ROOT%gms-server\src\main\resources\static"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo JDK 21 not found: "%JAVA_HOME%"
  exit /b 1
)

if not exist "%MAVEN%" (
  echo Maven not found: "%MAVEN%"
  echo Install Maven or update this script to point at your mvn.cmd.
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"

pushd "%ROOT%gms-ui"
call yarn install --frozen-lockfile
if errorlevel 1 exit /b 1
call yarn build
if errorlevel 1 exit /b 1
popd

if exist "%STATIC_DIR%" rmdir /s /q "%STATIC_DIR%"
mkdir "%STATIC_DIR%"
xcopy "%ROOT%gms-ui\dist\*" "%STATIC_DIR%\" /E /I /Y >nul

pushd "%ROOT%"
call "%MAVEN%" -B -ntp -pl gms-server -am -DskipTests clean package
popd

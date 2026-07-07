@echo off
setlocal

set "ROOT=%~dp0"
set "JAVA_HOME=%ROOT%jdk-21.0.2"
set "MAVEN=%ProgramFiles%\JetBrains\IntelliJ IDEA Community Edition 2025.2\plugins\maven\lib\maven3\bin\mvn.cmd"
set "STATIC_DIR=%ROOT%gms-server\src\main\resources\static"

echo.
echo ==============================
echo BeiDou Windows Build
echo ==============================
echo 1. build 遊戲邏輯
echo 2. Build 網頁後台
echo 3. Build 遊戲邏輯 + 網頁後台
echo 4. 關閉
echo.
set /p "BUILD_CHOICE=Select [1-4]: "

if "%BUILD_CHOICE%"=="1" (
  call :build_game
  if errorlevel 1 goto :fail
  goto :success
)

if "%BUILD_CHOICE%"=="2" (
  call :build_web
  if errorlevel 1 goto :fail
  goto :success
)

if "%BUILD_CHOICE%"=="3" (
  call :build_web
  if errorlevel 1 goto :fail
  call :build_game
  if errorlevel 1 goto :fail
  goto :success
)

if "%BUILD_CHOICE%"=="4" (
  echo Build cancelled.
  exit /b 0
)

echo Invalid option: %BUILD_CHOICE%
goto :fail

:build_web
echo.
echo [1/1] Building web admin code...

pushd "%ROOT%gms-ui"
call yarn install --frozen-lockfile
if errorlevel 1 (
  popd
  exit /b 1
)
call "%ROOT%gms-ui\node_modules\.bin\vue-tsc.cmd" --noEmit
if errorlevel 1 (
  popd
  exit /b 1
)
call "%ROOT%gms-ui\node_modules\.bin\vite.cmd" build --config "%ROOT%gms-ui\config\vite.config.prod.ts"
if errorlevel 1 (
  popd
  exit /b 1
)
popd

if exist "%STATIC_DIR%" rmdir /s /q "%STATIC_DIR%"
mkdir "%STATIC_DIR%"
xcopy "%ROOT%gms-ui\dist\*" "%STATIC_DIR%\" /E /I /Y >nul
if errorlevel 1 exit /b 1

echo Web admin code build completed.
exit /b 0

:build_game
echo.
echo [1/1] Building game code...

call :prepare_java_maven
if errorlevel 1 exit /b 1

call powershell -NoProfile -ExecutionPolicy Bypass -File "%ROOT%tools\check-flyway-migrations.ps1"
if errorlevel 1 exit /b 1

pushd "%ROOT%"
call "%MAVEN%" -B -ntp -pl gms-server -am -DskipTests clean package
set "EXIT_CODE=%ERRORLEVEL%"
popd
if not "%EXIT_CODE%"=="0" exit /b %EXIT_CODE%

echo Game code build completed.
exit /b 0

:prepare_java_maven
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo JDK 21 not found: "%JAVA_HOME%"
  exit /b 1
)

if not exist "%JAVA_HOME%\bin\javac.exe" (
  echo Full JDK 21 is required for packaging: "%JAVA_HOME%"
  echo This folder looks like a JRE. Replace it with a JDK 21 build that includes javac.exe.
  exit /b 1
)

if not exist "%MAVEN%" (
  echo Maven not found: "%MAVEN%"
  echo Install Maven or update this script to point at your mvn.cmd.
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
exit /b 0

:success
echo.
echo build-windows.bat completed.
pause
exit /b 0

:fail
echo.
echo build-windows.bat failed.
pause
exit /b 1

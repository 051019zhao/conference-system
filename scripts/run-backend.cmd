@echo off
setlocal
set "PROJECT_DIR=%~dp0.."
if exist "%~dp0local.env.cmd" call "%~dp0local.env.cmd"
where java >nul 2>&1 || (echo JDK 17 is required on PATH. & exit /b 1)
where mvn >nul 2>&1 || (echo Maven is required on PATH. & exit /b 1)
pushd "%PROJECT_DIR%\backend" || exit /b 1
call mvn spring-boot:run
set "RESULT=%ERRORLEVEL%"
popd
exit /b %RESULT%

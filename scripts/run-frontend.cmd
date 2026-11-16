@echo off
setlocal
set "PROJECT_DIR=%~dp0.."
where npm >nul 2>&1 || (echo Node.js and npm are required on PATH. & exit /b 1)
if not defined VITE_API_TARGET (
    for /f %%S in ('docker inspect --format "{{.State.Running}}" conference-backend 2^>nul') do if "%%S"=="true" set "VITE_API_TARGET=http://127.0.0.1:18080"
)
pushd "%PROJECT_DIR%\frontend" || exit /b 1
if not exist node_modules call npm ci || (popd & exit /b 1)
call npm run dev
set "RESULT=%ERRORLEVEL%"
popd
exit /b %RESULT%

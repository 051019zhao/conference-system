@echo off
setlocal
set "PROJECT_DIR=%~dp0.."
where npm >nul 2>&1 || (echo Node.js and npm are required on PATH. & exit /b 1)
pushd "%PROJECT_DIR%\frontend" || exit /b 1
if not exist node_modules call npm ci || (popd & exit /b 1)
call npm run dev
set "RESULT=%ERRORLEVEL%"
popd
exit /b %RESULT%

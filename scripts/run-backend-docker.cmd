@echo off
setlocal
for %%I in ("%~dp0..") do set "PROJECT_DIR=%%~fI"
where docker >nul 2>&1 || (echo Docker Desktop is required. & exit /b 1)
docker info >nul 2>&1 || (echo Start Docker Desktop, then try again. & exit /b 1)
call "%~dp0run-db.cmd" || exit /b 1
call "%~dp0run-backend.cmd" --package || exit /b 1

rem Use 18080 because port 8080 can be reserved on Windows.
docker inspect conference-backend >nul 2>&1
if not errorlevel 1 (
  docker rm -f conference-backend >nul || exit /b 1
)
docker run -d --name conference-backend --restart unless-stopped ^
  -p 127.0.0.1:18080:8080 ^
  -e "DB_URL=jdbc:opengauss://host.docker.internal:5432/conference_db?currentSchema=public&batchRestoreSchema=true" ^
  -e DB_USER=gaussdb -e DB_PASSWORD=Gauss@123 ^
  -v "%PROJECT_DIR%\backend\target\conference-server-1.0.0.jar:/app/app.jar:ro" ^
  -v conference_upload:/app/upload -w /app ^
  eclipse-temurin:17-jre java -jar /app/app.jar || exit /b 1
echo Backend container started at http://localhost:18080
echo Run scripts\run-frontend.cmd, then visit http://localhost:5173
exit /b 0

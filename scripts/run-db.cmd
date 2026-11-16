@echo off
setlocal EnableDelayedExpansion
where docker >nul 2>&1 || (echo Docker Desktop is required. & exit /b 1)
docker info >nul 2>&1 || (echo Start Docker Desktop, then try again. & exit /b 1)

docker inspect opengauss >nul 2>&1
if errorlevel 1 (
    pushd "%~dp0.." || exit /b 1
    docker compose up -d || (popd & exit /b 1)
    popd
) else (
    for /f %%S in ('docker inspect --format "{{.State.Running}}" opengauss') do set "DB_RUNNING=%%S"
    if not "!DB_RUNNING!"=="true" docker start opengauss || exit /b 1
)

for /l %%N in (1,1,60) do (
    for /f %%H in ('docker inspect --format "{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}" opengauss') do set "DB_STATUS=%%H"
    if "!DB_STATUS!"=="healthy" (echo openGauss is ready. & exit /b 0)
    if "!DB_STATUS!"=="running" (echo openGauss is running. & exit /b 0)
    timeout /t 2 /nobreak >nul
)
echo openGauss did not become ready. Run docker logs opengauss for details.
exit /b 1

@echo off
setlocal
set "PROJECT_DIR=%~dp0.."
if exist "%~dp0local.env.cmd" call "%~dp0local.env.cmd"

rem Prefer an explicit JAVA_HOME, then find a local JDK 17 installation.
if defined JAVA_HOME if not exist "%JAVA_HOME%\bin\java.exe" set "JAVA_HOME="
if not defined JAVA_HOME for /d %%J in ("%USERPROFILE%\.jdks\ms-17*" "%USERPROFILE%\.jdks\jdk-17*" "%ProgramFiles%\Java\jdk-17*") do (
    if exist "%%~fJ\bin\java.exe" set "JAVA_HOME=%%~fJ"
)
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
for /f "tokens=3" %%V in ('java -version 2^>^&1 ^| findstr /C:"version"') do set "JAVA_VERSION=%%~V"
if not "%JAVA_VERSION:~0,3%"=="17." (
    echo JDK 17 is required. Set JAVA_HOME to a JDK 17 installation.
    exit /b 1
)

rem Use Maven on PATH, MAVEN_HOME, or a bundled IntelliJ IDEA Maven.
where mvn.cmd >nul 2>&1 && set "MAVEN_CMD=mvn.cmd"
if not defined MAVEN_CMD if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
if not defined MAVEN_CMD for /d %%I in ("%ProgramFiles%\JetBrains\IntelliJ IDEA*") do (
    if exist "%%~fI\plugins\maven\lib\maven3\bin\mvn.cmd" set "MAVEN_CMD=%%~fI\plugins\maven\lib\maven3\bin\mvn.cmd"
)
if not defined MAVEN_CMD (
    echo Maven was not found. Install Maven or set MAVEN_HOME.
    exit /b 1
)
if /i "%~1"=="--check" (
    echo JAVA_HOME=%JAVA_HOME%
    echo MAVEN_CMD=%MAVEN_CMD%
    exit /b 0
)

if not defined DB_URL call "%~dp0run-db.cmd" || exit /b 1
pushd "%PROJECT_DIR%\backend" || exit /b 1
call "%MAVEN_CMD%" spring-boot:run
set "RESULT=%ERRORLEVEL%"
popd
exit /b %RESULT%

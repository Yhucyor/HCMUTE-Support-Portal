@echo off
setlocal
echo =======================================================
echo   AUTO START DOCKER ^& AI SERVICE (QAUTE HCMUTE)
echo =======================================================

tasklist /FI "IMAGENAME eq Docker Desktop.exe" 2>NUL | find /I /N "Docker Desktop.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo [OK] Docker Desktop is already running.
) else (
    echo [INFO] Starting Docker Desktop...
    start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
)

echo [INFO] Waiting for Docker daemon to respond...
set /a count=0

:WAIT_LOOP
timeout /t 3 /nobreak >nul
docker info >nul 2>&1
if "%ERRORLEVEL%"=="0" goto DOCKER_READY

set /a count+=1
if %count% geq 20 (
    echo [WARN] Docker took more than 60s. Please check Docker Desktop in System Tray.
    goto DOCKER_START_CONTAINERS
)
echo   ... waiting for Docker daemon (%count%/20)
goto WAIT_LOOP

:DOCKER_READY
echo [OK] Docker daemon is ready!

:DOCKER_START_CONTAINERS
echo [INFO] Starting AI Service containers...
cd /d "%~dp0"
docker compose up -d

echo.
echo =======================================================
echo   AI Service status:
echo =======================================================
docker ps --filter "name=hcmute-chatbot" --filter "name=qdrant"
endlocal

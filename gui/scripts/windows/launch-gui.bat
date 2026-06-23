@echo off
setlocal

REM IDEasy GUI Launcher for Windows

set "SCRIPT_DIR=%~dp0"

REM Desktop/Start-Menu shortcuts are spawned by explorer.exe, which caches its
REM environment from login and does not pick up registry PATH/IDE_ROOT changes
REM made by the IDEasy installer until the user logs off or restarts explorer.
REM Re-read IDE_ROOT directly from the registry and extend PATH with it here,
REM so the shortcut works even with explorer's stale inherited environment.
if not defined IDE_ROOT (
    for /f "tokens=2,*" %%A in ('reg query "HKCU\Environment" /v IDE_ROOT 2^>nul') do set "IDE_ROOT=%%B"
)
if defined IDE_ROOT (
    set "PATH=%IDE_ROOT%\_ide\installation\bin;%PATH%"
)

REM Check if ide command exists
where ide >nul 2>&1 || (
    echo.
    echo Error: IDEasy is not installed or not in PATH
    echo If you just installed IDEasy, log off/on once or restart Explorer so
    echo the desktop shortcut picks up the updated environment.
    echo https://github.com/devonfw/IDEasy#setup
    echo.
    pause
    exit /b 1
)

REM Determine project root safely
pushd "%SCRIPT_DIR%..\..\.."
set "PROJECT_ROOT=%CD%"
popd

REM Check project structure
if not exist "%PROJECT_ROOT%\pom.xml" (
    echo Error: IDEasy project root not found at %PROJECT_ROOT%
    echo.
    pause
    exit /b 1
)

REM Launch GUI
cd /d "%PROJECT_ROOT%"
echo Starting IDEasy GUI...
START "" /B ide gui >> "%USERPROFILE%\.ideasy-gui.log" 2>&1

exit /b 0

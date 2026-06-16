@echo off
setlocal

REM IDEasy GUI Launcher for Windows

set "SCRIPT_DIR=%~dp0"

REM Check if ide command exists
where ide >nul 2>&1 || (
    echo.
    echo Error: IDEasy is not installed or not in PATH
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
START "" ide gui

exit /b 0
@echo off
setlocal EnableDelayedExpansion

set "INSTALL_FOLDER=%~1"
set "IDEASY_CREATE_DESKTOP_SHORTCUT=%~2"
set "IDEASY_CREATE_STARTMENU_SHORTCUT=%~3"

cd /d "%INSTALL_FOLDER%" || exit /b 1

if not exist "%USERPROFILE%\.ide\logs" (
  mkdir "%USERPROFILE%\.ide\logs"
)

set "ts=%date:/=-%_%time::=-%"
set "ts=!ts:,=-!"

"%INSTALL_FOLDER%\bin\ideasy.exe" -ftb --no-colors install > "%USERPROFILE%\.ide\logs\install-!ts!.log" 2>&1
set "exitCode=!ERRORLEVEL!"

exit /b %ERRORLEVEL%

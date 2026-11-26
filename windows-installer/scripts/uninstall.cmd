@echo off
setlocal EnableDelayedExpansion

set "INSTALL_FOLDER=%~1"

cd /d "%INSTALL_FOLDER%" || exit /b 1

if not exist "%USERPROFILE%\.ide\logs" (
  mkdir "%USERPROFILE%\.ide\logs"
)

set "ts=%date:/=-%_%time::=-%"
set "ts=!ts:,=-!"

"%INSTALL_FOLDER%\bin\ideasy.exe" -ft --no-colors uninstall > "%USERPROFILE%\.ide\logs\uninstall-!ts!.log" 2>&1

exit /b %ERRORLEVEL%

@echo off
if "%1%" == "--version" (
  for /f "delims=" %%i in (%~dp0..\.ide.software.version) do @echo OpenJDK version %%i
) else (
  echo java %*
)

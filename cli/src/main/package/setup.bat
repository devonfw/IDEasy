@echo off

Set _fBYellow=[93m
Set _fBGreen=[92m
Set _fBRed=[91m
Set _RESET=[0m

pushd %~dp0
echo Setting up IDEasy from %CD%

REM activate ANSI support for colors in native terminals (cmd and powershell)
reg import system/windows/terminal/Enable_ANSI_encoding_native_terminal.reg >nul 2>&1

REM find bash on your Windows system...
for %%H in ( HKEY_LOCAL_MACHINE HKEY_CURRENT_USER ) do for /F "usebackq tokens=2*" %%O in (`call "%SystemRoot%"\system32\reg.exe query "%%H\Software\GitForWindows" /v "InstallPath" 2^>nul ^| "%SystemRoot%\system32\findstr.exe" REG_SZ`) do set GIT_HOME=%%P

if exist "%GIT_HOME%\bin\bash.exe" (
  set "BASH=%GIT_HOME%\bin\bash.exe"
  set "HOME=%USERPROFILE%"
  goto :bash_detected
)

echo %_fBYellow%WARNING: Git-bash is required but was not found at GIT_HOME=%GIT_HOME%.%_RESET%

REM If bash can not be autodetected allow the user to configure bash via BASH_HOME environment variable as fallback

if exist "%BASH_HOME%\bin\bash.exe" (
  set "BASH=%BASH_HOME%\bin\bash.exe"
  set "HOME=%USERPROFILE%"
  goto :bash_detected
)
echo:
echo %_fBYellow%*** ATTENTION ***%_RESET%
echo %_fBRed%ERROR: Could not find bash. It seems git for windows is not installed on your machine%_RESET%
echo %_fBRed%Please download and install git for windows from the following URL and after that rerun IDEasy setup:%_RESET%
echo %_fBRed%https://git-scm.com/download/win%_RESET%
exit /b 5

:bash_detected
echo Found bash at %BASH%
"%BASH%" -c "cd \"%CD%\";./setup"
if %ERRORLEVEL% neq 0 (
  echo %_fBRed%Error occurred while running setup of IDEasy in bash.%_RESET%
  pause
  exit /b %ERRORLEVEL%
)
echo %_fBGreen%Setup of IDEasy completed%_RESET%
if not "%1%" == "-b" (
  pause
)
popd
goto :EOF

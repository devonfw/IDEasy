@echo off

Set _fBYellow=[93m
Set _fBGreen=[92m
Set _fBRed=[91m
Set _RESET=[0m

pushd %~dp0
echo Setting up IDEasy from %CD%

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
for /F "tokens=2* delims= " %%f IN ('reg query HKCU\Environment /v PATH ^| findstr /i PATH') do set USER_PATH=%%g
if "%USER_PATH:~-1,1%" == ";" (
  set "USER_PATH=%USER_PATH:~0,-1%"
)
echo Adding %IDE_ROOT%\_ide\installation\bin to your users system PATH
if "%USER_PATH%" == "" (
  echo %_fBYellow%ATTENTION:
  echo Your user specific PATH variable seems to be empty.
  echo You can double check this by pressing [Windows][r] and launch the program SystemPropertiesAdvanced.
  echo Then click on 'Environment variables' and check if 'PATH' is set in in the 'user variables' from the upper list.
  echo In case 'PATH' is defined there non-empty and you get this message, please abort and give us feedback:
  echo https://github.com/devonfw/IDEasy/issues
  echo Otherwise all is correct and you can continue by pressing enter.
  echo %_RESET%
  pause
  setx PATH "%IDE_ROOT%\_ide\installation\bin"
) else (
  setx PATH "%IDE_ROOT%\_ide\installation\bin;%USER_PATH%"
)
echo %_fBGreen%Setup of IDEasy completed%_RESET%
if not "%1%" == "-b" (
  pause
)
popd
goto :EOF

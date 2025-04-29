@echo off

Set _fBYellow=[93m
Set _fBGreen=[92m
Set _fBRed=[91m
Set _RESET=[0m

rem Add Git PATH entries for CMD - https://github.com/devonfw/IDEasy/issues/764
for %%H in ( HKEY_LOCAL_MACHINE HKEY_CURRENT_USER ) do for /F "usebackq tokens=2*" %%O in (`call "%SystemRoot%"\system32\reg.exe query "%%H\Software\GitForWindows" /v "InstallPath" 2^>nul ^| "%SystemRoot%\system32\findstr.exe" REG_SZ`) do set GIT_HOME=%%P

set "GIT_BIN=%GIT_HOME%\usr\bin"
set "GIT_CORE=%GIT_HOME%\mingw64\libexec\git-core"

if exist "%GIT_BIN%" (
  echo "%PATH%" | find /i "%GIT_BIN%" >nul || set "PATH=%PATH%;%GIT_BIN%"
)

if exist "%GIT_CORE%" (
  echo "%PATH%" | find /i "%GIT_CORE%" >nul || set "PATH=%PATH%;%GIT_CORE%"
)

if not "%1%" == "" (
  ideasy %IDE_OPTIONS% %*
  if not %ERRORLEVEL% == 0 (
    echo %_fBRed%Error: IDEasy failed with exit code %ERRORLEVEL% %_RESET%
    call :echoUseBash
    exit /b %ERRORLEVEL%
  )
)

REM https://stackoverflow.com/questions/61888625/what-is-f-in-the-for-loop-command
for /f "tokens=*" %%i in ('ideasy %IDE_OPTIONS% env') do (
  call set %%i
)

ideasy %IDE_OPTIONS% env >nul

if %ERRORLEVEL% == 0 (
  echo IDE environment variables have been set for %IDE_HOME% in workspace %WORKSPACE%
)

call :echoUseBash
goto :eof

:echoUseBash
  echo.
  echo %_fBYellow%Please use ^(git-^)bash ^(integrated in Windows Terminal^) for full IDEasy support:
  echo https://github.com/devonfw/IDEasy/blob/main/documentation/advanced-tooling-windows.adoc#tabs-for-shells %_RESET%
  exit /b

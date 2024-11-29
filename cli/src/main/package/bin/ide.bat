@echo off

Set _fBYellow=[93m
Set _fBGreen=[92m
Set _fBRed=[91m
Set _RESET=[0m

if not "%1%" == "" (
  ideasy %IDE_OPTIONS% %*
  if not %ERRORLEVEL% == 0 (
    echo %_fBRed%Error: IDEasy failed with exit code %ERRORLEVEL% %_RESET%
    exit /b %ERRORLEVEL%
  )
)

REM https://stackoverflow.com/questions/61888625/what-is-f-in-the-for-loop-command
for /f "tokens=*" %%i in ('ideasy %IDE_OPTIONS% env') do (
  call set %%i
)
if not %ERRORLEVEL% == 0 (
  echo IDE environment variables have been set for %IDE_HOME% in workspace %WORKSPACE%
)

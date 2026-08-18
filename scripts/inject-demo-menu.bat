@echo off
setlocal EnableExtensions EnableDelayedExpansion
title Onyx Menu Injector

set "SCRIPT=%~dp0inject-demo-menu.ps1"

rem Advanced mode: preserve the existing named PowerShell arguments.
set "FIRST=%~1"
if defined FIRST if "%FIRST:~0,1%"=="-" goto advanced

echo ============================================================
echo                    ONYX MENU INJECTOR
echo ============================================================
echo Only use APKs you own or are authorized to test.
echo.

set "HOST_PATH=%~1"
if not defined HOST_PATH (
    echo Drag an APK file or a folder containing APKs into this window,
    set /p "HOST_PATH=then press Enter: "
    set "HOST_PATH=!HOST_PATH:"=!"
)

if not defined HOST_PATH (
    echo No APK or folder was provided.
    pause
    exit /b 2
)

set "TOOLKIT_PATH="
set "MENU_SOURCE="
set "VALIDATE_ARGS="

if /I "%~2"=="--validate-only" (
    set "VALIDATE_ARGS=-SkipBuild -ValidateOnly"
) else if not "%~2"=="" (
    set "MENU_SOURCE=%~2"
)

if /I "%~3"=="--validate-only" (
    set "VALIDATE_ARGS=-SkipBuild -ValidateOnly"
) else if not "%~3"=="" (
    set "TOOLKIT_PATH=%~3"
)
if /I "%~4"=="--validate-only" set "VALIDATE_ARGS=-SkipBuild -ValidateOnly"

if not defined MENU_SOURCE (
    echo.
    echo Drag a menu donor APK/folder here, or press Enter for built-in demo.
    set /p "MENU_SOURCE=Menu source: "
    set "MENU_SOURCE=!MENU_SOURCE:"=!"
)

echo.
echo Selected APK/folder:
echo   %HOST_PATH%
if defined MENU_SOURCE (
    echo Menu donor APK/folder:
    echo   !MENU_SOURCE!
) else (
    echo Menu donor: built-in Onyx demo
)
if defined TOOLKIT_PATH (
    echo Toolkit folder:
    echo   %TOOLKIT_PATH%
)
if defined TOOLKIT_PATH (
    if defined MENU_SOURCE goto run_both
    goto run_tools
)
if defined MENU_SOURCE goto run_menu
goto run_default

:run_both
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" ^
    -HostPath "%HOST_PATH%" ^
    -MenuSource "!MENU_SOURCE!" ^
    -ToolkitRoot "%TOOLKIT_PATH%" ^
    -IHavePermission %VALIDATE_ARGS%
goto after_inject

:run_tools
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" ^
    -HostPath "%HOST_PATH%" ^
    -ToolkitRoot "%TOOLKIT_PATH%" ^
    -IHavePermission %VALIDATE_ARGS%
goto after_inject

:run_menu
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" ^
    -HostPath "%HOST_PATH%" ^
    -MenuSource "!MENU_SOURCE!" ^
    -IHavePermission %VALIDATE_ARGS%
goto after_inject

:run_default
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" ^
    -HostPath "%HOST_PATH%" ^
    -IHavePermission %VALIDATE_ARGS%

:after_inject

set "RESULT=%ERRORLEVEL%"
echo.
if "%RESULT%"=="0" (
    echo Completed successfully.
) else (
    echo Failed with exit code %RESULT%.
)
pause
exit /b %RESULT%

:advanced
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" %*
exit /b %ERRORLEVEL%

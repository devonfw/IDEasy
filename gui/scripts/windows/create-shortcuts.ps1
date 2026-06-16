#Requires -Version 5.0

# IDEasy GUI Launcher - Create Windows Start Menu Shortcut and Desktop Link
# This script creates shortcuts to launch the IDEasy GUI from Windows Start Menu and Desktop

param (
    [switch]$SkipDesktop,
    [switch]$SkipStartMenu
)

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$guiDir = Split-Path -Parent (Split-Path -Parent $scriptDir)
$projectRoot = Split-Path -Parent $guiDir

$launcherBat = Join-Path $scriptDir "launch-gui.bat"
$pomFile = Join-Path $projectRoot "pom.xml"

if (-not (Test-Path $launcherBat)) {
    Write-Host "Error: launch-gui.bat not found" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $pomFile)) {
    Write-Host "Error: pom.xml not found" -ForegroundColor Red
    exit 1
}

Write-Host "IDEasy GUI Launcher Setup" -ForegroundColor Cyan
Write-Host "Project: $projectRoot" -ForegroundColor Cyan
Write-Host ""

function Create-Shortcut {
    param(
        [string]$Path,
        [string]$Target,
        [string]$Description
    )
    
    $wshShell = New-Object -ComObject WScript.Shell

    try {
        Write-Host "Creating shortcut: $Path"

        $shortcut = $wshShell.CreateShortcut($Path)
        $shortcut.TargetPath = $Target
        $shortcut.WorkingDirectory = $projectRoot
        $shortcut.Description = $Description

        $iconPath = Join-Path $scriptDir "icon.ico"
        if (Test-Path $iconPath) {
            $shortcut.IconLocation = "$iconPath,0"
        } else {
            $shortcut.IconLocation = "$env:SystemRoot\system32\shell32.dll,1"
        }

        $shortcut.Save()
        Write-Host "Created: $Path" -ForegroundColor Green
    }
    catch {
        Write-Host "Error creating shortcut: $($_.Exception.Message)" -ForegroundColor Red
    }
    finally {
        [System.Runtime.Interopservices.Marshal]::ReleaseComObject($wshShell) | Out-Null
    }
}

if (-not $SkipDesktop) {
    $desktopShortcut = Join-Path "$env:USERPROFILE\Desktop" "IDEasy GUI.lnk"
    Create-Shortcut -Path $desktopShortcut -Target $launcherBat -Description "Launch IDEasy GUI"
}

if (-not $SkipStartMenu) {
    $startMenuShortcut = Join-Path "$env:APPDATA\Microsoft\Windows\Start Menu\Programs" "IDEasy GUI.lnk"
    Create-Shortcut -Path $startMenuShortcut -Target $launcherBat -Description "Launch IDEasy GUI"
}

Write-Host ""
Write-Host "Setup complete!" -ForegroundColor Green

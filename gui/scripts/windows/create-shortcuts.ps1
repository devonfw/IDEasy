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
$launcherVbs = Join-Path $scriptDir "launch-gui-silent.vbs"
$pomFile = Join-Path $projectRoot "pom.xml"

if (-not (Test-Path $launcherBat)) {
    Write-Host "Error: launch-gui.bat not found" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $launcherVbs)) {
    Write-Host "Error: launch-gui-silent.vbs not found" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $pomFile)) {
    Write-Host "Error: pom.xml not found" -ForegroundColor Red
    exit 1
}

Write-Host "IDEasy GUI Launcher Setup" -ForegroundColor Cyan
Write-Host "Project: $projectRoot" -ForegroundColor Cyan
Write-Host ""

# Wrap the PNG bytes directly into an ICO container (Windows Vista+ supports PNG frames in ICO).
# This preserves full color depth and alpha transparency without any image processing.
# The ICO is written next to the scripts so the shortcut has a stable reference path.
# Returns $true on success, $false on failure.
function Convert-PngToIco {
    param([string]$PngPath, [string]$IcoPath)
    try {
        $pngBytes = [System.IO.File]::ReadAllBytes($PngPath)

        # PNG dimensions are stored big-endian in the IHDR chunk at bytes 16-23
        $width  = ($pngBytes[16] -shl 24) -bor ($pngBytes[17] -shl 16) -bor ($pngBytes[18] -shl 8) -bor $pngBytes[19]
        $height = ($pngBytes[20] -shl 24) -bor ($pngBytes[21] -shl 16) -bor ($pngBytes[22] -shl 8) -bor $pngBytes[23]

        # ICO directory encodes 256 as 0
        $icoW = if ($width  -ge 256) { [byte]0 } else { [byte]$width }
        $icoH = if ($height -ge 256) { [byte]0 } else { [byte]$height }

        $sizeBytes   = [System.BitConverter]::GetBytes([int32]$pngBytes.Length)
        $offsetBytes = [System.BitConverter]::GetBytes([int32](6 + 16))  # header(6) + one dir entry(16)

        # ICO header (6 bytes) + single directory entry (16 bytes)
        $icoHeader = [byte[]](
            0x00, 0x00,                                                          # reserved
            0x01, 0x00,                                                          # type: icon
            0x01, 0x00,                                                          # image count: 1
            $icoW, $icoH, 0x00, 0x00,                                           # w, h, colorCount, reserved
            0x01, 0x00,                                                          # color planes
            0x20, 0x00,                                                          # bits per pixel (32)
            $sizeBytes[0],   $sizeBytes[1],   $sizeBytes[2],   $sizeBytes[3],   # image data size
            $offsetBytes[0], $offsetBytes[1], $offsetBytes[2], $offsetBytes[3]  # image data offset
        )

        $stream = [System.IO.FileStream]::new($IcoPath, [System.IO.FileMode]::Create)
        $stream.Write($icoHeader, 0, $icoHeader.Length)
        $stream.Write($pngBytes, 0, $pngBytes.Length)
        $stream.Close()
        return $true
    }
    catch {
        return $false
    }
}

# Resolve icon location: prefer devonfw.png converted to ICO, fall back to shell32
$pngSource = Join-Path $guiDir "src\main\resources\com\devonfw\ide\gui\assets\devonfw.png"
$generatedIco = Join-Path $scriptDir "ideasy-gui.ico"
$iconLocation = "$env:SystemRoot\system32\shell32.dll,1"

if (Test-Path $pngSource) {
    if (-not (Test-Path $generatedIco)) {
        if (Convert-PngToIco -PngPath $pngSource -IcoPath $generatedIco) {
            Write-Host "Icon generated from devonfw.png" -ForegroundColor Cyan
        } else {
            Write-Host "Note: Icon conversion failed, using default icon" -ForegroundColor Yellow
        }
    }
    if (Test-Path $generatedIco) {
        $iconLocation = "$generatedIco,0"
    }
} elseif (Test-Path (Join-Path $scriptDir "icon.ico")) {
    $iconLocation = "$(Join-Path $scriptDir 'icon.ico'),0"
}

function Create-Shortcut {
    param(
        [string]$Path,
        [string]$Target,
        [string]$Arguments,
        [string]$Description
    )

    $wshShell = $null
    try {
        Write-Host "Creating shortcut: $Path"

        $wshShell = New-Object -ComObject WScript.Shell
        $shortcut = $wshShell.CreateShortcut($Path)
        $shortcut.TargetPath = $Target
        $shortcut.Arguments = $Arguments
        $shortcut.WorkingDirectory = $projectRoot
        $shortcut.Description = $Description
        $shortcut.IconLocation = $iconLocation

        $shortcut.Save()
        Write-Host "Created: $Path" -ForegroundColor Green
    }
    catch {
        Write-Host "Error creating shortcut: $($_.Exception.Message)" -ForegroundColor Red
    }
    finally {
        if ($wshShell) {
            [System.Runtime.Interopservices.Marshal]::ReleaseComObject($wshShell) | Out-Null
        }
    }
}

# Target wscript.exe running the silent VBS wrapper instead of launch-gui.bat directly,
# so no console window ever appears - independent of the user's terminal "close on exit" setting.
$wscriptPath = Join-Path "$env:SystemRoot\System32" "wscript.exe"
$launcherArgs = "`"$launcherVbs`""

if (-not $SkipDesktop) {
    # Use Shell32 to resolve the Desktop folder — works correctly with OneDrive-redirected Desktops
    $desktopShortcut = Join-Path ([Environment]::GetFolderPath('Desktop')) "IDEasy GUI.lnk"
    Create-Shortcut -Path $desktopShortcut -Target $wscriptPath -Arguments $launcherArgs -Description "Launch IDEasy GUI"
}

if (-not $SkipStartMenu) {
    $startMenuShortcut = Join-Path "$env:APPDATA\Microsoft\Windows\Start Menu\Programs" "IDEasy GUI.lnk"
    Create-Shortcut -Path $startMenuShortcut -Target $wscriptPath -Arguments $launcherArgs -Description "Launch IDEasy GUI"
}

Write-Host ""
Write-Host "Setup complete!" -ForegroundColor Green

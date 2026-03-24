$ErrorActionPreference = "Stop"

$IDEASY_HOME = if ($env:IDEASY_HOME) { $env:IDEASY_HOME } else { Join-Path $env:USERPROFILE ".ideasy" }
$GITHUB_REPO = "devonfw/IDEasy"

function Write-Status($msg) { Write-Host $msg -ForegroundColor Cyan }
function Write-Success($msg) { Write-Host $msg -ForegroundColor Green }
function Write-Err($msg) { Write-Host "Error: $msg" -ForegroundColor Red; exit 1 }

function Test-Prerequisite($cmd, $hint) {
  if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
    Write-Err "$cmd is required but not installed. $hint"
  }
}

function Get-LatestVersion {
  $url = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
  $release = Invoke-RestMethod -Uri $url -UseBasicParsing
  if (-not $release.tag_name) {
    Write-Err "Failed to determine latest release version."
  }
  return $release.tag_name -replace "^release/", ""
}

Write-Status "Installing IDEasy..."

Test-Prerequisite "git" "See https://git-scm.com/download/win"

$arch = "x64"
$version = Get-LatestVersion
$archive = "ide-cli-$version-windows-$arch.tar.gz"
$downloadUrl = "https://github.com/$GITHUB_REPO/releases/download/release/$version/$archive"

Write-Status "Latest version: $version"

$tmpDir = Join-Path ([System.IO.Path]::GetTempPath()) ("ideasy-install-" + [guid]::NewGuid().ToString("N").Substring(0, 8))
New-Item -ItemType Directory -Path $tmpDir -Force | Out-Null
$archivePath = Join-Path $tmpDir $archive

try {
  Write-Status "Downloading $archive..."
  Invoke-WebRequest -Uri $downloadUrl -OutFile $archivePath -UseBasicParsing

  if (-not (Test-Path $IDEASY_HOME)) {
    New-Item -ItemType Directory -Path $IDEASY_HOME -Force | Out-Null
  }

  Write-Status "Extracting to $IDEASY_HOME..."
  tar xzf $archivePath -C $IDEASY_HOME

  Write-Status "Running setup..."
  $setupBat = Join-Path $IDEASY_HOME "setup.bat"
  if (Test-Path $setupBat) {
    & cmd /c "`"$setupBat`" -b"
  } else {
    Write-Err "setup.bat not found in $IDEASY_HOME"
  }

  $binDir = Join-Path $IDEASY_HOME "bin"
  $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
  if ($userPath -notlike "*$binDir*") {
    [Environment]::SetEnvironmentVariable("Path", "$binDir;$userPath", "User")
    $env:Path = "$binDir;$env:Path"
  }

  Write-Host ""
  Write-Success "IDEasy $version installed successfully!"
  Write-Host ""
  Write-Host "  Run 'ide' to get started or 'ide create <project>' to set up a project."
  Write-Host ""
  Write-Host "  You may need to restart your terminal for PATH changes to take effect."
  Write-Host ""
} finally {
  Remove-Item -Recurse -Force $tmpDir -ErrorAction SilentlyContinue
}

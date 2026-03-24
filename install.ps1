$ErrorActionPreference = "Stop"

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

  Write-Status "Extracting..."
  tar xzf $archivePath -C $tmpDir

  Write-Status "Running setup..."
  $setupBat = Join-Path $tmpDir "setup.bat"
  if (Test-Path $setupBat) {
    & cmd /c "`"$setupBat`" -b"
  } else {
    Write-Err "setup.bat not found in extracted archive."
  }

  Write-Host ""
  Write-Success "IDEasy $version installed successfully!"
  Write-Host ""
  Write-Host "  Restart your terminal, then run:"
  Write-Host "    ide create <project>    # set up a new project"
  Write-Host "    ide --help              # see all commands"
  Write-Host ""
} finally {
  Remove-Item -Recurse -Force $tmpDir -ErrorAction SilentlyContinue
}

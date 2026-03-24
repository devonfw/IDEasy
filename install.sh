#!/bin/bash
set -eu

GITHUB_REPO="devonfw/IDEasy"

red() { printf '\033[1;31m%s\033[0m\n' "$1"; }
green() { printf '\033[1;32m%s\033[0m\n' "$1"; }
blue() { printf '\033[1;34m%s\033[0m\n' "$1"; }

abort() { red "Error: $1" >&2; exit 1; }

detect_os() {
  case "$(uname -s)" in
    Linux*)  echo "linux" ;;
    Darwin*) echo "mac" ;;
    *)       abort "Unsupported operating system: $(uname -s). Use the PowerShell installer on Windows." ;;
  esac
}

detect_arch() {
  case "$(uname -m)" in
    x86_64|amd64)   echo "x64" ;;
    arm64|aarch64)  echo "arm64" ;;
    *)              abort "Unsupported architecture: $(uname -m)" ;;
  esac
}

fetch_latest_version() {
  local url="https://api.github.com/repos/${GITHUB_REPO}/releases/latest"
  local tag
  tag=$(curl -fsSL "${url}" | grep '"tag_name"' | cut -d '"' -f 4)
  if [ -z "${tag}" ]; then
    abort "Failed to determine latest release version."
  fi
  echo "${tag#release/}"
}

check_prerequisites() {
  if ! command -v git >/dev/null 2>&1; then
    abort "git is required but not installed. See https://git-scm.com/downloads"
  fi
  if ! command -v curl >/dev/null 2>&1; then
    abort "curl is required but not installed."
  fi
  if ! command -v tar >/dev/null 2>&1; then
    abort "tar is required but not installed."
  fi
}

main() {
  blue "Installing IDEasy..."

  check_prerequisites

  local os arch version download_url archive
  os=$(detect_os)
  arch=$(detect_arch)
  version=$(fetch_latest_version)
  archive="ide-cli-${version}-${os}-${arch}.tar.gz"
  download_url="https://github.com/${GITHUB_REPO}/releases/download/release/${version}/${archive}"

  blue "Detected: ${os} ${arch}"
  blue "Latest version: ${version}"

  local tmpdir
  tmpdir=$(mktemp -d)
  trap 'rm -rf "${tmpdir}"' EXIT

  blue "Downloading ${archive}..."
  curl -fSL --progress-bar -o "${tmpdir}/${archive}" "${download_url}"

  blue "Extracting..."
  tar xzf "${tmpdir}/${archive}" -C "${tmpdir}"

  if [ "${os}" = "mac" ]; then
    xattr -r -d com.apple.quarantine "${tmpdir}" 2>/dev/null || true
  fi

  blue "Running setup..."
  cd "${tmpdir}"
  bash setup

  echo ""
  green "IDEasy ${version} installed successfully!"
  echo ""
  echo "  Restart your terminal, then run:"
  echo "    ide create <project>    # set up a new project"
  echo "    ide --help              # see all commands"
  echo ""
}

main

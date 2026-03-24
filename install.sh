#!/bin/bash
set -eu

IDEASY_HOME="${IDEASY_HOME:-${HOME}/.ideasy}"
GITHUB_REPO="devonfw/IDEasy"

red() { printf '\033[1;31m%s\033[0m\n' "$1"; }
green() { printf '\033[1;32m%s\033[0m\n' "$1"; }
blue() { printf '\033[1;34m%s\033[0m\n' "$1"; }

abort() { red "Error: $1" >&2; exit 1; }

detect_os() {
  case "$(uname -s)" in
    Linux*)  echo "linux" ;;
    Darwin*) echo "mac" ;;
    *)       abort "Unsupported operating system: $(uname -s). Use the MSI installer on Windows." ;;
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

  mkdir -p "${IDEASY_HOME}"
  blue "Extracting to ${IDEASY_HOME}..."
  tar xzf "${tmpdir}/${archive}" -C "${IDEASY_HOME}"

  if [ "${os}" = "mac" ]; then
    xattr -r -d com.apple.quarantine "${IDEASY_HOME}" 2>/dev/null || true
  fi

  blue "Running setup..."
  cd "${IDEASY_HOME}"
  bash setup

  add_to_path

  echo ""
  green "IDEasy ${version} installed successfully!"
  echo ""
  echo "  Run 'ide' to get started or 'ide create <project>' to set up a project."
  echo ""
  echo "  You may need to restart your terminal or run:"
  echo "    source ~/.bashrc   # or source ~/.zshrc"
  echo ""
}

add_to_path() {
  local bin_dir="${IDEASY_HOME}/bin"
  if echo "${PATH}" | tr ':' '\n' | grep -qx "${bin_dir}"; then
    return
  fi
  for rc in "${HOME}/.bashrc" "${HOME}/.zshrc"; do
    if [ -f "${rc}" ] && ! grep -q "/.ideasy/bin" "${rc}" 2>/dev/null; then
      echo "export PATH=\"\${HOME}/.ideasy/bin:\${PATH}\"" >> "${rc}"
    fi
  done
}

main

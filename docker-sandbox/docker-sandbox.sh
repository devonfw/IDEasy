#!/usr/bin/env bash
# =============================================================================
# IDEasy Docker Sandbox - host launcher
# =============================================================================
# Starts an isolated Docker container containing a fully provisioned IDEasy
# workspace (IDEasy + Claude Code installed by the Dockerfile) and runs an AI
# coding agent (default: Claude Code) inside it.
#
# Only the git repository you pass is bind-mounted (read-write) - and it is
# mounted *into* the container's IDEasy workspace as a subfolder, so the
# container workspace is a complete, IDEasy-managed workspace (not a bare git
# checkout). The agent's edits, `git commit` and pull requests therefore operate
# on your real repository, while the IDEasy runtime is container-local (discarded
# on exit) - the host IDEasy installation is never touched.
#
# [EXPERIMENTAL] Targets technically skilled users. No warranty or support.
#
# Modelled on the pi.dev sandbox (a devonfw/Capgemini sibling of IDEasy):
#   * the working directory (your git repo) is mounted into the container
#   * your host Git identity is passed in (commits use your author/committer)
#   * host SSH keys / SSH agents are NOT mounted into the container
#   * corporate CA trust is honoured via NODE_EXTRA_CA_CERTS when provided
#
# Layout:
#   container IDEasy root : /projects               (baked into the image)
#   container workspace   : /projects/sandbox/workspaces/main   (provisioned by IDEasy)
#   mounted repository    : /projects/sandbox/workspaces/main/<repo>
#
# Usage:
#   bash docker-sandbox/docker-sandbox.sh [options] [-- agent-args ...]
#
# Options:
#   --repo <path>         Git repository to mount (default: auto-detected via
#                         `git rev-parse --show-toplevel` from the current
#                         directory - i.e. run this from inside the repo you want
#                         to work on).
#   --image <name>        Image name (default: ideasy-sandbox).
#   --agent <name>        Agent commandlet to run (default: claude).
#   --pull                Rebuild the image before running.
#   --check               Validate the host (engine, repo, git identity) and exit
#                         - do not build or run. Needs no engine running.
#   --dry-run             Validate, build if necessary, print the exact `docker
#                         run` command, then exit - do not start the container.
#   --ca-cert <file>      Bundle of CA certificates to trust inside the container
#                         (exported as NODE_EXTRA_CA_CERTS=/ca/certs/ca-bundle.crt).
#   -h | --help           Show this help.
#
# Everything after `--` is forwarded to the agent (e.g. `-- -p "fix the test"`).
# =============================================================================

set -euo pipefail

# --- Defaults ----------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
IMAGE="ideasy-sandbox"
AGENT="claude"
REPO=""
CA_CERT_FILE=""
DO_PULL=0
DO_CHECK=0
DO_DRY_RUN=0
AGENT_ARGS=()

# The container IDEasy root is baked into the image (see the Dockerfile). The
# provisioned workspace is where the repository is mounted as a subfolder.
IDE_ROOT="/projects"
PROJECT="sandbox"
WORKSPACE="main"
CONTAINER_WORKSPACE="${IDE_ROOT}/${PROJECT}/workspaces/${WORKSPACE}"

# --- Colored output helpers (mirror the repository install.sh style) ----------
if [ -t 1 ]; then
  RED=$'\033[1;31m'; GREEN=$'\033[1;32m'; BLUE=$'\033[1;34m'; OFF=$'\033[0m'
else
  RED=""; GREEN=""; BLUE=""; OFF=""
fi
red()   { printf '%sError: %s%s\n' "$RED" "$1" "$OFF" >&2; }
green() { printf '%s%s%s\n' "$GREEN" "$1" "$OFF"; }
blue()  { printf '%s%s%s\n' "$BLUE" "$1" "$OFF"; }

usage() {
  sed -n '2,48p' "$0" | sed 's/^# \{0,1\}//'
}

# --- Argument parsing --------------------------------------------------------
while [ $# -gt 0 ]; do
  case "$1" in
    --repo) REPO="${2:?--repo requires a value}"; shift 2 ;;
    --image) IMAGE="${2:?--image requires a value}"; shift 2 ;;
    --agent) AGENT="${2:?--agent requires a value}"; shift 2 ;;
    --ca-cert) CA_CERT_FILE="${2:?--ca-cert requires a value}"; shift 2 ;;
    --pull) DO_PULL=1; shift ;;
    --check) DO_CHECK=1; shift ;;
    --dry-run) DO_DRY_RUN=1; shift ;;
    -h|--help) usage; exit 0 ;;
    --) shift; AGENT_ARGS=("$@"); break ;;
    *) red "Unknown option: $1"; usage; exit 2 ;;
  esac
done

# --- Helpers -----------------------------------------------------------------
detect_container_engine() {
  if command -v docker >/dev/null 2>&1; then
    echo "docker"
  elif command -v podman >/dev/null 2>&1; then
    echo "podman"
  else
    echo ""
  fi
}

# Detect the git repository to mount: an explicit --repo, else the toplevel of
# the current working tree.
resolve_repo() {
  if [ -n "$REPO" ]; then
    if [ ! -d "$REPO" ]; then
      red "--repo path does not exist: $REPO"
      return 1
    fi
    REPO="$(cd "$REPO" && pwd)"
  else
    local toplevel
    if ! toplevel="$(git rev-parse --show-toplevel 2>/dev/null)"; then
      red "Cannot detect a git repository to mount. Pass one explicitly with --repo <path>."
      red "Tip: run this script from inside the repository you want to work on."
      return 1
    fi
    REPO="$toplevel"
  fi
  REPO_BASENAME="$(basename "$REPO")"
}

# Resolve the host git identity so commits inside the sandbox are authored by you.
resolve_git_identity() {
  GIT_AUTHOR_NAME="$(git config user.name 2>/dev/null || true)"
  GIT_AUTHOR_EMAIL="$(git config user.email 2>/dev/null || true)"
  GIT_COMMITTER_NAME="${GIT_COMMITTER_NAME:-$GIT_AUTHOR_NAME}"
  GIT_COMMITTER_EMAIL="${GIT_COMMITTER_EMAIL:-$GIT_AUTHOR_EMAIL}"
}

# Validate the host: container engine (and daemon when actually running), and the
# presence of the Dockerfile.
validate_host() {
  local failures=0
  local engine="$1"
  if [ -z "$engine" ]; then
    red "No container engine found. Install Docker or Podman (IDEasy recommends"
    red "Rancher Desktop: https://rancherdesktop.io/)."
    failures=$((failures + 1))
  elif [ "$DO_CHECK" -eq 0 ] && [ "$DO_DRY_RUN" -eq 0 ]; then
    # Only require the daemon when we intend to actually start a container.
    if ! "$engine" info >/dev/null 2>&1; then
      red "Container engine '$engine' is not running (daemon not reachable)."
      red "Start your container engine (e.g. Rancher Desktop) and retry."
      failures=$((failures + 1))
    fi
  fi

  if [ ! -f "$SCRIPT_DIR/Dockerfile" ]; then
    red "Dockerfile not found at $SCRIPT_DIR/Dockerfile."
    failures=$((failures + 1))
  fi

  if [ "$failures" -gt 0 ]; then
    return 1
  fi
}

# Build the list of `docker run` environment -e flags (git identity + optional CA).
build_env_flags() {
  ENV_FLAGS=()
  if [ -n "${GIT_AUTHOR_NAME:-}" ]; then
    ENV_FLAGS+=("-e" "GIT_AUTHOR_NAME=$GIT_AUTHOR_NAME")
  fi
  if [ -n "${GIT_AUTHOR_EMAIL:-}" ]; then
    ENV_FLAGS+=("-e" "GIT_AUTHOR_EMAIL=$GIT_AUTHOR_EMAIL")
  fi
  if [ -n "${GIT_COMMITTER_NAME:-}" ]; then
    ENV_FLAGS+=("-e" "GIT_COMMITTER_NAME=$GIT_COMMITTER_NAME")
  fi
  if [ -n "${GIT_COMMITTER_EMAIL:-}" ]; then
    ENV_FLAGS+=("-e" "GIT_COMMITTER_EMAIL=$GIT_COMMITTER_EMAIL")
  fi
  if [ -n "$CA_CERT_FILE" ]; then
    ENV_FLAGS+=("-e" "NODE_EXTRA_CA_CERTS=/ca/certs/ca-bundle.crt")
  fi
}

print_plan() {
  local engine="$1"
  echo ""
  blue "IDEasy Docker Sandbox - plan"
  echo "  Engine          : ${engine}"
  echo "  Image           : ${IMAGE}"
  echo "  Agent           : ${AGENT}"
  echo "  Repository      : ${REPO}  ->  ${CONTAINER_WORKSPACE}/${REPO_BASENAME}"
  echo "  Container root  : ${IDE_ROOT} (container-local, discarded on exit)"
  echo "  Git identity    : ${GIT_AUTHOR_NAME:-<unset>} <${GIT_AUTHOR_EMAIL:-<unset>}>"
  echo "  CA cert         : ${CA_CERT_FILE:-<none>}"
  echo "  Agent args      : ${AGENT_ARGS[*]:-<none>}"
  echo ""
  echo "  Command:"
  echo "    docker run -it --rm -w ${CONTAINER_WORKSPACE}/${REPO_BASENAME} -e <git identity> \\"
  echo "      -v \"${REPO}:${CONTAINER_WORKSPACE}/${REPO_BASENAME}\" ${IMAGE} ${AGENT} ${AGENT_ARGS[*]:-}"
  echo ""
  blue "Security notes (see documentation/docker-sandbox.adoc):"
  echo "  * Only your Git identity is passed in. Host SSH keys / SSH agents are NOT mounted."
  echo "  * Pushing / opening PRs from inside requires an explicit credential (see the doc)."
  echo ""
}

# --- Main --------------------------------------------------------------------
main() {
  local engine
  engine="$(detect_container_engine)"

  blue "IDEasy Docker Sandbox (experimental)"
  resolve_repo
  resolve_git_identity

  if ! validate_host "$engine"; then
    if [ "$DO_CHECK" -eq 1 ] || [ "$DO_DRY_RUN" -eq 1 ]; then
      # In check/dry-run a stopped daemon is not fatal (we will not start a container).
      :
    else
      exit 1
    fi
  fi

  if [ "$DO_CHECK" -eq 1 ]; then
    print_plan "$engine"
    green "Check passed. (No image was built and no container was started.)"
    exit 0
  fi

  # Ensure the image exists (build if requested or absent) - unless we only print a plan.
  if [ "$DO_DRY_RUN" -eq 0 ]; then
    local image_present=0
    "$engine" image inspect "$IMAGE" >/dev/null 2>&1 && image_present=1
    if [ "$DO_PULL" -eq 1 ] || [ "$image_present" -eq 0 ]; then
      blue "Building image '$IMAGE' (first run / --pull)..."
      (cd "$SCRIPT_DIR" && "$engine" build -t "$IMAGE" .)
    else
      blue "Using existing image '$IMAGE' (use --pull to rebuild)."
    fi
  fi

  # Compose the run command. The repository is mounted as a subfolder of the
  # provisioned IDEasy workspace and the agent starts inside it (-w).
  local -a run_cmd=("$engine" run -it --rm)
  build_env_flags
  run_cmd+=("${ENV_FLAGS[@]}")
  run_cmd+=("-v" "${REPO}:${CONTAINER_WORKSPACE}/${REPO_BASENAME}")
  if [ -n "$CA_CERT_FILE" ]; then
    run_cmd+=("-v" "${CA_CERT_FILE}:/ca/certs/ca-bundle.crt:ro")
  fi
  run_cmd+=("-w" "${CONTAINER_WORKSPACE}/${REPO_BASENAME}")
  # No host SSH keys or SSH_AUTH_SOCK are mounted (deliberate; see the doc).
  # The container command is the agent commandlet plus its arguments; the
  # entrypoint runs `ide <command...>` with CWD on the mounted repository.
  run_cmd+=("$IMAGE" "$AGENT")
  if [ ${#AGENT_ARGS[@]} -gt 0 ]; then
    run_cmd+=("${AGENT_ARGS[@]}")
  fi

  if [ "$DO_DRY_RUN" -eq 1 ]; then
    print_plan "$engine"
    echo "  Exact command:"
    local -a q=()
    for t in "${run_cmd[@]}"; do q+=("$(printf '%q' "$t")"); done
    echo "    ${q[*]}"
    echo ""
    green "Dry run complete. No container was started."
    exit 0
  fi

  # Prevent Git Bash/MSYS from rewriting Linux container paths.
  if [[ -n "${MSYSTEM:-}" ]]; then
    export MSYS_NO_PATHCONV=1
    export MSYS2_ARG_CONV_EXCL="*"
  fi

  exec "${run_cmd[@]}"
}

main

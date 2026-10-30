#!/usr/bin/env bash
# =============================================================================
# IDEasy Docker Sandbox - container entrypoint
# =============================================================================
# Runs the agent commandlet passed in (the agent + its args, e.g.
# `claude -p "fix the test"`) against the container-local `sandbox` project,
# with the working directory set to the mounted repository (set by the launcher).
#
# This script is deliberately AGENT-NEUTRAL. It knows only:
#   * how to activate the IDEasy environment (export IDE_ROOT + exec the CLI), and
#   * the generic container concerns (keep-alive, forwarding the command).
# All agent-specific behaviour (which tools the image installs, and how this
# agent receives its credentials) lives in the agent profile at
#   $SBOX_AGENTS_DIR/<agent>.sh
# which this script sources and which defines sbox_agent_apply_credentials.
# Adding a supported agent therefore does NOT touch this file - drop a new
# agents/<agent>.sh into the image (see documentation/docker-sandbox.adoc,
# "Adding an agent").
#
# Running the agent through the `ideasy` commandlet (rather than the bare binary)
# is deliberate: `ideasy <tool>` installs/verifies the tool, relocates its config
# into the container and scrubs ambient provider/auth env vars, isolating the
# agent from any secrets that may have leaked into the container.
#
# We activate IDEasy simply by exporting IDE_ROOT and invoking the CLI, rather
# than `source`ing the installation `functions` script: that script is written
# for interactive, non-strict shells and (a) references unset variables such as
# $IDE_OPTIONS (fatal under `set -u`) and (b) triggers a full environment refresh
# on load that is slow and emits noise. For a one-shot agent launch neither is
# needed - `exec ideasy <tool>` applies the project environment to the agent.
# =============================================================================

# No `functions` script is sourced here, so a strict shell is safe.
set -euo pipefail

export IDE_ROOT="/projects"
# Where the Dockerfile copies docker-sandbox/agents/ (shared helpers + profiles).
SBOX_AGENTS_DIR="${SBOX_AGENTS_DIR:-/opt/ideasy-sandbox/agents}"
# The agent used when none is requested (e.g. a bare `keep-alive`).
SBOX_DEFAULT_AGENT="${SBOX_DEFAULT_AGENT:-claude}"

# `keep-alive` starts the container and keeps it running so you can `docker exec`
# into it later (e.g. `docker exec -it sbx bash`). It is agent-aware so you can
# keep the container alive with a specific agent's credentials applied:
#   `keep-alive`          -> applies the default agent's credentials (claude)
#   `keep-alive <agent>`  -> applies that agent's credentials
# Any other first argument is the agent commandlet to run (plus its args).
if [ "${1:-}" = "keep-alive" ]; then
  agent="${2:-$SBOX_DEFAULT_AGENT}"
else
  agent="${1:?no agent given (usage: <agent> [args...] | keep-alive [agent])}"
fi

# Load the shared helpers and this agent's profile (defines
# sbox_agent_apply_credentials and SBOX_AGENT_TOOLS). The launcher has already
# validated the agent name; this is a defensive check in case the profile is
# missing from the image.
[ -f "$SBOX_AGENTS_DIR/common.sh" ] || { echo "[ideasy-sandbox] ERROR: $SBOX_AGENTS_DIR/common.sh not found" >&2; exit 127; }
# shellcheck source=/dev/null
source "$SBOX_AGENTS_DIR/common.sh"
profile="$SBOX_AGENTS_DIR/$agent.sh"
[ -f "$profile" ] || { echo "[ideasy-sandbox] ERROR: no agent profile for '$agent' ($profile missing)" >&2; exit 127; }
# shellcheck source=/dev/null
source "$profile"

# Apply this agent's credentials / endpoint config from the neutral
# IDEASY_SANDBOX_* variables (best-effort: failures only warn, the agent starts).
# This is called at top level under `set -e`, so a profile that returns non-zero
# would abort the launch; guard the call so a credential-application failure never
# prevents the agent from starting.
if ! sbox_agent_apply_credentials; then
  echo "[ideasy-sandbox] WARNING: failed to apply credentials for '$agent'; the agent will start without them" >&2
fi

if [ "${1:-}" = "keep-alive" ]; then
  exec sleep infinity
fi

# The command is the agent commandlet plus its arguments, e.g. `claude -p "..."`.
# `exec` replaces this shell so the agent runs in the foreground and receives
# terminal signals (Ctrl-C) directly.
exec /projects/_ide/installation/bin/ideasy "$@"

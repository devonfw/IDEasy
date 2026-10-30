#!/usr/bin/env bash
# =============================================================================
# IDEasy Docker Sandbox - shared helpers for agent profiles
# =============================================================================
# Every agent profile (agents/<agent>.sh) is sourced by the container
# entrypoint (entrypoint.sh) and by the image build (Dockerfile). The launcher
# (docker-sandbox.sh) stays generic: it does NOT name any agent except the
# default and does not interpret credentials - it only forwards the agent's
# name and the credential values under the neutral IDEASY_SANDBOX_* names and
# leaves the interpretation to the profile.
#
# A profile declares:
#   * SBOX_AGENT_TOOLS          - space-separated IDEasy tools the image must
#                                 install for this agent (e.g. "node claude" or
#                                 "copilot"). The Dockerfile installs the union
#                                 of all selected agents' tools.
#   * sbox_agent_apply_credentials
#                                 - how this agent receives its credentials at
#                                 container start. Reads the neutral transport
#                                 variables and applies them in an
#                                 agent-specific way. Best-effort: on failure it
#                                 only logs a warning so the agent still starts.
#
# Neutral credential transport (set by the launcher, never baked into the image):
#   * IDEASY_SANDBOX_API_KEY    - the agent's primary credential (an API key or
#                                 a token). The profile decides what it means
#                                 (e.g. ANTHROPIC_API_KEY for Claude, GH_TOKEN
#                                 for Copilot).
#   * IDEASY_SANDBOX_E_<NAME>   - any number of extra agent variables (the
#                                 launcher prefixes --env NAME=VALUE with
#                                 IDEASY_SANDBOX_E_). The profile applies them.
#
# IDEasy does not scrub IDEASY_SANDBOX_* names, so they reach the entrypoint
# intact even when the agent commandlet would scrub the agent's own names.
# =============================================================================

# The agent used when none is requested (e.g. a bare `keep-alive`).
SBOX_DEFAULT_AGENT="${SBOX_DEFAULT_AGENT:-claude}"

# Directory holding the agent profiles. In the container this is where the
# Dockerfile copies docker-sandbox/agents/; the entrypoint may override it.
SBOX_AGENTS_DIR="${SBOX_AGENTS_DIR:-/opt/ideasy-sandbox/agents}"

# The container-local IDEasy project the sandbox provisions (see Dockerfile).
SBOX_PROJECT="${SBOX_PROJECT:-${IDE_ROOT:-/projects}/sandbox}"

# Locate the node binary provisioned into the container. IDEasy exposes it at
# <project>/software/node (a symlink to the installed version); the node tarball
# keeps the binary under bin/. Echoes the path, or nothing if not found. Some
# agents need node to run a small helper (e.g. merging credentials into a JSON
# config); agents that do not need node never call this.
sbox_find_node() {
  local candidate
  for candidate in \
    "${SBOX_PROJECT}/software/node/bin/node" \
    "${SBOX_PROJECT}/software/node/node"
  do
    if [ -x "$candidate" ]; then echo "$candidate"; return 0; fi
  done
  if command -v node >/dev/null 2>&1; then
    command -v node
    return 0
  fi
  return 1
}

# Merge credential values into the "env" block of a settings.json file.
#
# Usage: sbox_settings_merge <settings-file> <TARGET=SOURCE>...
#   For each TARGET=SOURCE pair, the value of the *environment variable* SOURCE
#   (if set) is written into the JSON object at cfg.env[TARGET]. Existing
#   entries are preserved; the file is created with an empty env block if absent.
#   TARGET and SOURCE may differ (e.g. ANTHROPIC_API_KEY=IDEASY_SANDBOX_API_KEY)
#   so the neutral transport name can be mapped to the agent-specific name.
#
# Everything is read from the environment (not argv) for the *values* so secrets
# never appear in `ps`; only the key names are passed on argv.
sbox_settings_merge() {
  local settings="$1"; shift
  local node_bin
  if ! node_bin="$(sbox_find_node)"; then
    echo "[ideasy-sandbox] WARNING: node not found - cannot write ${settings}; the agent will be unauthenticated" >&2
    return 0
  fi
  mkdir -p "$(dirname "$settings")"
  if "$node_bin" -e '
      const fs = require("fs");
      const file = process.argv[1];
      const pairs = process.argv.slice(2);
      let cfg = {};
      try { cfg = JSON.parse(fs.readFileSync(file, "utf8")); } catch (e) { cfg = {}; }
      if (!cfg.env || typeof cfg.env !== "object") cfg.env = {};
      const applied = [];
      for (const pair of pairs) {
        const eq = pair.indexOf("=");
        if (eq < 0) continue;
        const target = pair.slice(0, eq);
        const source = pair.slice(eq + 1);
        const value = process.env[source];
        if (value !== undefined) { cfg.env[target] = value; applied.push(target); }
      }
      fs.writeFileSync(file, JSON.stringify(cfg, null, 2) + "\n");
      if (applied.length) {
        process.stderr.write("[ideasy-sandbox] Applied to " + file + ": " + applied.join(", ") + "\n");
      }
    ' "$settings" "$@"; then
    :
  else
    echo "[ideasy-sandbox] WARNING: failed to apply credentials to ${settings}" >&2
  fi
}

# Print every IDEASY_SANDBOX_E_* variable currently in the environment, one name
# (without the prefix) per line. Shared by the profiles that pass extra
# variables through unchanged.
sbox_sandbox_env_names() {
  local name
  for name in "${!IDEASY_SANDBOX_E_@}"; do
    printf '%s\n' "${name#IDEASY_SANDBOX_E_}"
  done
}

#!/usr/bin/env bash
# =============================================================================
# IDEasy Docker Sandbox - agent profile: GitHub Copilot CLI
# =============================================================================
# GitHub Copilot CLI (https://github.com/github/copilot-cli) is a standalone Go
# binary, so the image installs only `copilot` (no node dependency).
#
# Unlike Claude, Copilot has no config-file credential mechanism: it authenticates
# from a token passed through the environment (GH_TOKEN, or GITHUB_TOKEN as an
# alternative). We therefore export the token into the environment for the agent
# process rather than writing a settings file. Use a fine-grained Personal
# Access Token (PAT) for headless / unattended use.
#
# Note on keep-alive: the token is exported into the entrypoint's process, which
# is then replaced by the agent (exec), so it is present for a normal launch. For
# `docker exec` into a kept-alive container, pass the token to the exec'd command
# explicitly (docker exec -e GH_TOKEN=... sbx copilot) - env vars do not cross a
# docker exec boundary.
# =============================================================================

# Tools the image must install for this agent.
SBOX_AGENT_TOOLS="copilot"

# Apply Copilot's credentials at container start (called by entrypoint.sh).
#   * IDEASY_SANDBOX_API_KEY      -> GH_TOKEN  (a GitHub PAT with Copilot access)
#   * IDEASY_SANDBOX_E_<NAME>     -> <NAME>    (e.g. GITHUB_TOKEN to prefer the
#     alternate variable, or any other variable the CLI honours)
# Exported into the environment for the agent process; nothing is written to disk
# and nothing is baked into the image.
sbox_agent_apply_credentials() {
  local applied=()
  if [ -n "${IDEASY_SANDBOX_API_KEY:-}" ]; then
    export GH_TOKEN="$IDEASY_SANDBOX_API_KEY"
    applied+=("GH_TOKEN")
  fi
  local name src
  for name in $(sbox_sandbox_env_names); do
    src="IDEASY_SANDBOX_E_${name}"   # indirect expansion (portable across bash builds)
    export "$name"="${!src}"
    applied+=("$name")
  done
  # Nothing to apply is not a failure: the entrypoint runs under `set -e`, so a
  # bare `[ ... ] && echo` returning non-zero here would abort the launch.
  if [ ${#applied[@]} -gt 0 ]; then
    echo "[ideasy-sandbox] Applied credentials for copilot (env): ${applied[*]}" >&2
  fi
  return 0
}

#!/usr/bin/env bash
# =============================================================================
# IDEasy Docker Sandbox - agent profile: Claude Code
# =============================================================================
# Claude Code (https://github.com/anthropics/claude-code) is a node-based tool,
# so the image installs `node` in addition to `claude`.
#
# Claude is isolated by IDEasy: the `claude` commandlet sets CLAUDE_CONFIG_DIR to
# <project>/conf/claude and scrubs ambient ANTHROPIC_* / provider variables out
# of the environment. Its credentials therefore have to be placed in Claude's
# OWN config file (settings.json -> env), not in the environment - that block is
# applied by Claude itself, independent of the scrubbed environment. Nothing is
# baked into the image; the file lives under the container-local $IDE_ROOT and is
# discarded on exit (--rm).
# =============================================================================

# Tools the image must install for this agent.
SBOX_AGENT_TOOLS="node claude"

# Apply Claude's credentials at container start (called by entrypoint.sh).
#   * IDEASY_SANDBOX_API_KEY      -> ANTHROPIC_API_KEY
#   * IDEASY_SANDBOX_E_<NAME>     -> <NAME>  (e.g. ANTHROPIC_BASE_URL,
#     ANTHROPIC_AUTH_TOKEN, ANTHROPIC_MODEL for a proxy / custom endpoint)
# Writes them into Claude's isolated settings.json -> env. No-op when there is no
# credential, so an empty settings.json is only created when something was set.
sbox_agent_apply_credentials() {
  local settings="${SBOX_PROJECT}/conf/claude/settings.json"
  local -a pairs=()
  [ -n "${IDEASY_SANDBOX_API_KEY:-}" ] && pairs+=("ANTHROPIC_API_KEY=IDEASY_SANDBOX_API_KEY")
  local name
  for name in $(sbox_sandbox_env_names); do
    pairs+=("${name}=IDEASY_SANDBOX_E_${name}")
  done
  [ ${#pairs[@]} -gt 0 ] || return 0
  sbox_settings_merge "$settings" "${pairs[@]}"
}

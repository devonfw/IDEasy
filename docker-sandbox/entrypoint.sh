#!/usr/bin/env bash
# IDEasy Docker Sandbox - container entrypoint.
#
# Runs the agent commandlet passed in (the agent + its args, e.g.
# `claude -p "fix the test"`) against the container-local `sandbox` project,
# with the working directory set to the mounted repository (by the launcher).
#
# Running the agent through the `ideasy` commandlet (rather than the bare binary)
# is deliberate: `ideasy <tool>` installs/verifies the tool, relocates its config
# into the container (CLAUDE_CONFIG_DIR=$IDE_ROOT/sandbox/conf/claude), scrubs
# ambient provider/auth env vars, and puts the tool on PATH - isolating the agent
# from any secrets that may have leaked into the container.
#
# We activate IDEasy simply by exporting IDE_ROOT and invoking the CLI, rather
# than `source`ing the installation `functions` script: that script is written
# for interactive, non-strict shells and (a) references unset variables such as
# $IDE_OPTIONS (fatal under `set -u`) and (b) triggers a full environment refresh
# on load that is slow and emits noise. For a one-shot agent launch neither is
# needed - `exec ideasy <tool>` applies the project environment to the agent.

# No `functions` script is sourced here, so a strict shell is safe.
set -euo pipefail

export IDE_ROOT="/projects"

# --- Optional: apply agent credentials / endpoint config ---------------------
# The launcher may pass credentials under IDEASY_SANDBOX_* names (which IDEasy
# does NOT scrub): IDEASY_SANDBOX_API_KEY -> ANTHROPIC_API_KEY, and any number of
# IDEASY_SANDBOX_E_<NAME> -> <NAME> (e.g. ANTHROPIC_BASE_URL, ANTHROPIC_AUTH_TOKEN
# for a proxy). We merge them into Claude's isolated config (settings.json ->
# env), the sanctioned place for values that survive IDEasy's scrub of ambient
# ANTHROPIC_* variables - Claude applies its own settings.json env block itself,
# independent of the environment IDEasy hands it. Nothing is baked into the image;
# all of this lives under the container-local $IDE_ROOT and is discarded on exit
# (--rm). Best-effort: any failure only logs a warning; the agent still starts.
apply_credentials() {
  # Collect the values to apply (API key + any IDEASY_SANDBOX_E_* vars). Nothing
  # to do when there is none.
  local has_credential=0
  [ -n "${IDEASY_SANDBOX_API_KEY:-}" ] && has_credential=1
  case "$(env)" in *IDEASY_SANDBOX_E_*) has_credential=1 ;; esac
  [ "$has_credential" -eq 1 ] || return 0

  local project="${IDE_ROOT}/sandbox"
  local settings="${project}/conf/claude/settings.json"
  mkdir -p "$(dirname "$settings")"

  # Locate the provisioned node binary. IDEasy exposes it at <project>/software/node
  # (a symlink to the installed version); the node tarball keeps the binary under bin/.
  local node_bin=""
  local candidate
  for candidate in \
    "${project}/software/node/bin/node" \
    "${project}/software/node/node"; do
    if [ -x "$candidate" ]; then node_bin="$candidate"; break; fi
  done
  if [ -z "$node_bin" ] && command -v node >/dev/null 2>&1; then
    node_bin="$(command -v node)"
  fi
  if [ -z "$node_bin" ]; then
    echo "[ideasy-sandbox] WARNING: node not found - cannot apply credentials; the agent will be unauthenticated" >&2
    return 0
  fi

  # Read everything from the environment (not argv) so secrets never appear in `ps`.
  # Merge: ANTHROPIC_API_KEY (from the shorthand) plus each IDEASY_SANDBOX_E_<NAME>.
  if "$node_bin" -e '
      const fs = require("fs");
      const file = process.argv[1];
      let cfg = {};
      try { cfg = JSON.parse(fs.readFileSync(file, "utf8")); } catch (e) { cfg = {}; }
      if (!cfg.env || typeof cfg.env !== "object") cfg.env = {};
      const applied = [];
      const api = process.env.IDEASY_SANDBOX_API_KEY;
      if (api) { cfg.env.ANTHROPIC_API_KEY = api; applied.push("ANTHROPIC_API_KEY"); }
      for (const [k, v] of Object.entries(process.env)) {
        const prefix = "IDEASY_SANDBOX_E_";
        if (k.startsWith(prefix)) {
          const name = k.slice(prefix.length);
          cfg.env[name] = v;
          applied.push(name);
        }
      }
      fs.writeFileSync(file, JSON.stringify(cfg, null, 2) + "\n");
      process.stderr.write("[ideasy-sandbox] Applied to " + file + ": " + applied.join(", ") + "\n");
    ' "$settings"; then
    :
  else
    echo "[ideasy-sandbox] WARNING: failed to apply credentials to ${settings}" >&2
  fi
}

apply_credentials

# `keep-alive` starts the container and keeps it running so you can `docker exec`
# into it later (e.g. `docker exec -it sbx bash`). Credentials are already applied
# above. Any other argument is treated as the agent commandlet plus its args.
if [ "${1:-}" = "keep-alive" ]; then
  exec sleep infinity
fi

# The command is the agent commandlet plus its arguments, e.g. `claude -p "..."`.
# `exec` replaces this shell so the agent runs in the foreground and receives
# terminal signals (Ctrl-C) directly.
exec /projects/_ide/installation/bin/ideasy "$@"

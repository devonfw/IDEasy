#!/usr/bin/env bash
# IDEasy Docker Sandbox - container entrypoint.
#
# Activates the IDEasy environment for the container-local `sandbox` project and
# runs the command passed in (the agent commandlet + its args, e.g.
# `claude -p "fix the test"`).
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

export IDE_ROOT="/projects"

# The command is the agent commandlet plus its arguments, e.g. `claude -p "..."`.
# `exec` replaces this shell so the agent runs in the foreground and receives
# terminal signals (Ctrl-C) directly.
exec /projects/_ide/installation/bin/ideasy "$@"

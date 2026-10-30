# syntax=docker/dockerfile:1

# =============================================================================
# IDEasy Docker Sandbox - base image
# =============================================================================
# Provisions an isolated IDEasy "project" (named `sandbox`) inside the image so
# that a container can run a supported AI coding agent (Claude Code, GitHub
# Copilot CLI, ...) against a bind-mounted git repository, with a
# container-local (host-free) IDEasy runtime.
#
# The image is AGENT-NEUTRAL: which agents it provisions is controlled by the
# AGENTS build-arg, and each agent's specifics (which tools to install, how it
# receives credentials) live in its profile at docker-sandbox/agents/<agent>.sh.
# This file never hard-codes a single agent.
#
# See docker-sandbox/docker-sandbox.sh (the host launcher) and
# documentation/docker-sandbox.adoc (user documentation, security model).
#
# [EXPERIMENTAL] Aimed at technically skilled users. No warranty or support.
#
# Design notes
#   - The container IDEasy root is /projects ($IDE_ROOT). IDEasy's fresh install
#     honours the IDE_ROOT environment variable ONLY if that directory already
#     exists (see getIdeRootPathFromEnv / determineIdeRoot), otherwise it falls
#     back to $HOME/projects. The install step below therefore `mkdir -p`s
#     $IDE_ROOT first. The launcher must mount the repository into
#     /projects/sandbox/workspaces/main accordingly.
#   - The host git repository is bind-mounted over a subfolder of that workspace
#     so the agent edits the *real* repo, while everything under /projects
#     (conf/, software/, _ide/) is the container-local runtime, discarded on
#     `docker run --rm`.
#   - No host SSH keys or SSH agents are ever mounted (see the doc). Push/PR
#     auth is an explicit, opt-in concern handled by the launcher.
#   - Build-time provisioning is cached in image layers, so `docker run` is fast
#     after the first build. Pin IDEASY_VERSION / EXTRA_TOOLS for reproducibility.
# =============================================================================

# Pin the IDEasy release ("" = latest release, like the one-liner install.sh).
ARG IDEASY_VERSION=""
# Settings git repository for the sandbox project. "-" = IDEasy default settings
# (https://github.com/devonfw/ide-settings). Point this at your own fork to layer
# project-specific tools/variables into the sandbox.
ARG IDE_SETTINGS_REPO="-"
# Comma-separated list of the supported agents to provision into the image
# (default: both, so a single image can run any of them). Each name must match a
# profile in docker-sandbox/agents/<name>.sh; the image installs the union of
# the tools each profile declares (SBOX_AGENT_TOOLS). Add/adjust agents here or
# via EXTRA_TOOLS without touching this file.
# Pass via: docker build --build-arg AGENTS="claude,copilot"
ARG AGENTS="claude,copilot"
# Comma-separated list of *additional* IDEasy tools to install into the sandbox
# beyond the agents' own dependencies (e.g. "java,mvn" for a JVM project).
# Pass via: docker build --build-arg EXTRA_TOOLS="java,mvn"
ARG EXTRA_TOOLS=""

FROM ubuntu:22.04

# Bash so the RUN steps can use IDEasy's `functions` (bash functions) and shell
# conveniences. Keep the image non-interactive and small.
SHELL ["/bin/bash", "-c"]

ENV DEBIAN_FRONTEND=noninteractive \
    HOME=/root \
    IDE_ROOT=/projects \
    PATH=/projects/_ide/installation/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

# git is mandatory (the agent works on the mounted repository); curl + ca-certificates
# for the IDEasy installer; tar/gzip/unzip for archive handling; openssh-client is
# present so `git`/`gh` can function, but NO host SSH keys or agents are mounted.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       git curl ca-certificates tar gzip unzip openssh-client bash \
    && rm -rf /var/lib/apt/lists/*

# ---- Install the IDEasy CLI -------------------------------------------------
# Mirrors the repository one-liner install.sh (download the released ide-cli
# archive and run `setup`), but without the `</dev/tty` redirect (no TTY during
# docker build) and with IDE_ROOT pinned so IDEasy installs inside the image.
ARG IDEASY_VERSION
RUN set -ex; \
    export IDE_ROOT=/projects; \
    os=linux; arch=x64; \
    version="${IDEASY_VERSION:-$(curl -fsSL https://api.github.com/repos/devonfw/IDEasy/releases/latest \
        | grep '"tag_name"' | cut -d '"' -f4 | sed 's#^release/##')}"; \
    archive="ide-cli-${version}-${os}-${arch}.tar.gz"; \
    url="https://repo1.maven.org/maven2/com/devonfw/tools/IDEasy/ide-cli/${version}/${archive}"; \
    echo "Installing IDEasy ${version} from ${url}"; \
    curl -fSL --progress-bar -o /tmp/${archive} "${url}"; \
    mkdir -p /tmp/ideasy-install; \
    tar xzf /tmp/${archive} -C /tmp/ideasy-install; \
    # Create IDE_ROOT before setup: the installer only honours the IDE_ROOT env
    # variable if the directory already exists (see getIdeRootPathFromEnv),
    # otherwise it silently falls back to $HOME/projects.
    mkdir -p "${IDE_ROOT}"; \
    # Pre-accept the license so the install never blocks on the "Do you accept
    # these terms?" prompt. setup runs `ideasy -ft install` (NOT batch mode) with
    # no TTY (stdin from /dev/null), so the interactive yes/no prompt would
    # otherwise crash on readLine() at EOF. The check is a pure file-existence
    # test (see AbstractIdeContext.ensureLicenseAgreement), so any marker works.
    mkdir -p "${HOME}/.ide" && echo "accepted during image build" > "${HOME}/.ide/.license.agreement"; \
    cd /tmp/ideasy-install && bash setup </dev/null; \
    # setup pipes through `tee`, so a failed install exits 0 (tee's status) and
    # `set -e` would not catch it. Fail loudly if the CLI was not actually installed.
    test -f "${IDE_ROOT}/_ide/installation/functions"; \
    rm -rf /tmp/ideasy-install /tmp/${archive}

# ---- Provision the sandbox project (automatic provisioning) -----------------
# Creates the IDEasy project `sandbox` under IDE_ROOT and installs the tools the
# selected agents declare. For each agent in AGENTS we source its profile
# (agents/<agent>.sh) and collect SBOX_AGENT_TOOLS, then install the union
# (de-duplicated) of those tools plus any EXTRA_TOOLS. `--skip-tools` on create
# avoids installing the full default toolchain; `--skip-repositories` avoids
# cloning the default repositories. AGENTS / EXTRA_TOOLS add project-specific
# agents and tools without editing this file.
ARG IDE_SETTINGS_REPO
ARG AGENTS
ARG EXTRA_TOOLS
COPY agents/ /opt/ideasy-sandbox/agents/
RUN set -ex; \
    export IDE_ROOT=/projects; \
    # Source WITHOUT `set -u` (nounset): the functions script, and the bare `ide`
    # call it runs on load, reference unset variables (e.g. $IDE_OPTIONS) and the
    # script itself `unset`s IDE_HOME before re-testing it. It is written for
    # interactive, non-strict shells, so nounset would abort it. `set -e` is kept
    # so any real command failure still stops the build.
    source /projects/_ide/installation/functions; \
    # Global options (e.g. -b = batch/non-interactive) precede the commandlet.
    ide -b create sandbox "${IDE_SETTINGS_REPO}" --skip-tools --skip-repositories; \
    # Collect the tools the selected agents declare (union, de-duplicated).
    AGENT_TOOLS=""; \
    for a in $(echo "${AGENTS}" | tr ',' ' '); do \
      if [ ! -f "/opt/ideasy-sandbox/agents/${a}.sh" ]; then \
        echo "ERROR: no agent profile for '${a}' (/opt/ideasy-sandbox/agents/${a}.sh)" >&2; \
        exit 1; \
      fi; \
      . "/opt/ideasy-sandbox/agents/${a}.sh"; \
      for t in ${SBOX_AGENT_TOOLS}; do \
        case " ${AGENT_TOOLS} " in *" ${t} "*) ;; *) AGENT_TOOLS="${AGENT_TOOLS} ${t}";; esac; \
      done; \
    done; \
    echo "Provisioning agents from tools: ${AGENT_TOOLS}"; \
    for t in ${AGENT_TOOLS}; do ide -b install "${t}"; done; \
    if [ -n "${EXTRA_TOOLS}" ]; then \
      IFS=','; for t in ${EXTRA_TOOLS}; do ide -b install "${t}"; done; unset IFS; \
    fi

# ---- Container entrypoint ---------------------------------------------------
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

# The provisioned IDEasy workspace is the default working directory. The launcher
# mounts the host repository as a subfolder of this workspace
# (/projects/sandbox/workspaces/main/<repo>) and sets -w to that subfolder, so the
# agent starts inside its repository within a complete IDEasy-managed workspace.
WORKDIR /projects/sandbox/workspaces/main

# The launcher appends the agent commandlet + its args as the container command;
# the entrypoint runs `ide <command...>` (e.g. `ide claude -p "fix the test"`).
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]

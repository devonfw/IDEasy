# IDEasy Docker Sandbox (experimental)

> **[EXPERIMENTAL]** This solution is experimental and targets **technically skilled
> users only**. Use at your own risk. No warranty or support is implied.

Run an AI coding agent (default: **Claude Code**; **GitHub Copilot CLI** is also
supported) inside an isolated Docker container that contains an **automatically
provisioned IDEasy workspace**, so the agent can safely perform development
tasks, run tests, and prepare commits/PRs **without affecting your host
environment**.

 Full documentation:
[`documentation/docker-sandbox.adoc`](../documentation/docker-sandbox.adoc).

## What you get

- An isolated container pre-loaded with a **provisioned IDEasy workspace**
  (IDEasy + the supported agents installed at image build time).
- Your git repository **bind-mounted read-write *into* that workspace as a
  subfolder**, so the agent works inside a complete IDEasy-managed workspace and
  its edits and `git commit` operate on your real repository.
- A **container-local IDEasy runtime** that is discarded on exit - the host
  IDEasy installation is never touched.
- Your **host Git identity** passed in, so commits are authored by you.
- **Host SSH keys / agents are NOT mounted** into the container.

## Prerequisites

- A running container engine (IDEasy recommends [Rancher
  Desktop](https://rancherdesktop.io/); Podman is also supported).
- [Git](https://git-scm.com/) with a configured `user.name` / `user.email`.
- An IDEasy workspace/checkout of the repository you want to work on.

## Quick start

Run from inside the repository you want to work on:

```bash
bash docker-sandbox/docker-sandbox.sh                 # Claude Code (default)
bash docker-sandbox/docker-sandbox.sh --agent copilot # GitHub Copilot CLI
```

By default Claude Code starts; pass `--agent <name>` to run another supported
agent (the names are the profiles in `agents/`). The agent starts with its
working directory set to your repository. On first run the image is built (this
provisions IDEasy + the agents in the image and takes a few minutes); subsequent
runs are fast.

### Supported agents

Agents are described by **profiles** in [`agents/`](./agents) - one file per
agent. Each profile declares the IDEasy tools the agent needs and how it
receives its credentials:

- **Claude Code** (`claude`) - credentials go into its isolated
  `conf/claude/settings.json` `env` block.
- **GitHub Copilot CLI** (`copilot`) - credentials are exported as environment
  variables (e.g. `GH_TOKEN`).

Adding an agent is a new file in `agents/`; the shared launcher, `Dockerfile`
and `entrypoint.sh` stay agent-neutral. See the documentation for the full
guide.

### Verify without a container engine (dry run)

`--check` validates the host and `--dry-run` prints the exact `docker run`
command. Neither builds the image nor starts a container, so they work even when
the engine is stopped:

```bash
bash docker-sandbox/docker-sandbox.sh --check
bash docker-sandbox/docker-sandbox.sh --dry-run -- -p "fix the failing test"
```

## Options

| Option           | Description                                                                 |
|------------------|-----------------------------------------------------------------------------|
| `--repo <path>`  | Repository to mount (default: auto-detected from the current directory).     |
| `--image <name>` | Image name (default: `ideasy-sandbox`).                                       |
| `--agent <name>` | Agent to run (default: `claude`). Must be a profile in `agents/<name>.sh`. |
| `--pull`         | Rebuild the image before running.                                             |
| `--check`        | Validate the host and exit (no build, no run).                                |
| `--dry-run`      | Print the exact `docker run` command and exit (no run).                       |
| `--ca-cert <f>`  | CA bundle to trust inside the container (`NODE_EXTRA_CA_CERTS`).              |
| `--api-key <v>`  | The agent's primary credential. Forwarded as `IDEASY_SANDBOX_API_KEY`; the agent profile decides what it means. |
| `--env N=V`      | Extra environment variable for the agent (repeatable), interpreted by the agent profile. |
| `--`             | Forward everything after `--` to the agent.                                   |

Example - run a single prompt non-interactively:

```bash
bash docker-sandbox/docker-sandbox.sh -- -p "run the failing test and fix it"
```

Example - run the Copilot CLI with a GitHub token:

```bash
bash docker-sandbox/docker-sandbox.sh --agent copilot --api-key "<token>" -- -p "fix the failing test"
```

## Provisioning agents and more tools

Each agent profile declares the IDEasy tools it needs; the `Dockerfile` installs
the union of tools for the agents you select via the `AGENTS` build-arg
(comma-separated profile names, default `claude,copilot`):

```bash
# only Claude Code
docker build -t ideasy-sandbox --build-arg AGENTS="claude" docker-sandbox/
```

For projects that need additional IDEasy tools beyond what the agents declare
(e.g. a JVM stack), add them with `EXTRA_TOOLS`:

```bash
docker build -t ideasy-sandbox --build-arg EXTRA_TOOLS="java,mvn" docker-sandbox/
```

Point the sandbox at your own settings fork to layer in project-specific
tools/variables:

```bash
docker build -t ideasy-sandbox \
  --build-arg IDE_SETTINGS_REPO="https://github.com/you/ide-settings.git" \
  docker-sandbox/
```

## Security notes

- Only your **Git identity** is passed into the container (see
  `GIT_AUTHOR_*` / `GIT_COMMITTER_*`).
- **Host SSH keys and SSH agents are NOT mounted.** Pushing or opening PRs from
  inside the container is an **explicit, opt-in** concern - see the
  documentation for how to supply a credential.
- Agent credentials are **never baked into the image**. The launcher forwards
  them under neutral names (`IDEASY_SANDBOX_API_KEY`, `IDEASY_SANDBOX_E_*`); the
  agent profile applies them at container start, and they are discarded on exit.
- The container is started with `--rm`; the container-local IDEasy runtime is
  discarded when the container exits.

# IDEasy Docker Sandbox (experimental)

> **[EXPERIMENTAL]** This solution is experimental and targets **technically skilled
> users only**. Use at your own risk. No warranty or support is implied.

Run an AI coding agent (default: **Claude Code**) inside an isolated Docker
container that contains an **automatically provisioned IDEasy workspace**, so the
agent can safely perform development tasks, run tests, and prepare commits/PRs
**without affecting your host environment**.

The idea is modelled on the [pi.dev sandbox](https://github.com/capgemini/pi.dev)
(a devonfw/Capgemini sibling of IDEasy). Full documentation:
[`documentation/docker-sandbox.adoc`](../documentation/docker-sandbox.adoc).

## What you get

- An isolated container pre-loaded with a **provisioned IDEasy workspace**
  (IDEasy + Claude Code installed at image build time).
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
bash docker-sandbox/docker-sandbox.sh
```

Claude Code starts inside the container with its working directory set to your
repository. On first run the image is built (this provisions IDEasy + Claude
Code and takes a few minutes); subsequent runs are fast.

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
| `--agent <name>` | Agent commandlet to run (default: `claude`).                                  |
| `--pull`         | Rebuild the image before running.                                             |
| `--check`        | Validate the host and exit (no build, no run).                                |
| `--dry-run`      | Print the exact `docker run` command and exit (no run).                       |
| `--ca-cert <f>`  | CA bundle to trust inside the container (`NODE_EXTRA_CA_CERTS`).              |
| `--`             | Forward everything after `--` to the agent.                                   |

Example - run a single prompt non-interactively:

```bash
bash docker-sandbox/docker-sandbox.sh -- -p "run the failing test and fix it"
```

## Provisioning more tools

By default the image provisions `node` and `claude`. For projects that need
additional IDEasy tools (e.g. a JVM stack), build the image with the tools you
need:

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
- The container is started with `--rm`; the container-local IDEasy runtime is
  discarded when the container exits.

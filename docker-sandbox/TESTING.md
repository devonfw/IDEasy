# Manual Test Guide — Docker Sandbox (local)

> **Audience:** a technically skilled user verifying the sandbox on their own machine.
>
> **Prerequisites**
> - A running container engine (Docker / Rancher Desktop / Podman).
> - `git` with a configured `user.name` / `user.email`.
> - A checkout of the repository (this one by default).
> - A fresh agent credential (API key or token) — the previous `sk-...` token must be considered leaked.
> - On **Windows**, run everything from WSL.

> **Security**
> Replace every `<TOKEN>` / `<ENDPOINT>` below with your own values. Never paste a
> live token into a file you commit. The bundle `docker-sandbox/capgemini-ca.pem`
> is git-ignored on purpose — keep it local only.

---

## Step 0 — Prerequisites (once)

```bash
# 1. Engine is up
docker info >/dev/null && echo "engine OK" || echo "ENGINE NOT RUNNING"

# 2. Host Git identity (the launcher reads these to attribute commits)
git config user.name
git config user.email

# 3. (First time only) build the image — provision IDEasy + node + claude
cd <wsl-path>/workspaces/main/IDEasy
bash docker-sandbox/docker-sandbox.sh --pull
```

**PASS:** `engine OK`, a name/email are printed, and the build finishes without
`failed` / `error` lines.

---

## Step 1 — Validate the host, no container started

```bash
cd <wsl-path>/workspaces/main/IDEasy
bash docker-sandbox/docker-sandbox.sh --check
```

- **Purpose:** check engine + Dockerfile + git identity.
- **PASS:** prints `Check passed. (No image was built or started.)`
- Works even when the engine is stopped.

---

## Step 2 — Preview the exact `docker run` (no container started)

```bash
bash docker-sandbox/docker-sandbox.sh --dry-run -- -p "reply with the single word: hello"
```

- **Purpose:** review the command the launcher will run.
- **PASS:** it prints a `docker run` line containing `-v <repo>:...`, `-w ...`, and
  the agent `claude`. No container is started.

---

## Step 3 — End-to-end round-trip (the critical test)

This is the single most important check: it proves the agent runs, credentials are
injected, and the corporate CA is trusted.

```bash
cd <wsl-path>/workspaces/main/IDEasy

CA="$(pwd)/docker-sandbox/capgemini-ca.pem"

docker run --rm --init \
  -e NODE_EXTRA_CA_CERTS=/etc/ssl/ideasy-ca/ca-bundle.pem \
  -e IDEASY_SANDBOX_E_ANTHROPIC_BASE_URL="https://<ENDPOINT-COMPANY>" \
  -e IDEASY_SANDBOX_E_ANTHROPIC_AUTH_TOKEN="<YOUR-TOKEN>" \
  -e IDEASY_SANDBOX_E_ANTHROPIC_MODEL=code \
  -v "${CA}:/etc/ssl/ideasy-ca/ca-bundle.pem:ro" \
  -w /projects/sandbox/workspaces/main \
  ideasy-sandbox:latest claude -p 'Reply with exactly the single word: hello'
```

**How to read the output:**

| You see | Meaning |
|---|---|
| `[ideasy-sandbox] Applied to ...: ANTHROPIC_MODEL, ANTHROPIC_AUTH_TOKEN, ANTHROPIC_BASE_URL` | credential injection works |
| **No** `warn: ignoring extra certs ... Is a directory` | CA bundle loads correctly |
| **No** `Self-signed certificate detected` | corporate CA is trusted |
| `hello` on the last line | **agent runs and calls the API successfully** |
| `unrecognized_model "code"` | non-blocking warning (session-title only) — ignore |

**PASS:** you get `hello` and there is no certificate error.

> Do **not** use `-i -t` in a non-interactive (CI/script) context — it fails with
> `cannot attach stdin to a TTY-enabled container`. Use the `-p` form above.

---

## Step 4 — Lifecycle + `docker exec` (keep-alive)

```bash
# Keep the container running so you can inspect it
docker run -d --init --name sbx \
  -e IDEASY_SANDBOX_E_ANTHROPIC_BASE_URL="https://<ENDPOINT>" \
  -e IDEASY_SANDBOX_E_ANTHROPIC_AUTH_TOKEN="<YOUR-TOKEN>" \
  ideasy-sandbox:latest keep-alive

docker exec -it sbx bash

#   inside the container:
claude                       # start the interactive agent
docker exec sbx ls -la /etc/ssl/ideasy-ca/
docker exec sbx cat /projects/sandbox/conf/claude/settings.json | sed 's/sk-[A-Za-z0-9]*/<hidden>/g'
docker exec sbx printenv | grep -E '^(ANTHROPIC|IDEASY_SANDBOX)' | sed 's/sk-[A-Za-z0-9]*/<hidden>/g'

# Tear down
docker rm -f sbx
```

- **PASS:** you can enter the container; `settings.json` has an `env` block with the
  endpoint/token; the CA is a **file** (not a directory); the token is not leaked
  in plain text in the config you inspect.

---

## Step 5 — Isolation (the core quality property)

Goal: prove the agent edits **only** the real repo and does **not** touch the host
IDEasy installation.

```bash
# 1. Seed a scratch file in the repo
echo "test-line-before" > <repo>/sandbox-test.txt
git status                    # note the before-state

# 2. Ask the agent to modify it
docker run --rm --init \
  -e IDEASY_SANDBOX_E_ANTHROPIC_BASE_URL="https://<ENDPOINT>" \
  -e IDEASY_SANDBOX_E_ANTHROPIC_AUTH_TOKEN="<YOUR-TOKEN>" \
  -w /projects/sandbox/workspaces/main/IDEasy \
  ideasy-sandbox:latest claude -p 'Append the text "sandbox-ran" to the file sandbox-test.txt. Do not commit.'

# 3. Verify on the HOST (the real repo)
git status                    # sandbox-test.txt shows as modified
cat <repo>/sandbox-test.txt   # contains "sandbox-ran"
git diff

# 4. Verify the host IDEasy runtime is untouched
#    conf/ and software/ of your host workspace must be unchanged
```

- **PASS:** the file on the host was modified by the agent, while the host `conf/`
  and `software/` are unchanged.
- **Cleanup:** `git checkout -- sandbox-test.txt` (or delete the file).

---

## Step 6 — `--api-key` path (alternative credential path)

```bash
export IDEASY_SANDBOX_API_KEY="<YOUR-TOKEN>"
bash docker-sandbox/docker-sandbox.sh -- -p "reply with the single word: ok"
```

- **PASS:** you get `ok`. Confirms the `--api-key` / `IDEASY_SANDBOX_API_KEY`
  shorthand works, not only the `--env` path.

---

## Checklist

| # | Test | Pass condition |
|---|------|----------------|
| 0 | Prereqs + image build | `engine OK`, identity present, build clean |
| 1 | `--check` | `Check passed` |
| 2 | `--dry-run` | command printed, nothing started |
| 3 | **E2E + proxy + CA** | gets `hello`, no cert error |
| 4 | keep-alive + `exec` | enter container, config/cert/env correct |
| 5 | Isolation | agent edits the real repo, host IDEasy unchanged |
| 6 | `--api-key` | gets `ok` |

Step 3 is the gating test: if it passes, the rest confirm quality and security.

---

## Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| `cannot attach stdin to a TTY-enabled container` | You used `-i -t` without a TTY. Use the `-p` form (Step 3). |
| `Self-signed certificate detected` | The CA bundle is missing or wrong. Rebuild it from the endpoint chain (intermediate + root) and re-verify with `openssl verify -CAfile <bundle> <leaf>` → `OK`. |
| `Is a directory` warning | The source file for the CA mount does not exist as a **file** — Docker created a directory. Ensure `capgemini-ca.pem` is a real file. |
| Agent cannot authenticate | Credentials not passed. Use `--api-key` or `--env ANTHROPIC_AUTH_TOKEN=...`; IDEasy scrubs ambient `ANTHROPIC_*` variables, so they must be injected at start. |
| Windows path errors in `docker run` | Run from WSL; keep container paths Unix-style. |
| `unrecognized_model` warning | Non-blocking. Clear by mapping the model via `behavesAs` / `modelOverrides` in the settings repo. |

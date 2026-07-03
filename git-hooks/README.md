# git-hooks

Version-controlled git hooks for IDEasy. Shared with the whole team so everyone gets the same
pre-commit safety net.

## Install (required, once per clone)

From the repository root (Git Bash on Windows):

```sh
./install-hooks.sh
```

This sets `core.hooksPath` to this directory **for the current clone only** (repo-local git
config — it never touches your global config and cannot interfere with other repositories).

Uninstall with `git config --unset core.hooksPath`.

## Hooks

- **pre-commit** — runs the mandatory harness in ratchet mode (legacy code is grandfathered; every
  file you change must comply):
  1. build + tests — `mvn -q verify -pl '!documentation'` (docs skipped: slow AsciiDoc generation);
  2. formatting — `mvn spotless:check` (ratcheted to origin/main, i.e. only your changed files);
  3. static analysis — `mvn checkstyle:check` scoped to the Java files staged in the commit.

  Any violation or test failure blocks the commit.

### Escape hatches (use sparingly)

| Goal | Command |
| --- | --- |
| Skip all hooks for one commit | `git commit --no-verify` |
| Skip only the pre-commit build | `HARNESS_SKIP=1 git commit ...` |

The hooks are plain POSIX shell scripts and run via the bash bundled with Git for Windows, so the
same files work on Windows, macOS, and Linux.

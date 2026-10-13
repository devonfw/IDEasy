# AGENTS.md

Shared instructions for AI coding agents (GitHub Copilot, Claude Code, and others).
This is the single source of truth — Copilot reads `AGENTS.md` natively, and Claude Code
reads it via the `@AGENTS.md` import in `CLAUDE.md`.

## Project Overview

This project is a software orchestration and setup tool called IDEasy, which enables automated
provisioning and configuration of software packages on local hardware.
To keep multiple configurations in sync, a settings folder can be distributed via git.
The standard configuration of IDEasy is maintained in the repository
https://github.com/devonfw/ide-settings.

## Folder Structure

- `/cli`: Source code for the IDEasy CLI
- `/documentation`: Source code for the AsciiDoc documentation
- `/gui`: Source code for the UI of IDEasy
- `/url-updater`: Source code of the URL updater that refreshes all files in
  https://github.com/devonfw/ide-urls with fresh URLs crawled from the web for all supported
  software packages
- `/windows-installer`: Source code to compile an MSI installer for IDEasy

## Where Things Live

- Commandlets: `cli/src/main/java/com/devonfw/tools/ide/commandlet`, registered in
  `CommandletManagerImpl`.
- Tool commandlets: `cli/src/main/java/com/devonfw/tools/ide/tool/<tool>`.
- Help texts (NLS) for every commandlet and option: `cli/src/main/resources/nls/Help*.properties`
  (English and German).
- URL updaters: `url-updater/src/main/java/com/devonfw/tools/ide/url/tool/<tool>`, registered in
  `UpdateManager`.
- How to add a commandlet or url-updater: `documentation/contributing/commandlet-and-url-updater.adoc`.
  How to write tests: `documentation/contributing/junit-testing.adoc`.

## Git Remotes and Branches

- `origin` is the contributor's fork, `upstream` is `devonfw/IDEasy` (see
  `documentation/contributing/setup.adoc`). The fork's `main` is often stale: compare and branch
  against `upstream/main`, never `origin/main`.
- Branch names: `feature/«issue-id»-«brief-description»`.
- Push to `origin`, open the pull request against `devonfw/IDEasy`.

## Development Method

- TDD is the standard for features and bug fixes: write a failing test first, make it pass, refactor.
  For a bug, step one is a test that reproduces it and fails.

## Test Execution

- During development, execute specific test classes from your IDE or via
  `mvn -Dtest=ClassName test` in the module folder of interest (e.g. the `cli` folder, i.e. the
  `ide-cli` Maven module).
- Before a pull request, run all tests once (the PR checklist requires it). From the root folder use
  `mvn clean test -pl '!documentation'`: a plain `mvn clean test` in the root also runs the slow
  documentation generation (the `ide-doc` module), so use it only when the documentation changed.
- All integration tests can be executed via the script `cli/src/test/all-tests.sh`.

## Development Harness (cheap → expensive)

Run the tiers covering your change proactively — cheap ones constantly, expensive ones before commit
and review — and fix what they report.

1. Conventions (free): follow `documentation/contributing/coding-conventions.adoc` + `.editorconfig`.
2. Compile: `mvn -q -o compile` in the changed module.
3. Tests: `mvn -Dtest=ClassName test` or `mvn -q test` in the module folder (never repo root).
4. Format: `mvn spotless:apply` (auto-fix; ratcheted to `origin/main`, so only files you changed).
   If your `origin` is a stale fork, pass `-Dspotless.ratchetFrom=upstream/main`. Spotless cannot
   read a `git worktree` checkout ("Cannot find git repository"): run it in a regular clone.
5. Lint: `mvn checkstyle:check` in the module folder — must pass for every file you change.
6. Build + tests: `mvn -q verify -pl '!documentation'` from repo root.

Enforcement is by CHANGED code (ratchet), not the whole legacy tree. Git hooks are mandatory: run
`./install-hooks.sh` once per clone; pre-commit then runs steps 4–6 on every commit. Skip a single
commit only when strictly necessary with `git commit --no-verify`.

## Coding Conventions

- ALWAYS review and refactor your code after implementation to comply with the coding standards:
  `documentation/contributing/coding-conventions.adoc`.

## Exception Handling

- Wrap a caught exception with message AND cause: `throw new IllegalStateException("<meaningful
  message>", e)`. A bare `throw new RuntimeException(e)` loses the context.
- `CliException` is ONLY for expected, user-facing CLI aborts (defined exit code, stacktrace
  suppressed). Do not use it to wrap unexpected technical failures like an `IOException`.
- Details: `documentation/contributing/coding-conventions.adoc`, section "Catching and handling
  Exceptions".

## Definition of Done

- ENSURE the Definition of Done is met before requesting review:
  `documentation/contributing/DoD.adoc`.

## Commit Messages

- Always commit your changes in small logical units associated with an issue, using the format:
  `#«issue-id»: «describe your change»` — GitHub then auto-links the commit to the issue.
- For changes driven by an issue in a different repository (e.g. a change in `ide-settings` due to
  an issue in `IDEasy`), use: `«organization»/«repository»#«issue-id»: «describe your change»`.
- Keep the title short and informative; use the description body to elaborate when needed.
- See `documentation/contributing/commit.adoc` for full details.

## Changelog

- Add every issue implemented by a PR to `CHANGELOG.adoc`, unless the issue is labelled `internal`.
- Put the entry under the topmost version (the next open release), after its last bullet:
  `* https://github.com/devonfw/IDEasy/issues/«issue-id»[#«issue-id»]: «issue title»`

## DO NOT: AI attribution in commits and PRs

These are strict prohibitions. Contributions carrying AI authorship/attribution cannot be accepted
(CLA assistant cannot resolve AI-authored commits).

- DO NOT add `Co-authored-by:` (or any co-author) trailers referencing AI tools or assistants
  (e.g. Claude, Copilot, "Claude Code", an AI model name) to commits.
- DO NOT add AI advertising or "generated with"/"co-authored with" remarks to commit messages,
  commit descriptions, PR titles, PR descriptions, or PR/issue comments.
- DO NOT set commit author or committer identity to an AI tool or bot. Commit under the human
  contributor's git identity.
- Commit messages must contain only the issue reference and a description of the change — nothing
  about the tooling used to produce it.

## Ask Before Anything Public

- Comments, reviews, labels, issues, pull requests and status changes on GitHub are public. Show the
  draft to the user and post it only after the user explicitly confirmed it.
- Never approve or merge a pull request on the user's behalf.

## Pull Request Guidelines

- ALWAYS add the checklist from `.github/PULL_REQUEST_TEMPLATE.md` as acceptance criteria for all
  pull requests, and mark items already done.

## Testing Guidelines

- Always use AssertJ for assertions in all Java tests.
- Extend your test classes from `org.assertj.core.api.Assertions` to avoid static imports.
- Do not use JUnit static imports for assertions.
- When implementing tests that create symbolic links (via `FileAccess.symlink` or
  `Files.createSymbolicLink`), always call `WindowsSymlinkTestHelper.assumeSymlinksSupported()` at
  the beginning of the test method. This gracefully skips the test on Windows systems without
  symlink permissions (admin or Developer Mode), giving new contributors a better experience.

## Agent Skills

Shared workflows live as skills in `.claude/skills/<name>/SKILL.md` (read by Claude Code and by
GitHub Copilot in VS Code) and are started with `/<name>`. Project settings for Claude Code are in
`.claude/settings.json`. See `documentation/contributing/ai-agents.adoc`.

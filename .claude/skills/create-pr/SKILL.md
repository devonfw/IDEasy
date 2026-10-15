---
name: create-pr
description: Finalize local changes for an issue into a feature branch and open a pull request. Use when asked to create, open or update a pull request for IDEasy.
argument-hint: [issue-number]
model: haiku
---

The user asked to finalize the work on an issue with local changes and potentially an already
set-up feature branch.

**Clarify state of work**

- Clarify which issue is intended to be worked on. If unclear: STOP, ASK THE USER to specify the
  issue number, AND RESUME. (Provided after the command: $0)
- Check the state of the local repository:
  - Whether uncommitted changes exist. If so: STOP, ASK THE USER whether these are intended to be
    included in the pull request, AND RESUME.
  - Which remotes are configured.
  - Which branch is currently checked out, and whether the directory is a git worktree
    (`git rev-parse --git-common-dir` differs from `--git-dir`).

**Verify preconditions and prepare PR**

- IF a feature branch for the issue is already checked out (e.g. the worktree created by the
  `contribute` skill), stay on it. Do not stash, do not switch to `main`.
- OTHERWISE, if the changes sit on `main` or an unrelated branch:
  1. Fetch `upstream`.
  2. Create the feature branch `feature/«issue-id»-«brief-description»` from `upstream/main`, taking
     the uncommitted changes along (`git switch -c <branch> upstream/main` keeps them; if git
     refuses because of conflicts, use `git stash push -u -m "<issue-id>"`, switch, then
     `git stash apply`). ASK for support if conflicts cannot be resolved automatically.
- Do not push to or sync the fork's `main`: the PR is created from the feature branch, and the
  base is `upstream/main` (see the remote convention in `AGENTS.md`).
- If the feature branch is behind `upstream/main`, merge `upstream/main` into it before opening the
  PR.

**Tasks**

1. Understand the coding conventions (`documentation/contributing/coding-conventions.adoc`) — YOU
   NEED TO comply with them!
2. Make sure you tested your solution. Try to reuse available test classes instead of creating new
   ones, and MAKE SURE you didn't create duplicates.
3. ENSURE the Definition of Done is met (`documentation/contributing/DoD.adoc`) and potentially add
   tasks to your task list if needed.
4. IF unstaged files exist AND THE USER AGREES, commit (short and crisp commit message!) the code
   to the git remote `origin` on the feature branch. MAKE SURE you comply with the coding
   conventions in the commit message, and follow the strict AI-attribution prohibitions in
   AGENTS.md (no AI co-author trailers, no AI remarks in commit messages or PRs).
5. Create a new pull request to the `upstream` repository following `.github/PULL_REQUEST_TEMPLATE.md`.
   Push to `origin` (the fork) and open the PR against `devonfw/IDEasy`:
   `gh pr create -R devonfw/IDEasy --base main --head <login>:<branch>`.
6. MARK all tasks already done in the new pull request's description. Tick a checklist box ONLY if
   it is true. If you did not run something (e.g. the full test suite), leave the box unticked and
   say why.
7. If you can resolve some tasks on your own, take proper action immediately. If there is no change
   on any commandlet (located at `cli/src/main/java/com/devonfw/tools/ide/commandlet`), remove the
   task list for commandlets from the pull request description.

**Write the description for a reviewer without context**

- Explain the decisions and edge cases, not only the changes. Name related issues and open PRs that
  touch the same files.
- The testing instructions must be executable by a stranger: concrete commands in order, the
  preconditions (tool version, project setup, toggles in `conf/ide.properties`) and the expected
  result of each step. "Run the tests" is not a testing instruction.

**Editing an existing PR**

- Fetch the current body first (`gh pr view <n> --json body`). Authors and reviewers edit PR
  descriptions on GitHub, so never overwrite the body from a local copy.

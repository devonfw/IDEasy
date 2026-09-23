---
agent: agent
model: Claude Opus 4.8
---
The user asked to sync every open, non-draft pull request of the `devonfw/IDEasy` repository: bring each branch up to date with its base and auto-resolve conflicts that are limited to the changelog. (For the prioritized status overview, that is a separate prompt: `report-pr-overview`.)

**Scope**

* operate on the current repository (`devonfw/IDEasy`). Enumerate every OPEN, non-draft PR: `gh pr list --state open --limit 200 --json number,title,url,isDraft,mergeable,mergeStateStatus,headRefName,headRepositoryOwner,baseRefName`. Skip anything with `isDraft: true`
* process PRs one at a time. Never touch code you are not explicitly resolving, never force-push, never approve or merge a PR

**Per PR — 1) bring the branch up to date with its base**

* read `mergeStateStatus` / `mergeable`:
  * `BEHIND` (out of date, no conflict) → run `gh pr update-branch <n>` to merge the base in. This is exactly what clears the "This branch is out-of-date with the base branch" state
  * `CLEAN` / `BLOCKED` / `HAS_HOOKS` / `UNSTABLE` with `mergeable: MERGEABLE` → already up to date; nothing to update
  * `UNKNOWN` → GitHub is still computing mergeability; re-fetch (`gh pr view <n> --json mergeable,mergeStateStatus`) before deciding
  * `DIRTY` / `mergeable: CONFLICTING` → conflicts exist; go to step 2
* `gh pr update-branch` only succeeds with no conflict. If it fails, treat the branch as conflicted → step 2

**Per PR — 2) auto-resolve changelog-only conflicts**

* determine EXACTLY which files conflict — GitHub does not list them, so reproduce the merge locally:
  1. `gh pr checkout <n>` (handles fork remotes and tracking)
  2. `git fetch origin <baseRef>` then `git merge origin/<baseRef>` (do not commit yet)
  3. `git diff --name-only --diff-filter=U` → the conflicted files
* **if the ONLY conflicted file is `CHANGELOG.adoc`:** resolve it, `git add CHANGELOG.adoc`, complete the merge, and `git push`
  * the conflict is almost always two PRs each appending a bullet under the top `== <version>` milestone section. Resolution = keep BOTH bullets (union), preserve order, under that section. Delete only the `<<<<<<<` / `=======` / `>>>>>>>` markers
  * **old PRs referencing a stale version header:** if the PR's changelog entry sits under an OLD `== <old-version>` section (it was top-of-file when the PR was opened, but `main` now has newer release sections above it), MOVE the PR's new bullet up into the CURRENT milestone section — the FIRST `== <version>` block in the file — and remove its stale placement. Do not leave the entry stranded under a released version
  * verify: no leftover conflict markers, exactly one bullet per issue, current milestone section on top, still valid AsciiDoc
* **if ANY non-changelog file conflicts:** `git merge --abort` and do NOT resolve. Leave the PR conflicted for manual attention
* pushing to a fork PR requires "Allow edits from maintainers". If the push is rejected, note it and move on

**Closing summary**

Report a concise summary of what you did: which branches you updated, which changelog conflicts you resolved and pushed, and which PRs still need manual attention. Do not build the full status table here — that is `report-pr-overview`.

**Guardrails**

* respect the strict no-AI-attribution rules in `AGENTS.md`: any commit you push (the changelog merge commit) must carry NO AI attribution and NO `Co-authored-by` AI trailer, under the human contributor's git identity. A plain default merge commit message is fine
* never modify code, never resolve non-changelog conflicts, never force-push, never approve or merge PRs

---
agent: agent
model: Claude Opus 4.8
---
The user asked to sync every open, non-draft pull request of the `devonfw/IDEasy` repository: bring each branch up to date with its base, make sure its `CHANGELOG.adoc` entry sits under the **current open release section**, and auto-resolve conflicts that are limited to the changelog. (For the prioritized status overview, that is a separate prompt: `report-pr-overview`.)

**Scope**

* operate on the current repository (`devonfw/IDEasy`). Enumerate every OPEN, non-draft PR: `gh pr list --state open --limit 200 --json number,title,url,isDraft,mergeable,mergeStateStatus,headRefName,headRepositoryOwner,baseRefName,milestone`. Skip anything with `isDraft: true`
* process PRs one at a time. Never touch code you are not explicitly resolving, never force-push, never approve or merge a PR

**Step 0 — resolve the target release section (ONCE, before touching any PR)**

Every later decision depends on knowing which `== <version>` section is the *current, unreleased* one. Get this wrong and you move entries into a shipped release.

* **read the base branch from the LIVE remote, never from the local working copy.** A stale clone is the classic failure: a local `main` weeks behind shows an already-released version as its top section, and `git fetch` may silently be unavailable (sandbox/offline) while `gh` still works. Always resolve from GitHub:
  * `gh api repos/devonfw/IDEasy/contents/.mvn/maven.config?ref=main --jq .content | base64 -d` → `-Drevision=<VERSION>-SNAPSHOT`. **This is the authoritative "next open release"** (`documentation/contributing/DoD.adoc` points contributors at exactly this file or the milestones). Call it `TARGET_SECTION`
  * `gh api repos/devonfw/IDEasy/contents/CHANGELOG.adoc?ref=main --jq .content | base64 -d` → the FIRST `== <version>` heading must equal `TARGET_SECTION`
  * `gh api "repos/devonfw/IDEasy/milestones?state=all&per_page=100"` → the earliest-due **open** `release:<version>` milestone must equal `TARGET_SECTION`
* collect the set of **released versions** (`gh release list --repo devonfw/IDEasy` plus every closed `release:` milestone). Any `== <version>` section naming one of these is **frozen** — never add to it or move an entry into it
* if the three sources disagree, **stop and report**; do not guess a target. A genuine mismatch means release housekeeping is incomplete (e.g. a release shipped but `main` never got its section or version bump) and a human must fix that first
* if `TARGET_SECTION` has no `== ` heading in the base changelog yet, **do not create one** — that heading is added by the release manager in a dedicated commit (`Update CHANGELOG for release <v>`). Report the gap and skip all changelog re-homing this run
* verify the local clone before any local git work: compare `gh api repos/devonfw/IDEasy/commits/main --jq .sha` against `git rev-parse origin/main`. If they differ, `git fetch origin main` first; if fetching is not possible, do NOT do local merges — limit the run to read-only reporting plus `gh pr update-branch`, and say so

**Per PR — 1) bring the branch up to date with its base**

* read `mergeStateStatus` / `mergeable`:
  * `BEHIND` (out of date, no conflict) → run `gh pr update-branch <n>` to merge the base in. This is exactly what clears the "This branch is out-of-date with the base branch" state
  * `CLEAN` / `BLOCKED` / `HAS_HOOKS` / `UNSTABLE` with `mergeable: MERGEABLE` → already up to date; nothing to update
  * `UNKNOWN` → GitHub is still computing mergeability; re-fetch (`gh pr view <n> --json mergeable,mergeStateStatus`) before deciding
  * `DIRTY` / `mergeable: CONFLICTING` → conflicts exist; go to step 3
* `gh pr update-branch` only succeeds with no conflict. If it fails, treat the branch as conflicted → step 3

**Per PR — 2) audit changelog placement (run for EVERY PR, conflict or not)**

This step catches the drift a conflict-only sweep misses. A PR opened while `2026.07.001` was current put its bullet under `== 2026.07.001`; two releases later that section is frozen and the entry is stranded — and because the bullet sits in a different region of the file than `main`'s additions, **this usually produces no merge conflict at all**. Observed in practice: an open PR whose milestone said `2026.08.001` had its bullet parked under `== 2026.05.001`, three releases back.

* find the PR's own bullet(s) — do not assume they are near the top of the file:
  1. `gh api "repos/devonfw/IDEasy/pulls/<n>/files?per_page=100"` → the `CHANGELOG.adoc` entry's `patch`; collect the added `* https://…/issues/<id>[#<id>]: …` lines
  2. fetch the changelog from the PR head (works for forks): `gh api "repos/<headRepo>/contents/CHANGELOG.adoc?ref=<headSha>" --jq .content | base64 -d`
  3. for each added bullet, locate its line and walk **backwards** to the nearest preceding `== ` heading — that is the section the entry currently lives in
* compare that section to `TARGET_SECTION`:
  * **equal** → nothing to do
  * **different (a frozen/released section)** → re-home the entry: delete the bullet from its stale section and re-insert it under the `== TARGET_SECTION` block, appended after the existing bullets and *above* the "The full list of changes for this release can be found in …" footer line. Keep the bullet text byte-identical; only its position changes
  * **PR adds no changelog entry at all** → do not invent one. Note it; the bullet is a Definition-of-Done item for the author (some PRs are legitimately exempt — see the label note in `.github/PULL_REQUEST_TEMPLATE.md`)
* **how to apply the move:** only as part of a merge you are already performing in step 3 (conflicted changelog), or — when there is no conflict — as its own minimal commit on the PR branch touching `CHANGELOG.adoc` and nothing else. Message: `#<issue-id>: move changelog entry to <TARGET_SECTION>` (use the PR number if it references no issue). Then `git push`
* **milestone drift is a separate, related symptom.** If the PR's GitHub `milestone` is a closed release (or unset) while its changelog entry belongs in `TARGET_SECTION`, the milestone field is also wrong. Setting it is a metadata write outside this prompt's branch-and-changelog remit: **report it, do not change it silently.** Offer the one-liner (`gh pr edit <n> --milestone "release:<TARGET_SECTION>"`) and let the user decide, or ask for blanket approval before applying it across the sweep

**Per PR — 3) auto-resolve changelog-only conflicts**

* determine EXACTLY which files conflict — GitHub does not list them, so reproduce the merge locally:
  1. `gh pr checkout <n>` (handles fork remotes and tracking)
  2. `git fetch origin <baseRef>` then `git merge origin/<baseRef>` (do not commit yet)
  3. `git diff --name-only --diff-filter=U` → the conflicted files
* **if the ONLY conflicted file is `CHANGELOG.adoc`:** resolve it, `git add CHANGELOG.adoc`, complete the merge, and `git push`
  * the conflict is almost always two PRs each appending a bullet under the top `== <version>` section. Resolution = keep BOTH bullets (union), preserve order, under that section. Delete only the `<<<<<<<` / `=======` / `>>>>>>>` markers
  * while you are in here, apply the step-2 re-homing in the same resolution: the merged result must end with this PR's bullet under `== TARGET_SECTION` and nothing added to any frozen section
  * verify: no leftover conflict markers; exactly one bullet per issue (no duplicate left behind in the old section); `TARGET_SECTION` is the first `== ` heading; released sections byte-identical to the base; still valid AsciiDoc
* **if ANY non-changelog file conflicts:** `git merge --abort` and do NOT resolve. Leave the PR conflicted for manual attention
* pushing to a fork PR requires "Allow edits from maintainers". If the push is rejected, note it and move on

**Closing summary**

Report a concise summary: the resolved `TARGET_SECTION` and the three sources you confirmed it against; which branches you updated; which changelog entries you re-homed (as `#<n>: == <old-section> → == TARGET_SECTION`); which changelog conflicts you resolved and pushed; **milestone drift found but not changed** (PR → current milestone → suggested milestone) with the `gh pr edit` commands ready to run; and which PRs still need manual attention (non-changelog conflicts, rejected push, no changelog entry, or release housekeeping missing on `main`). Do not build the full status table here — that is `report-pr-overview`.

**Guardrails**

* respect the strict no-AI-attribution rules in `AGENTS.md`: any commit you push (the changelog merge or re-home commit) must carry NO AI attribution and NO `Co-authored-by` AI trailer, under the human contributor's git identity. A plain default merge commit message is fine
* never add to or edit a released `== <version>` section, and never create a new `== <version>` heading — that is the release manager's commit
* never modify code, never resolve non-changelog conflicts, never force-push, never approve or merge PRs, never change milestones or other PR metadata without explicit approval

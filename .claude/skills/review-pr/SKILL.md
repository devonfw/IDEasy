---
name: review-pr
description: Review an IDEasy pull request against its referenced issue and the project conventions, then post an actionable GitHub review after the user confirmed. Use when asked to review a PR or ticket.
argument-hint: [pr-number]
model: opus
---

The user asked to review a pull request. (Provided: $0)

**Clarify scope**

- Determine the PR number. If unclear: STOP, ASK THE USER for the PR number, AND RESUME. If
  `gh pr view <n>` fails, the number is an issue: review the PR linked to it instead.
- Identify the referenced issue(s) from the PR body (e.g. `fixes #1992`). If the PR claims to fix an
  issue, YOU MUST review it against that issue's explicit requirements, not just the diff in
  isolation. If no issue is referenced, review against the change's own stated intent.

## 1. Gather context (read-only)

```bash
gh pr view <n> -R devonfw/IDEasy --json number,title,body,author,baseRefName,headRefOid,mergeable,mergeStateStatus,labels
gh pr diff <n> -R devonfw/IDEasy --name-only
gh pr diff <n> -R devonfw/IDEasy
gh pr checks <n> -R devonfw/IDEasy
gh issue view <referenced-issue> -R devonfw/IDEasy --json body,comments
```

Note if the branch is `BEHIND` main or checks are failing. Analyse against `upstream/main`, not the
local branch and not `origin/main` (that is the fork and often stale, see `AGENTS.md`):
`git fetch upstream main`, then `git show upstream/main:<path>` and
`git grep <pattern> upstream/main -- <path>`. Fetch the PR head as a local ref to read the changed
code in full:

```bash
git fetch upstream pull/<n>/head:review/pr-<n>
```

## 2. Prior discussion and full code

- **Earlier reviews.** Read what reviewers already asked for and check whether the later commits
  address it, instead of raising the same point again:
  ```bash
  gh api repos/devonfw/IDEasy/pulls/<n>/reviews --jq '.[] | "\(.user.login) \(.state) \(.submitted_at) \(.commit_id[0:8])"'
  gh api repos/devonfw/IDEasy/pulls/<n>/comments --jq '.[] | "\(.user.login) \(.path):\(.line) \(.body)"'
  gh api repos/devonfw/IDEasy/pulls/<n>/commits --jq '.[] | "\(.sha[0:8]) \(.commit.committer.date) \(.commit.message | split("\n")[0])"'
  ```
  An approval on an older commit does not cover commits pushed after it.
- **Whole methods, not hunks.** For every changed method, read it and its callers at the PR head
  (`git show review/pr-<n>:<path>`, `git grep <method> review/pr-<n> -- cli/src/main/java`).
  Changed signatures or semantics of shared helpers (e.g. in `FileAccessImpl`) affect callers
  outside the diff, check each of them.

## 3. Review dimensions (cover all that apply)

1. **Against the issue.** Walk its requirements one by one, mark met / partial / missing. If the
   issue also asks for tests or rework that the PR omits, ask whether that is a planned follow-up.
2. **Reuse over reinvention.** Does the PR hand-roll something the project already has
   (`FileAccess`, `IniFile`, an existing `Abstract*Updater` base class)? Compare with the closest
   sibling class, for a new url-updater that is the other updaters in `url-updater/.../tool/`.
3. **Architecture.** Is the change in the right layer and class? Generic concerns belong in the
   generic component (`ProcessContext`, `FileAccess`, `IdeContext`), not duplicated per tool or
   commandlet. Check responsibilities and dependency direction (no tool-specific logic in shared
   base classes), and that interfaces and their `*Mock` or test implementations stay consistent.
   Flag fixes that treat a symptom in one caller while the root cause sits in a shared component.
4. **Clean code.** Readable names, small focused methods, no duplication, no dead code or unused
   fields, no magic values, consistent style with the surrounding code (`this.` for fields, Javadoc
   on public API). Only raise it when it matters for maintainability, and file pure taste as Minor.
5. **Coding conventions** (`documentation/contributing/coding-conventions.adoc`), in particular the
   exception handling rules summarized in `AGENTS.md`.
6. **Tests** (`AGENTS.md`, `documentation/contributing/junit-testing.adoc`): AssertJ only, test
   class extends `org.assertj.core.api.Assertions`, no JUnit static imports, symlink tests call
   `WindowsSymlinkTestHelper.assumeSymlinksSupported()`. A bugfix needs a test that reproduces the bug.
7. **DoD and PR checklist** (`documentation/contributing/DoD.adoc`,
   `.github/PULL_REQUEST_TEMPLATE.md`), including the `CHANGELOG.adoc` entry.
8. **Risk and blast radius.** Behavioural changes with a wide side-effect surface (e.g. a mock used
   by a base test context that flips from no-op to active), backward compatibility, anything that
   passes CI but changes semantics.

### Url-updater specifics

- `JsonUrlUpdater` reads only the first page of the version URL, and the GitHub releases API returns
  30 releases per page by default. For repos that publish many pre-releases or foreign releases,
  almost nothing usable is left. Check how many releases on page 1 survive `mapVersion`. Fix
  pattern: override `doGetVersionUrl()` and request a larger page (`?per_page=100`), like
  `JavaUrlUpdater` does with `page_size`.
- Pre-release filtering (`alpha`, `beta`, `-rc`, ...) already happens in
  `AbstractUrlUpdater.isAcceptVersion`, so a custom `mapVersion` must call `super.mapVersion`.
- A minimum version cutoff should be justified in Javadoc. Verify against the real releases whether
  the assets actually start there.
- CPE mapping is required by `documentation/contributing/cpe-url-updater.adoc`. Check NVD before
  demanding a value, it may not know the vendor yet:
  `curl -s "https://services.nvd.nist.gov/rest/json/cpes/2.0?keywordSearch=<tool>"`.

## 4. Verify before you assert, do not rely on memory

- Convention violation: read the section of the conventions doc *and* grep how production code
  actually does it (`git grep <pattern> upstream/main -- cli/src/main/java`). Cite `file:line`.
- External facts: check them live. For a url-updater that means the real release assets:
  ```bash
  gh api repos/<org>/<repo>/releases/tags/<tag> --jq '.assets[].name'
  gh api "repos/<org>/<repo>/releases?per_page=30" --jq '.[].name'
  ```
- If your own finding does not survive scrutiny, drop it and say so. Pushing back with evidence is
  good, defending an unverified claim is not.

## 5. Show the draft, post only after an explicit OK

Categorize every finding as **Must-fix (blocking)**, **Should-address** or **Minor**. Show the drafted
review in the chat first. A GitHub review is public, so **post only after the user explicitly
confirmed** it, unless they already asked for posting.

Post as a single review via `POST repos/devonfw/IDEasy/pulls/<n>/reviews` with a `body` and a
`comments` array, anchoring each inline comment with `path` + `line` + `side:"RIGHT"` at the PR head
commit.

- **Propose fixes when easily possible.** For a clean, small change embed a GitHub ```suggestion
  block so the author can apply it with one click (multi-line: anchor with `start_line` + `line`).
  Findings on files outside the diff (e.g. a missing test case or `CHANGELOG.adoc` entry) are
  anchored at the most closely related changed line. When the fix spans multiple methods, give
  guidance only. Never push commits to the contributor's branch.
- **Make comments educational.** Link the doc and section for every convention finding so the author
  learns the rule. Cover every occurrence, not just the first.
- **Choose the event by severity:** `REQUEST_CHANGES` when any Must-fix exists, otherwise `COMMENT`.
  NEVER `APPROVE` on the author's behalf.
- No AI attribution of any kind in the review body or comments (`AGENTS.md`).

After posting, verify that the inline comments actually attached, the API silently drops comments
whose line does not map to the diff:

```bash
gh api repos/devonfw/IDEasy/pulls/<n>/comments --jq '.[] | "\(.path):\(.line)"'
```

Re-anchor and re-post anything that was dropped, then report the review URL, the event and a short
Must-fix / Should-address / Minor summary.

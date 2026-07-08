---
description: Review a GitHub pull request against its referenced issue and project conventions, then post an actionable GitHub review
argument-hint: [pr-number]
model: opus
---

The user asked to review a pull request and post the review to GitHub. (Provided: $1)

**Clarify scope**

- Determine the PR number. If unclear: STOP, ASK THE USER for the PR number, AND RESUME.
- Identify the referenced issue(s) from the PR body (e.g. `fixes #1992`). If the PR claims to fix an
  issue, YOU MUST review it against that issue's explicit requirements — not just the diff in
  isolation. If no issue is referenced, review against the change's own stated intent.

**Gather context (read-only first)**

1. Fetch PR metadata, the full diff, and the list of changed files
   (`gh pr view`, `gh pr diff`, `gh pr diff --name-only`).
2. Fetch the referenced issue's full body (`gh issue view … --json body,comments`). Extract every
   concrete requirement it states as a checklist to verify against.
3. Check CI and merge state: `gh pr checks <pr>` and the `mergeable` / `mergeStateStatus` fields.
   Note if the branch is `BEHIND` main (needs rebase) or checks are failing.
4. When the review depends on the current state of the codebase, analyze against `origin/main`
   (`git fetch origin main`, then `git grep …/git show … origin/main`), NOT the local working
   branch — the local branch may carry unrelated changes.

**Review dimensions — cover all that apply**

1. **Against the referenced issue.** Walk the issue's requirements one by one and mark each as met /
   partial / missing. Explicitly assess SCOPE COMPLETENESS: if the issue also asks for reworking
   existing tests or adding feature-level tests (e.g. "we can then properly test `ide upgrade`"),
   and the PR omits that, call it out and ask whether it is a planned follow-up or must land here.
2. **Reuse over reinvention.** Check whether the PR hand-rolls something the project already
   provides (e.g. `IniFile`/`IniFileImpl` for ini files, `FileAccess` for file operations). Prefer
   existing utilities — flag hand-rolled parsing/formatting/IO that duplicates project code.
3. **Coding conventions** (`documentation/contributing/coding-conventions.adoc`). In particular:
   - Exception handling: caught exceptions must be wrapped as
     `throw new IllegalStateException("<meaningful message>", e)` — message AND original cause. A
     bare `throw new RuntimeException(e)` violates this. Note: `CliException` is ONLY for expected,
     user-facing CLI aborts (defined exit code, stacktrace suppressed) — do NOT use it to wrap
     unexpected technical failures like `IOException`.
4. **Testing guidelines** (see `AGENTS.md`): AssertJ only, test classes extend
   `org.assertj.core.api.Assertions` (no JUnit static imports), symlink tests call
   `WindowsSymlinkTestHelper.assumeSymlinksSupported()`. For bug fixes, expect a test that first
   reproduces the bug (TDD).
5. **Definition of Done** (`documentation/contributing/DoD.adoc`) and the PR checklist
   (`.github/PULL_REQUEST_TEMPLATE.md`).
6. **Risk & blast radius.** Flag behavioral changes with a wide side-effect surface (e.g. a mock
   used by a base test context that flips from no-op to active), backward-compatibility risks, and
   anything that passed CI but changes semantics.

**Verify before you assert — do not rely on memory**

- Before stating that something violates a convention, CONFIRM it: read the relevant section of the
  conventions doc AND grep how the real production code actually does it
  (`git grep` the pattern under `cli/src/main/java`). Cite concrete file:line evidence.
- If your own recommendation turns out to be wrong under scrutiny, CORRECT IT. Pushing back with
  evidence is good; defending an unverified claim is not.

**Post the review to GitHub**

Categorize every finding as **Must-fix (blocking)**, **Should-address**, or **Minor**, then post a
single review via the GitHub reviews API
(`POST repos/<owner>/<repo>/pulls/<pr>/reviews` with a `body` and a `comments` array; anchor each
inline comment with `path` + `line` + `side:"RIGHT"` at the PR head commit).

- **Propose fixes when easily possible.** For a clean, small change (typically a single line), embed
  a GitHub ```suggestion block so the author can commit it in one click. When the fix spans multiple
  methods or is non-trivial, give inline GUIDANCE ONLY — do not force an awkward suggestion, and do
  NOT push commits to the contributor's branch.
- **Make comments educational.** When flagging a convention violation, link the specific doc and
  section (e.g. `coding-conventions.adoc` → "Catching and handling Exceptions") so the receiver
  learns the rule and why. Cover EVERY occurrence, not just the first — one suggestion per site.
- **Choose the review event by severity:** use `REQUEST_CHANGES` when any Must-fix item exists;
  use `COMMENT` when findings are only non-blocking. NEVER auto-`APPROVE` on the author's behalf.
- **Respect the strict no-AI-attribution rules in `AGENTS.md`:** the review body and comments must
  not mention AI tooling or add AI attribution of any kind.

**After posting**

1. VERIFY the inline comments actually attached to the diff
   (`gh api repos/<owner>/<repo>/pulls/<pr>/comments`) — the API silently drops comments whose line
   does not map to the diff. Re-anchor and re-post any that were dropped.
2. Report back to the user: the review URL, the chosen event, and a short summary of the Must-fix /
   Should-address / Minor findings.

---
description: Report all open non-draft pull requests to the chat, grouped by release milestone and ordered by what YOU (the current user) need to act on
model: opus
---

The user asked for a status overview of all open, non-draft pull requests of the `devonfw/IDEasy`
repository — read-only, no branch changes — presented so it is immediately actionable for the person
running this command. (To actually sync branches and resolve changelog conflicts, use `/sync-prs`.)

**Identify "me" and the current release**

1. Resolve the current user: `gh api user --jq .login` → call it `ME`.
2. Resolve the current release milestone: `gh api repos/devonfw/IDEasy/milestones --jq 'sort_by(.due_on) | .[] | select(.state=="open") | .title'`
   and take the earliest-due open milestone; cross-check it against the FIRST `== <version>` heading
   in `CHANGELOG.adoc`. Call it `CURRENT_MILESTONE`.

**Gather data cheaply — funnel first, do NOT read every comment**

Reading comments for all PRs is wasteful. Instead let GitHub filter server-side, then deep-read only
the handful of PRs that can actually require my action.

1. **Base table data in ONE call** (no comment reads) — enough to fill every table row:
   `gh pr list --state open --draft=false --limit 200 --json number,title,url,milestone,mergeable,reviewDecision,statusCheckRollup,author`
2. **Build my-actionable candidate sets with server-side search** — each is a single call returning
   just PR numbers; GitHub does the filtering:
   - review requested of me (A2) → `gh search prs --repo devonfw/IDEasy --state open --review-requested @me --json number`
   - already reviewed by me (A1 candidates) → `gh search prs --repo devonfw/IDEasy --state open --reviewed-by @me --json number`
   - mentions me (A3 candidates) → `gh search prs --repo devonfw/IDEasy --state open --mentions @me --json number`
   (Equivalent raw form: `gh api -X GET /search/issues -f q='repo:devonfw/IDEasy is:pr is:open mentions:ME'`.)
3. **Deep-read ONLY the PRs returned by those searches** — that small union is the only set that can
   need my action. For every other open PR, the base-list fields already fill the table; skip comment
   reads entirely.
   - **A3:** for each `mentions:@me` PR, fetch its comments once and confirm I have NOT commented
     after the latest mention (if I already replied, it is not A3). This bounds comment reading to
     only PRs that actually mention me — usually a few, not all.
   - **A1:** for each `reviewed-by:@me` PR, fetch my reviews + GraphQL `reviewThreads { isResolved }`
     (or commits pushed after my review's `submittedAt`) to decide whether a re-review is due.
   - **A2:** a `review-requested:@me` PR with no review submitted is A2 directly.
   - Use `gh pr checks <n>` only if `statusCheckRollup` from the base list is insufficient.

   (`gh api notifications --jq '.[] | select(.reason=="mention")'` is an even cheaper hint for FRESH
   mentions, but it misses already-read ones — treat it as a supplement, not the source of truth.)

**Classify each PR by what I must do (highest priority first)**

- **🔴 A1 — Re-review needed (my change request resolved):** I have a prior review with state
  `CHANGES_REQUESTED`, and since then the author pushed new commits and/or all my review threads are
  now resolved (check GraphQL `reviewThreads.isResolved`; fallback heuristic: commits pushed after my
  review's `submittedAt`). The ball is back in my court — this is the top priority.
- **🔴 A2 — Review requested, none given:** `ME` appears in `reviewRequests` and I have submitted no
  review yet.
- **🟠 A3 — Unanswered mention:** I was `@ME`-mentioned in a PR comment or review thread and have not
  commented after that mention. Only the `mentions:@me` candidates from the funnel qualify — do not
  scan comments of PRs outside that set.
- **🟡 B — Waiting on others:** my `CHANGES_REQUESTED` is still unresolved (waiting on the author), or
  the PR is approved and just waiting to merge / on other reviewers. No action from me right now.
- **⚪ C — No action for me:** everything else open.

**Render the report to the chat**

Two sections, in this order — **latest release milestone on top, then the rest**:

### 🎯 Current release milestone (`CURRENT_MILESTONE`)
### 📋 Other open PRs

Within EACH section, sort rows by the priority class above (A1 → A2 → A3 → B → C). Use one table per
section:

| Pull request | My action | Review | Checks | Conflicts |
|---|---|---|---|---|
| [#<n> <title>](<url>) | 🔴 Re-review (my changes resolved) / 🔴 Review requested / 🟠 Reply to mention / 🟡 Waiting on author / ⚪ — | approved / changes requested / ongoing / no | ✅ passing / ❌ N failing / ⏳ pending | no / yes |

- **Review** column maps `reviewDecision` + reviews: `APPROVED` → approved; `CHANGES_REQUESTED` →
  changes requested; reviews/comments exist but undecided → ongoing; no reviews → no.
- **Conflicts** = yes if `mergeable: CONFLICTING`, else no.

**Lead with my action items.** Above the tables, add a short "**👉 Needs your attention**" list that
names just the PRs in classes A1–A3 (the ones where I am the blocker), most urgent first, each as a
clickable link with a one-line reason — so the actionable items are visible without scanning tables.

Read-only: do not update branches, resolve conflicts, push, review, approve, or merge anything.

---
agent: agent
model: Claude Opus 4.8
---
The user asked for a status overview of all open, non-draft pull requests of the `devonfw/IDEasy` repository — read-only, no branch changes — presented so it is immediately actionable for the person running this prompt. (To actually sync branches and resolve changelog conflicts, use `sync-prs`.)

The pure PR view is not enough. The team's actual workflow lives on the **IDEasy board (project #5)**, documented in `documentation/contributing/project-board.adoc`. Read that file (it is the source of truth for column semantics) and use the board to prioritize the report.

**Board model (from `project-board.adoc`) — what each signal means**

* columns (`Status` field): `🆕 New` → `Research` → `Refinement` → `🏗 In progress` → `Team Review` → `👀 In review` → `✅ Done`
* `Team Review` = PR under **peer review** by a team member. `👀 In review` = PR awaiting **final review** by the Project Owner (currently `hohwille`) or a senior team member
* **only PRs are allowed in the review columns**; issues stay in `In progress` until their PR merges. So an open non-draft **PR** outside `Team Review` / `In review` / `Research` is misplaced
* **assignees encode reviewer ownership:** in the review columns both the reviewer and the author are assignees. Therefore `reviewer = assignees − author`. An empty reviewer set means **nobody owns the review** — a first-class finding, not a detail
* `Priority` (`🌋 Urgent` > `🏔 High` > `🏕 Medium` > `🏝 Low`) and `Size` (`🐋 X-Large` > `🦑 Large` > `🐂 Medium` > `🐇 Small` > `🦔 Tiny`) drive ordering: high priority + small size first

**Identify "me" and the current release**

1. resolve the current user: `gh api user --jq .login` → call it `ME`
2. resolve the current release milestone: `gh api "repos/devonfw/IDEasy/milestones?state=all&per_page=100"`, take the **earliest-due open** milestone → `CURRENT_MILESTONE`. Cross-check it against two things read from the **LIVE remote, not the local working copy**:
   * `gh api repos/devonfw/IDEasy/contents/.mvn/maven.config?ref=main --jq .content | base64 -d` → `-Drevision=<VERSION>-SNAPSHOT`, the authoritative next open release per `documentation/contributing/DoD.adoc`
   * `gh api repos/devonfw/IDEasy/contents/CHANGELOG.adoc?ref=main --jq .content | base64 -d` → its first `== <version>` heading

   **Never read `CHANGELOG.adoc` or `.mvn/maven.config` from the local checkout for this.** A local `main` that is behind will show an already-released version on top and produce a bogus "shipped release with no changelog section" finding. `git fetch` can also fail silently while `gh` still works, so a clean `git status` proves nothing about freshness. Only report a changelog/milestone mismatch when the **live** files disagree, and say which sources you compared

**Gather data cheaply — funnel first, do NOT read every comment**

Reading comments for all PRs is wasteful. Let GitHub filter server-side, then deep-read only the handful of PRs that can actually require my action.

1. **base table data in ONE call:** `gh pr list --repo devonfw/IDEasy --state open --draft=false --limit 200 --json number,title,url,milestone,mergeable,reviewDecision,statusCheckRollup,author,reviewRequests`
2. **board data** — paginate project #5 items (~1300 items, 100/page, so ~14 pages; loop on `pageInfo.hasNextPage` / `endCursor`). One GraphQL query per page:

   ```graphql
   query($cur: String) {
     organization(login: "devonfw") {
       projectV2(number: 5) {
         items(first: 100, after: $cur) {
           pageInfo { hasNextPage endCursor }
           nodes {
             fieldValues(first: 20) {
               nodes {
                 ... on ProjectV2ItemFieldSingleSelectValue {
                   name field { ... on ProjectV2FieldCommon { name } }
                 }
               }
             }
             content {
               __typename
               ... on PullRequest {
                 number title state isDraft url
                 repository { nameWithOwner }
                 assignees(first: 10) { nodes { login } }
               }
             }
           }
         }
       }
     }
   }
   ```

   Keep only `__typename == "PullRequest"`, `state == "OPEN"`, `repository == "devonfw/IDEasy"`. Extract `Status`, `Priority`, `Size` from `fieldValues`, plus `assignees`. Join to the PR list on `number`; note any open non-draft PR **missing from the board**
3. **reviewer counts in ONE call** (for the Review column) — GraphQL `pullRequests(states:OPEN, first:100){ nodes { number isDraft reviewDecision reviews(first:1){totalCount} } }`
4. **build my-actionable candidate sets with server-side search** — one call each, PR numbers only:
   * review requested of me (A2) → `gh search prs --repo devonfw/IDEasy --state open --review-requested '@me' --json number`
   * already reviewed by me (A1 candidates) → `gh search prs --repo devonfw/IDEasy --state open --reviewed-by '@me' --json number`
   * mentions me (A3 candidates) → `gh search prs --repo devonfw/IDEasy --state open --mentions '@me' --json number`
5. **deep-read ONLY the PRs from step 4 plus PRs where `ME` is a board assignee.** That union is the only set that can need my action; for every other PR the base + board fields fill the table
   * **A1:** for each `reviewed-by:@me` PR, fetch my reviews and GraphQL `reviewThreads { isResolved isOutdated }` (fallback: commits pushed after my review's `submittedAt`) to decide whether a re-review is due. Note whether my review was `CHANGES_REQUESTED` or only `COMMENTED` — an unresolved `COMMENTED` thread with newer commits is A4, not A1
   * **A3:** fetch comments once and confirm I have NOT commented after the latest mention
   * **A2:** a `review-requested:@me` PR with no review submitted is A2 directly

   Tooling notes (Windows/PowerShell): quote `'@me'` — bare `@me` is parsed as a splat. Prefer bash for `--jq` expressions containing double quotes. Strip non-ASCII when printing board field names to the console (`cp1252` cannot encode the column emoji)

**Classify each PR by what I must do (highest priority first)**

* **🔴 A0 — board says the review is mine:** `ME` is a board assignee (and not the author) on a PR in `Team Review` or `👀 In review`. This is the team's own ownership signal and outranks everything — weight by `Priority` (Urgent → Low) then ascending `Size`
* **🔴 A1 — re-review needed (my change request resolved):** I have a prior `CHANGES_REQUESTED` review, and since then the author pushed new commits and/or all my review threads are resolved
* **🔴 A2 — review requested, none given:** `ME` in `reviewRequests`, no review submitted by me
* **🟠 A3 — unanswered mention:** I was `@ME`-mentioned and have not commented after that mention
* **🟠 A4 — my review comment left hanging:** I reviewed with `COMMENTED`, at least one of my review threads is still unresolved, and the author has pushed commits since. Not a formal blocker, but the thread is mine to close
* **🟡 B — waiting on others:** my `CHANGES_REQUESTED` still unresolved (waiting on the author), or the PR is approved and just waiting to merge / on other reviewers
* **🧹 H — board hygiene, mine as a maintainer:** no reviewer assigned; PR parked in a non-review column; PR missing from the board; missing `Priority`/`Size`. No code review needed, but the PR is invisible or stalled until someone fixes the board
* **⚪ C — no action for me:** everything else open

**Render the report to the chat**

1. **header line:** `ME`, `CURRENT_MILESTONE` + due date, board link (`https://github.com/orgs/devonfw/projects/5/views/6`), count of open non-draft PRs, and how many are tracked on the board (call out any that are not)
2. **🚦 Board pipeline** — a small table of column → PR count → meaning, flagging columns that should not contain PRs. Name the bottleneck explicitly (e.g. `In review` depth vs. `Team Review` depth)
3. **👉 Needs your attention** — the A0–A4 PRs, most urgent first, each a clickable link with a one-line reason. State plainly if the board shows I am assignee on nothing. Close with a pointer to the hygiene section if it is non-empty
4. **🎯 Current release milestone (`CURRENT_MILESTONE`)** — table, board-ordered:

   | Pull request | Column | Prio / Size | Reviewer (board) | My action | Review | Checks | Conflicts |
   |---|---|---|---|---|---|---|---|

   Note how many of the open PRs carry the milestone and how many are merge-ready, relative to the milestone due date
5. **⚠️ Off-pipeline** — PRs in `🆕 New` / `🏗 In progress` / `Research` or absent from the board. Call out anything approved-but-parked
6. **🔍 `👀 In review`** and **👥 `Team Review`** — one table each (skip PRs already shown in the milestone table, with a one-line note saying so):

   | Pull request | Reviewer (board) | Review | Checks | Conflicts |
   |---|---|---|---|---|

   Sort each by class (A0 → A1 → A2 → A3 → A4 → B → C), then `Priority`, then ascending `Size`, then PR number descending. Put approved PRs at the top of their class — they are one click from merged
7. **🧹 Board hygiene gaps** — bullets: PRs with no reviewer (⚠️ mark them inline in the tables too), PRs missing `Priority`/`Size` (give the ratio), PRs in wrong columns, PRs off the board, reviewer load per person (`assignee − author`, so over/under-loaded reviewers are visible), conflict and failing-check totals with a pointer to `sync-prs`, and the changelog/milestone cross-check flag

Column value mappings:

* **Column** = board `Status`, emoji included
* **Prio / Size** = board `Priority` / `Size`; render `—` when unset
* **Reviewer (board)** = assignees minus author; render `⚠️ **none**` when empty
* **Review** maps `reviewDecision` + review count: `APPROVED` → approved; `CHANGES_REQUESTED` → changes requested; reviews or comments exist but undecided → ongoing; no reviews → no
* **Checks** = ✅ passing / ❌ N failing (name the failing check) / ⏳ pending (name it)
* **Conflicts** = yes if `mergeable: CONFLICTING`, no if `MERGEABLE`, `unknown` if `UNKNOWN` (GitHub had not computed mergeability yet — not a clean branch)

Read-only: do not update branches, resolve conflicts, push, review, approve, merge, or change any board field.

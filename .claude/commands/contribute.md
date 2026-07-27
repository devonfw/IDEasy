---
description: Start working on a GitHub issue end-to-end (assign, branch, solve, finalize PR)
argument-hint: <issue-number>
model: sonnet
---

Let's start working on issue #$1. For that, I have cloned my GitHub fork as the `origin` git
remote as well as the repository to issue the pull request to as the `upstream` git remote.

You need to:

**Clarify state of work**

1. Check assignment of the issue.
   1.1. If already assigned, STOP, ASK THE USER whether they want to assign the issue to
        themselves, AND RESUME.
   1.2. If not assigned, assign the issue to themselves. If you don't know the user name, STOP,
        ASK THE USER for their GitHub user name, AND RESUME.
2. Set the issue status in the "IDEasy board" project to "In progress" on GitHub. If you cannot
   do that, STOP, ASK THE USER to do it manually, AND RESUME.

**Tasks**

1. Sync `origin` main branch against `upstream` main branch and push the new fast-forwarded
   commits. The commits on `origin` main should be exactly the same as on `upstream` main.
2. Create a new branch starting from main branch to prepare a new pull request for the issue.
3. Understand the coding conventions (`documentation/contributing/coding-conventions.adoc`) — you
   need to comply with them!
4. Research the issue on GitHub including all referenced issues and comments, and plan your
   solution carefully.
5. Make sure you tested your solution. Try to reuse available test classes instead of creating new
   ones, and make sure you didn't create duplicates.
6. ENSURE the Definition of Done is met (`documentation/contributing/DoD.adoc`) and potentially add
   tasks to your task list if needed.
7. ASK for human review and potential improvements / iterations to be done.

When asked to continue finalization of the issue:

1. Commit (short and crisp commit message) the code to the git remote `origin` on the newly
   created branch. Make sure you comply with the coding conventions in the commit message, and
   follow the strict AI-attribution prohibitions in AGENTS.md (no AI co-author trailers, no AI
   remarks in commit messages or PRs).
2. Create a new pull request to the `upstream` repository following `.github/PULL_REQUEST_TEMPLATE.md`.
3. Mark all tasks already done in the new pull request's description. If you can resolve some tasks
   on your own, take proper action. Decide on your own whether you need the task lists for
   commandlets or not.
4. Assign the same labels to the pull request as issue #$1 is labeled with. If the issue is of type
   "Bug", add the label "bugfix" to the pull request as well.

ASSURE TO ALWAYS ASK FOR CONFIRMATION BEFORE STARTING FINALIZATION!
ASSURE TO REMEMBER THE TASKS TO BE DONE FOR THE ISSUE UNTIL FINALIZATION EVEN IF INTERRUPTED BY
ADDITIONAL QUESTIONS DURING HUMAN REVIEW!

---
description: Re-create a coding-agent PR under human authorship to satisfy the CLA, then close the original
argument-hint: <pr-number> <issue-number>
model: sonnet
---

Let's get pull request #$1 fixed. It was created some time ago by a coding agent, but unfortunately
we cannot accept Copilot-authored commits as the CLA assistant issues are not resolved.

To overcome this, we need to:

1. Sync `origin` main branch against `upstream` main branch and push the new fast-forwarded
   commits. The commits on `origin` main should be exactly the same as on `upstream` main.
2. Create a new branch for a new pull request to the original issue #$2 which should have been
   solved.
3. Pull the recent code from the existing pull request #$1 into that local branch and potentially
   fix git conflicts if needed. Please ask if you need my help for that. Make sure that you pull the
   changes in a way that the original authorship is NOT retained.
4. Afterwards, check that the code complies with the documented coding conventions
   (`documentation/contributing/coding-conventions.adoc`).
5. Check that no tests are failing.
6. Commit (short and crisp commit message) the code to the git remote `origin` on the newly created
   branch. Follow the strict AI-attribution prohibitions in AGENTS.md (no AI co-author trailers, no
   AI remarks in commit messages or PRs) — this is the whole point of the takeover.
7. Create a new pull request to devonfw/IDEasy following the pull request template
   (`.github/PULL_REQUEST_TEMPLATE.md`).
8. Mark all tasks already done in the new pull request's description.
9. Close the former existing pull request #$1 in favor of the new one, with an appropriate comment.

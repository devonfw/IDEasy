---
name: contribute
description: Start working on a GitHub issue end-to-end (assign, branch, solve, finalize PR). Use when asked to work on, implement or fix an IDEasy issue.
argument-hint: <issue-number>
model: sonnet
---

Let's start working on issue #$0. Remotes follow the convention in `AGENTS.md`: `origin` is the
contributor's fork, `upstream` is `devonfw/IDEasy`.

You need to:

**Clarify state of work**

1. Check assignment of the issue.
   1.1. If already assigned, STOP, ASK THE USER whether they want to assign the issue to
        themselves, AND RESUME.
   1.2. If not assigned, assign the issue to themselves. If you don't know the user name, STOP,
        ASK THE USER for their GitHub user name, AND RESUME.
2. Set the issue status in the "IDEasy board" project to "In progress" on GitHub. If you cannot
   do that, STOP, ASK THE USER to do it manually, AND RESUME.

**Understand the issue**

1. Research the issue on GitHub including all referenced issues, their comments and open PRs that
   touch the same area (`gh pr list --search "<area>"`). Comments often hold decisions such as
   feature toggles or scope cuts. Open PRs on the same files mean merge conflicts: keep the change
   minimal there.
2. Verify the premise of the issue in the real source before coding. The analysis in an issue can
   be wrong or incomplete. Check the tool's shipped code and behaviour (oldest and newest supported
   version), other call sites of the method you change, other operating systems, editions, feature
   toggles and migration of existing users. Write down the edge cases and decide each one.
3. A new tool needs two issues and two PRs: one for the UrlUpdater, one for the commandlet (except
   npm, Maven and pip based tools). See
   `documentation/contributing/commandlet-and-url-updater.adoc`.
4. Understand the coding conventions (`documentation/contributing/coding-conventions.adoc`) and
   plan your solution carefully.

**Implement**

1. Fetch `upstream` and create a git worktree with a new branch from `upstream/main`, so the
   developer's current checkout stays untouched:
   `git worktree add ../IDEasy-$0 -b feature/$0-<slug> upstream/main`
   Spotless cannot read a worktree: run `mvn spotless:apply` in a regular clone on the same branch,
   or use a separate clone instead of a worktree (until #2665 is done).
2. Work test first (see `AGENTS.md`). Try to reuse available test classes instead of creating new
   ones, and make sure you didn't create duplicates.
3. Add the `CHANGELOG.adoc` entry unless the issue is labelled `internal` (see `AGENTS.md`).
4. Run the test tiers of the development harness in `AGENTS.md`, including the full test suite
   of the changed module before asking for review.
5. Test end to end with the classes of the branch, not with the installed `ide` command (that is
   the released IDEasy). Build the classpath once
   (`mvn -q -o dependency:build-classpath -Dmdep.outputFile=<tmp>/cp.txt` in `cli`) and run
   `java -cp "cli/target/classes<path-separator><classpath>" com.devonfw.tools.ide.cli.Ideasy ...`.
   Use a throwaway project (e.g. `$IDE_ROOT/test-$0/`) so the developer's real projects, settings
   and running tools are not touched. Back up `~/.ide/ide.properties` if the test writes to it, and
   remove junctions or symlinks inside the throwaway project before deleting it, so the delete
   cannot follow them into the shared software repository.
6. ENSURE the Definition of Done is met (`documentation/contributing/DoD.adoc`) and potentially add
   tasks to your task list if needed.
7. ASK for human review and potential improvements / iterations to be done.

When asked to continue finalization of the issue:

1. Commit (short and crisp commit message) the code to the git remote `origin` on the newly
   created branch. Make sure you comply with the coding conventions in the commit message, and
   follow the strict AI-attribution prohibitions in AGENTS.md (no AI co-author trailers, no AI
   remarks in commit messages or PRs).
2. Create a new pull request to the `upstream` repository as described in the `create-pr` skill.
3. Mark all tasks already done in the new pull request's description. If you can resolve some tasks
   on your own, take proper action. Decide on your own whether you need the task lists for
   commandlets or not.
4. Assign the same labels to the pull request as issue #$0 is labeled with. If the issue is of type
   "Bug", add the label "bugfix" to the pull request as well.

ASSURE TO ALWAYS ASK FOR CONFIRMATION BEFORE STARTING FINALIZATION!
ASSURE TO REMEMBER THE TASKS TO BE DONE FOR THE ISSUE UNTIL FINALIZATION EVEN IF INTERRUPTED BY
ADDITIONAL QUESTIONS DURING HUMAN REVIEW!

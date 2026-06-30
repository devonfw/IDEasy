---
agent: agent
model: Claude Haiku 4.5
---
The user asked to finalize the work on an issue with local changes and potentially already a setup feature branch.

**Clarify state of work**

* clarify which issue is intended to be worked on. If unclear: STOP, ASK THE USER to specify the issue number, AND RESUME
* check state of local repository
  * whether uncommited changes exist. If so: STOP, ASK THE USER whether these are intended to be included in the pull request, AND RESUME
  * which remotes are configured
  * which branch is currently checked out. If it's unequal main branch, STOP, ASK the USER whether this branch is intended to be used for the pull request, AND RESUME

**Verify precondictions and prepare PR**

1. stash all local changes if existing
2. checkout main branch
3. IF the repository origin is a fork of devonfw/ideasy, THEN pull from upstream main and push the new fast forwarded commits to origin main branch. The commits on origin main should be exactly the same as on upstream main.
4. IF the repository origin is NOT a fork of devonfw/ideasy, THEN make sure local main branch is up to date with origin main branch by pulling latest changes
5. IF no feature branch for the issue exists yet, then create a new branch starting from main branch to prepare a new pull request for the issue under feature/ with a name starting with the issue number to be fixed
6. apply stashed changes if existing, ASK for support if conflicts occur and they cannot be resolved automatically

**Tasks**

1. understand the coding conventions (coding-conventions.adoc) documented, YOU NEED TO comply to!
2. make sure you tested your solution. Eventually, try to reuse test classes available intead of creating new one and MAKE SURE your didn't create duplicates
3. ENSURE the Defition of Done is met (=> DoD.adoc) and potentially add tasks to your tasklist if needed
4. IF unstaged files exists AND THE USER AGREES, commit (short and crisp commit message!) the code to the git remote origin to the feature branch. MAKE SURE you are compliant to coding conventions with the commit message!
5. finally create a new pull request to upstream repository by following the IDEasy/.github/PULL_REQUEST_TEMPLATE.md
6. MARK all tasks which have been already done in the new pull request's description. 
7. If you can resolve some of the tasks on your own, take proper actions immediately. If there is no change on any commandlet (to be located at IDEasy/cli/src/main/java/com/devonfw/tools/ide/commandlet), remove the task list for commandlets from the pull request description.

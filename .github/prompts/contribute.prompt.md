---
agent: agent
model: Claude Sonnet 4.5
---
Let's start working on issue ${input:issue}. For that, I have cloned my GitHub fork as origin git remote as well as the repository to issue the pull request to as upstream git remote.
You need to

**Clarify state of work**

1. check assignment of the issue. 
1.1. If already assigned, STOP, ASK THE USER whether they want to assign the issue to themselves, AND RESUME
1.2. If not assigned, assign the issue to themselves. If you don't know the user name, STOP, ASK THE USER for their GitHub user name, AND RESUME
2. set the issue status in the "IDEasy board" project to "In progress" on GitHub. If you cannot do that, STOP, ASK THE USER to do it manually, AND RESUME

**Tasks**

1. Sync origin main branch against upstream main branch and push the new fast forwarded commits. The commits on origin main should be exactly the same as on upstream main.
2. create a new branch starting from main branch to prepare a new pull request for the issue.
3. understand the coding conventions (documentation/coding-conventions.adoc) documented, you need to comply to!
4. research the issue on GitHub including all referenced issues and comments in text and plan your solution carefully.
5. make sure you tested your solution. Eventually, try to reuse test classes available intead of creating new one and make sure your didn't create duplicates
6. ENSURE the Defition of Done is met (documentation/DoD.adoc) and potentially add tasks to your tasklist if needed
7. ASK for human review and potential improvements / iterations to be done.

When asked to continue finalization of issue
1. commit (short and crisp commit message) the code to the git remote origin to the newly created branch. Make sure you are compliant to coding conventions with the commit message
2. finally create a new pull request to upstream repository by following the ./github/PULL_REQUEST_TEMPLATE.md
3. we need to mark all tasks which have been already done in the new pull request's description. If you can resolve some of the tasks on your own, please take proper actions. Decide on your own whether you need the task lists for commandlets or not.
4. Assign the same labels to the pull request as the issue ${input:issue} is labeled with. If the issue is of Type "Bug", add the label "bugfix" on the pull request as well.


ASSURE TO ALWAYS ASK FOR CONFIRMATION BEFORE STARTING FINALIZATION!
ASSURE TO REMEMBER THE TASKS TO BE DONE FOR THE ISSUE UNTIL FINALIZATION EVEN IF INTERRUPTED BY ADDITIONAL QUESTIONS DURING HUMAN REVIEW!
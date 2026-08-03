---
agent: agent
model: Claude Sonnet 4.5 (copilot)
---
Let's get pull request ${input:pr} fixed. It has been created some time ago by coding agent, but unfortunately, we cannot accept copilot authored commits as of the cla assistant issues not resolved.
To overcome this, we need to

1. Sync origin main branch against upstream main branch and push the new fast forwarded commits. The commits on origin main should be exactly the same as on upstream main.
2. create a new branch for a new pull request to the original issue ${input:issue} which should have been solved.
3. pull the recent code from the existing pull request ${input:pr} into that local branch and potentially fix git
conflicts if needed. Please ask if you need my help for that. Make sure that you pull the changes in a way, that the original authorship is NOT retained 
4. afterwards, we need to check the code complies to the coding conventions documented
5. we need to check that no test are failing
6. and we need to commit (short and crisp commit message) the code to the git remote origin to the newly created branch
7. finally we need to create a new pull request to devonfw/ideasy by following the pull request template
8. we need to mark all tasks which have been already done in the new pull request's description
9. we need to close the former existing pull request ${input:pr} in favor of the new with an appropriate comment
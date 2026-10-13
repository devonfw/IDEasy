---
name: retest-pr
description: Retest merged IDEasy pull requests against the nightly SNAPSHOT (QA retest after merge into main). Use for requests like "retest PR 2398" or "retest my merged PRs", or before adding the "QA approved" label.
argument-hint: [pr-number]
---

The user asked to retest merged pull requests. (Provided: $0)

The process is defined in `documentation/contributing/retest.adoc`. Read it first, it is the source
of truth and wins over this skill if they differ.

## 1. Check the SNAPSHOT

1. Update the installed IDEasy: `ide upgrade --mode=snapshot`.
2. Check `ide --version`. The timestamp in the version (e.g. `2026.09.002-09_16_07-SNAPSHOT` was built
   on 16 September at 07:xx) must be **after** the PR's `mergedAt`
   (`gh pr view <n> -R devonfw/IDEasy --json mergedAt`). If it is not, the nightly build with the
   change is not out yet: STOP and tell the user.

## 2. Find the PRs

If no PR number was given, list the merged PRs of the user that still lack the label (or use the
Retests board linked in `retest.adoc`, filtered by *Author*):

```bash
gh search prs --repo devonfw/IDEasy --author @me --merged --json number,title,labels \
  -q '.[] | select([.labels[].name] | index("QA approved") | not) | "\(.number) \(.title)"'
```

## 3. Retest each PR

1. `gh pr view <n> -R devonfw/IDEasy` and the linked issue: understand the change and follow its
   *Testing instructions*.
2. Use the **installed** IDEasy (the SNAPSHOT), not a local build.
3. Do not touch the user's real setup:
   - Use a throwaway project per PR, e.g. `$IDE_ROOT/retest-<n>/` with `settings/ide.properties`
     (may be empty), `conf/ide.properties` (variables and feature toggles for the test) and
     `workspaces/main/`. The warning `Settings repository exists but is not a git repository.` is
     expected there. `cd` into it before every `ide` call.
   - Use `ide -b` for non-interactive runs, pipe answers for prompts (`printf 'answer\n' | ide ...`).
   - Back up `~/.ide/ide.properties` before a test that writes to it and restore it afterwards.
   - Delete the throwaway project at the end, after closing tools started from it. Remove
     junctions or symlinks inside it first, so the delete cannot follow them into the shared
     software repository.
4. Steps that need a browser login, a second account or a visual judgement cannot be done by the
   agent. List them for the user as manual steps instead of claiming them as tested.
5. A PR that cannot be tested end to end (documentation, code cleanup, test or workflow change,
   url-updater) gets the observation instead, as described in the NOTE of `retest.adoc`.

## 4. Report, after the user confirmed

Comments, issues and labels are public. Show the drafted comment to the user and get an explicit OK
before running `gh pr comment`, `gh issue create` or `gh pr edit --add-label "QA approved"`.

Comment template (OS, commands, relevant output, clear pass or fail):

```
Retest on SNAPSHOT `<ide --version>` on <OS>: passed / failed (see #<bug>)

1. <testing instruction step>: <command>
   <relevant log lines>
...
Not retested (manual): <steps>, reason
```

- Failure: create a bug issue first and reference it in the comment.
- Add the label `QA approved` once the retest is completed, also when a bug was created and linked
  (see `retest.adoc`).

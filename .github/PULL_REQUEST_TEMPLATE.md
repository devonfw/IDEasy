### This PR fixes `<#issue ID>`.

### Implemented changes:

* Change 1

---

## Checklist for this PR

Make sure everything is checked before merging this PR. For further info please also see
our [DoD](https://github.com/devonfw/IDEasy/blob/main/documentation/DoD.adoc).

- [ ] When running `mvn clean test` locally all tests pass and build is successful
- [ ] PR title is of the form `#«issue-id»: «brief summary»` (e.g. `#921: fixed setup.bat`). If no issue ID exists, title only.
- [ ] PR top-level comment summarizes what has been done and contains link to addressed issue(s)
- [ ] PR and issue(s) have suitable labels
- [ ] Issue is set to `In Progress` and assigned to you *or* there is no issue (might happen for very small PRs)
- [ ] You followed all [coding conventions](https://github.com/devonfw/IDEasy/blob/main/documentation/coding-conventions.adoc)
- [ ] You have added the issue implemented by your PR in [CHANGELOG.adoc](https://github.com/devonfw/IDEasy/blob/main/CHANGELOG.adoc) unless issue is labeled
  with `internal`

### Checklist for tool commandlets

Have you added a new `«tool»` as commandlet? There are the following additional checks:

- [ ] The tool can be installed automatically (during setup via settings) or via the commandlet call
- [ ] The tool is isolated in its IDEasy project, see [Sandbox Principle](https://github.com/devonfw/IDEasy/blob/main/documentation/sandbox.adoc)
- [ ] The new tool is added to the table of tools in [LICENSE.asciidoc](https://github.com/devonfw/IDEasy/blob/main/documentation/LICENSE.adoc)
- [ ] The new commandlet is a [command-wrapper](https://github.com/devonfw/IDEasy/blob/main/documentation/cli.adoc#command-wrapper) for `«tool»`
- [ ] Proper help texts for all supported languages are added [here](https://github.com/devonfw/IDEasy/tree/main/cli/src/main/resources/nls)
- [ ] The new commandlet installs potential dependencies automatically
- [ ] The variables `«TOOL»_VERSION` and `«TOOL»_EDITION` are honored by your commandlet
- [ ] The new commandlet is tested on all platforms it is available for or tested on all platforms that are in scope of the linked issue

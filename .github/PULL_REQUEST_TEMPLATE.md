This PR fixes `<#issue ID>`.

### Implemented changes:

* Change 1

### Checklist for this PR

Make sure everything is checked before merging this PR. For further info please also see
our [DoD](https://github.com/devonfw/ide/blob/master/documentation/DoD.asciidoc).

- [ ] Build is successful and tests pass when running `mvn test`
- [ ] PR title is of the form `#«issue-id»: «brief summary»` (e.g. `#921: fixed setup.bat`). If no issue ID exists, title only.
- [ ] PR top-level comment summarizes what has been done and contains link to addressed issue(s)
- [ ] PR and issue(s) have suitable labels
- [ ] Issue is set to `In Progress` and assigned to you *or* there is no issue (might happen for very small PRs)
- [ ] At least one milestone is assigned (typically done by PO)
- [ ] The feature branch of the PR is up-to-date with the `main` branch
- [ ] You followed all [coding conventions](link:coding-conventions.adoc)
- [ ] You have added the issue implemented by your PR in [CHANGELOG.adoc](https://github.com/devonfw/IDEasy/blob/main/CHANGELOG.adoc)

Have you added a new `«tool»` as commandlet? The following points need to be checked off, too:

<details>
<summary>Checklist for tool commandlets</summary>
- [ ] A new urlUpdater named `«tool»` has been added to a folder in [updater](https://github.com/devonfw/ide/tree/master/url-updater/src/main/java/com/devonfw/tools/ide/url/updater)
- [ ] A new commandlet named `«tool»` has been added to [command](https://github.com/devonfw/ide/tree/master/scripts/src/main/resources/scripts/command)
- [ ] The tool can be installed automatically (during setup via settings) or via the commandlet call
- [ ] The tool will be installed locally to the software folder inside the IDEasy installation (${IDEASY_HOME}) *or* it is an exception to this rule
- [ ] The tool can be configured via files that are placed inside the IDEasy installation (${IDEASY_HOME}) *or* it is an exception to this rule
- [ ] The new commandlet is documented as `«tool»`.asciidoc in the [documentation](https://github.com/devonfw/ide/tree/master/documentation) folder
- [ ] The new commandlet is added and linked in [cli.asciidoc](https://github.com/devonfw/ide/blob/master/documentation/cli.asciidoc#commandlet-overview)
- [ ] The new commandlet is added and linked in [scripts.asciidoc](https://github.com/devonfw/ide/blob/master/documentation/scripts.asciidoc)
- [ ] The new commandlet is included to [devonfw-ide-usage.asciidoc](https://github.com/devonfw/ide/blob/master/documentation/devonfw-ide-usage.asciidoc)
- [ ] The new tool is added to the table of tools in [LICENSE.asciidoc](https://github.com/devonfw/ide/blob/master/documentation/LICENSE.asciidoc#license)
- [ ] The new commandlet is a [command-wrapper](https://github.com/devonfw/ide/blob/master/documentation/cli.asciidoc#command-wrapper) for `«tool»`
- [ ] The new commandlet installs potential dependencies automatically
- [ ] The new commandlet defines the variable `TOOL_VERSION_COMMAND` before sourcing the functions
- [ ] Calling `ide get-version «tool»", `ide list-versions «tool»" and `ide set-version «tool»" works *or* the tool does not provide any such setup
- [ ] The variable `«TOOL»_VERSION` is honored by your commandlet??? 
- [ ] The new commandlet is tested on all platforms it is available for or tested on all platforms that are in scope of the linked issue
</details>

Have you added, changed or deleted a *function* (in `functions` or `environment-project`)?

- [ ] [functions.asciidoc](https://github.com/devonfw/ide/blob/master/documentation/functions.asciidoc) has been updated

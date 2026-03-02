# Project Overview

This project is a software orchestration and setup tool called IDEasy, which enables automated provisioning and configuration of software packages on local hardware. 
To keep multiple configurations in sync, there a settings folder can be distributed via git.
The standard configuration of the IDEasy is maintained in the repository https://github.com/devonfw/ide-settings.

## Folder Structure

- `/cli`: Contains the source code for the IDEasy cli
- `/documentation`: Contains the source code for Asciidoc documentation
- `/gui`: Contains the source code of the UI of IDEasy
- `/url-updater`: Contains the source code of the URL updater to update all files in the respository https://github.com/devonfw/ide-urls with fresh URL crawled from the web for all supported software packages
- `windows-installer`: Contains the source code to compile an MSI installer for the IDEasy

## Test Execution

- All tests can be executed by `mvn clean test`
- Run `mvn test` in the root folder RARELY as it triggers unnecessary long-running documentation generation (ide-doc module)
- Execute specific test classes by using your IDE or the command line `mvn -Dtest=ClassName test` in the module folder of interest, e.g. the `cli` folder respectively `ide-cli` maven module folder. 
- All integration tests can be executed by executing the script `cli/src/test/all-tests.sh`

## Commit Messages

-  Always commit your changes in small logical units associated with an issue (see above section) using the commit message format `#«issue-id»: «describe your change»`

## Coding Conventions

- Please ALWAYS review and refactor your code after implementation to comply to the coding standards documented here: https://github.com/devonfw/IDEasy/blob/main/documentation/coding-conventions.adoc

## Pull-Request Guidelines

- Please ALWAYS add the Checklist from https://github.com/devonfw/IDEasy/blob/main/.github/PULL_REQUEST_TEMPLATE.md as acceptance criteria for all pull requests

## Testing Guidelines

- Always use AssertJ for assertions in all Java tests.
- Extend your test classes from org.assertj.core.api.Assertions to avoid static imports.
- Do not use JUnit static imports for assertions.
- When implementing tests that create symbolic links (via FileAccess.symlink or Files.createSymbolicLink), always call `WindowsSymlinkTestHelper.assumeSymlinksSupported()` at the beginning of the test method. This ensures the test is gracefully skipped on Windows systems without proper symlink permissions (admin or Developer Mode), providing a better experience for new contributors.

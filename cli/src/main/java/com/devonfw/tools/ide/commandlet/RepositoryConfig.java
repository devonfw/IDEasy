package com.devonfw.tools.ide.commandlet;

/**
 * Represents the configuration of a repository to be used by the {@link RepositoryCommandlet}.
 *
 * @param path Path into which the project is cloned. This path is relative to the workspace.
 * @param workingSets The working sets associated with the repository.
 * @param workspace Workspace to use for checkout and import. Default is main.
 * @param gitUrl Git URL to use for cloning the project.
 * @param gitBranch Git branch to checkout. Git default branch is default.
 * @param buildPath The build path for the repository.
 * @param buildCmd The command to invoke to build the repository after clone or pull. If omitted no build is triggered.
 * @param eclipse Desired action for eclipse IDE. If equals to "import" all modules will be imported into eclipse.
 * @param active {@code true} to setup the repository during setup, {@code false} to skip.
 */
public record RepositoryConfig(
    String path,
    String workingSets,
    String workspace,
    String gitUrl,
    String gitBranch,
    String buildPath,
    String buildCmd,
    String eclipse,
    boolean active) {
}

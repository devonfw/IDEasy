package com.devonfw.tools.ide.git.repository;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.GitUrl;

/**
 * Represents the configuration of a repository to be used by the {@link RepositoryCommandlet}.
 *
 * @param id Identifier derived from the filename without extension.
 * @param path Path into which the project is cloned. This path is relative to the workspace.
 * @param workingSets The working sets associated with the repository (for Eclipse import).
 * @param workspaces Workspaces to use for checkout and import. Supports comma-separated values. Default is main.
 * @param gitUrl Git URL to use for cloning the project.
 * @param gitBranch Git branch to checkout. Git default branch is default.
 * @param buildPath The build path for the repository.
 * @param buildCmd The command to invoke to build the repository after clone or pull. If omitted no build is triggered.
 * @param imports list of IDEs where the repository will be imported to.
 * @param active {@code true} to setup the repository during setup, {@code false} to skip.
 */
public record RepositoryConfig(
    String id,
    String path,
    String workingSets,
    List<String> workspaces,
    String gitUrl,
    String gitBranch,
    String buildPath,
    String buildCmd,
    Set<String> imports,
    List<RepositoryLink> links,
    boolean active) {

  /** Wildcard to match all workspaces. */
  public static final String WORKSPACE_NAME_ALL = "*";

  public RepositoryConfig {
    if (workspaces == null || workspaces.isEmpty()) {
      throw new IllegalArgumentException("workspaces cannot be empty");
    }
  }

  /**
   * @return the {@link GitUrl} from {@link #gitUrl()} and {@link #gitBranch()}.
   */
  public GitUrl asGitUrl() {

    if (this.gitUrl == null) {
      return null;
    }
    return new GitUrl(this.gitUrl, this.gitBranch);
  }

  /**
   * @param filePath the {@link Path} to the {@link Properties} to load.
   * @param context the {@link IdeContext}.
   * @return the parsed {@link RepositoryConfig}.
   */
  public static RepositoryConfig loadProperties(Path filePath, IdeContext context) {

    RepositoryProperties properties = new RepositoryProperties(filePath, context);
    String filename = filePath.getFileName().toString();
    final String id;
    if (filename.endsWith(IdeContext.EXT_PROPERTIES)) {
      id = filename.substring(0, filename.length() - IdeContext.EXT_PROPERTIES.length());
    } else {
      id = filename;
    }
    RepositoryConfig config = new RepositoryConfig(id, properties.getPath(), properties.getWorkingSets(), properties.getWorkspaces(), properties.getGitUrl(),
        properties.getGitBranch(), properties.getBuildPath(), properties.getBuildCmd(), properties.getImports(), properties.getLinks(), properties.isActive());
    if (properties.isInvalid()) {
      return null;
    }
    return config;
  }

}

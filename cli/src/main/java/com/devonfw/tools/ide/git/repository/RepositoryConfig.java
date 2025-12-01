package com.devonfw.tools.ide.git.repository;

import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.GitUrl;

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
 * @param imports list of IDEs where the repository will be imported to.
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
    Set<String> imports,
    boolean active) {

  /** {@link RepositoryProperties#getProperty(String) Property name} for {@link #path()}. */
  public static final String PROPERTY_PATH = "path";

  /** {@link RepositoryProperties#getProperty(String) Property name} for {@link #workingSets()}. */
  public static final String PROPERTY_WORKING_SETS = "workingsets";

  /** {@link RepositoryProperties#getProperty(String) Property name} for {@link #workspace()}. */
  public static final String PROPERTY_WORKSPACE = "workspace";

  /** {@link RepositoryProperties#getProperty(String) Property name} for {@link #gitUrl()}. */
  public static final String PROPERTY_GIT_URL = "git_url";

  /** {@link RepositoryProperties#getProperty(String) Property name} for {@link #buildPath()}. */
  public static final String PROPERTY_BUILD_PATH = "build_path";

  /** {@link RepositoryProperties#getProperty(String) Property name} for {@link #buildCmd()}. */
  public static final String PROPERTY_BUILD_CMD = "build_cmd";

  /** {@link RepositoryProperties#getProperty(String) Property name} for {@link #active()}. */
  public static final String PROPERTY_ACTIVE = "active";

  /** {@link RepositoryProperties#getProperty(String) Property name} for {@link #gitBranch()}. */
  public static final String PROPERTY_GIT_BRANCH = "git_branch";

  /** {@link RepositoryProperties#getProperty(String) Property name} for {@link #imports()}. */
  public static final String PROPERTY_IMPORT = "import";

  /** Legacy {@link RepositoryProperties#getProperty(String) property name} for {@link #imports()}. */
  public static final String PROPERTY_ECLIPSE = "eclipse";

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

    Set<String> importsSet = properties.getImports();

    return new RepositoryConfig(properties.getProperty(PROPERTY_PATH), properties.getProperty(PROPERTY_WORKING_SETS),
        properties.getProperty(PROPERTY_WORKSPACE), properties.getProperty(PROPERTY_GIT_URL, true), properties.getProperty(PROPERTY_GIT_BRANCH),
        properties.getProperty(PROPERTY_BUILD_PATH), properties.getProperty(PROPERTY_BUILD_CMD), importsSet,
        parseBoolean(properties.getProperty(PROPERTY_ACTIVE)));
  }

  private static boolean parseBoolean(String value) {

    if (value == null) {
      return true;
    }
    return "true".equals(value.trim());
  }
}

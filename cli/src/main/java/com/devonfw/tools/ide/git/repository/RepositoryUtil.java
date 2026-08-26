package com.devonfw.tools.ide.git.repository;

import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;

/**
 * Utility class for IDEasy settings/code repositories.
 */
public class RepositoryUtil {

  /**
   * Checks whether the given git repository is a settings repository, a combined settings and code repository, or a typical code repository. Combined code and
   * settings repository is detected by checking whether IDE_HOME/workspaces/main/[gitProjectName]/settings exists and is a valid settings repository.
   *
   * @param repositoryPath the path of the repository to be checked.
   * @param gitProjectName the name of the git project.
   * @return {@link RepositoryType} of the repository.
   */
  public static RepositoryType getRepositoryType(Path repositoryPath, String gitProjectName) {

    return getRepositoryType(repositoryPath, gitProjectName, 0);
  }

  /**
   * Internal recursive method with depth tracking to prevent infinite recursion.
   *
   * @param repositoryPath the path of the repository to be checked.
   * @param gitProjectName the name of the git project.
   * @param depth the current recursion depth.
   * @return {@link RepositoryType} of the repository.
   */
  private static RepositoryType getRepositoryType(Path repositoryPath, String gitProjectName, int depth) {

    // Prevent infinite recursion by limiting depth (max 2 levels: root -> settings)
    if (depth > 2) {
      return RepositoryType.UNKNOWN;
    }

    if (!Files.exists(repositoryPath)) {
      return RepositoryType.UNKNOWN;
    }

    if (Files.exists(repositoryPath.resolve(EnvironmentVariables.DEFAULT_PROPERTIES))
        || Files.exists(repositoryPath.resolve(EnvironmentVariables.LEGACY_PROPERTIES))) {
      return RepositoryType.SETTINGS;
    } else if (gitProjectName != null
        && Files.exists(repositoryPath.resolve(IdeContext.FOLDER_SETTINGS))
        && getRepositoryType(repositoryPath.resolve(IdeContext.FOLDER_SETTINGS), gitProjectName, depth + 1) == RepositoryType.SETTINGS) {
      return RepositoryType.CODE_SETTINGS_COMBINED;
    } else if (!Files.exists(repositoryPath.resolve(IdeContext.FOLDER_SETTINGS))) {
      return RepositoryType.CODE;
    }
    return RepositoryType.UNKNOWN;
  }
}

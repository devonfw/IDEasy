package com.devonfw.tools.ide.git.repository;

import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;

/// Utility class for IDEasy settings/code repositories
public class RepositoryUtil {

  /**
   * Checks whether te given git repository is a settings repository, a combined settings and code repository, or a typical code repositor. Combined code and
   * settings repository is detected by checking whether IDE_HOME/workspaces/main/[gitProjectName]/settings exists and is a valid settings repository.
   *
   * @param repositoryPath - The path of the repository to be checked.
   * @return {@link RepositoryType} of the repository.
   */
  public static RepositoryType getRepositoryType(Path repositoryPath, String gitProjectName) {

    if (!Files.exists(repositoryPath)) {
      return RepositoryType.UNKNOWN;
    }

    if (Files.exists(repositoryPath.resolve(EnvironmentVariables.DEFAULT_PROPERTIES))
        || Files.exists(repositoryPath.resolve(EnvironmentVariables.LEGACY_PROPERTIES))) {
      return RepositoryType.SETTINGS;
    } else if (gitProjectName != null
        && Files.exists(
        repositoryPath.resolve(IdeContext.FOLDER_SETTINGS))
        && getRepositoryType(
        repositoryPath.resolve(IdeContext.FOLDER_SETTINGS),
        gitProjectName) == RepositoryType.SETTINGS) {
      return RepositoryType.CODE_SETTINGS_COMBINED;
    } else if (!Files.exists(repositoryPath.resolve(IdeContext.FOLDER_SETTINGS))) {
      return RepositoryType.CODE;
    }
    return RepositoryType.UNKNOWN;
  }

  /// enum representation of a detected {@link RepositoryType}
  public enum RepositoryType {
    /// Git Repository is a code repository.
    CODE,
    /// Git Repository is a settings repository.
    SETTINGS,
    /// A combined code & settings repository contains both the settings-folder and the code within the workspace folder.
    CODE_SETTINGS_COMBINED,
    /// The type of the repository could not be determined.
    UNKNOWN
  }
}

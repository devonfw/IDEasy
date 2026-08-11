package com.devonfw.tools.ide.git.repository;

import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;

/// Utility class for IDEasy settings/code repositories
public class RepositoryUtil {

  /**
   * Checks whether te given repository is a settings repository by checking for the presence of ide.properties or devon.properties on the top level.
   *
   * @param repositoryPath - The path of the repository to be checked.
   * @return true if the repo in the given path is a settings repository
   */
  public static boolean isSettingsRepository(Path repositoryPath) {
    return Files.exists(repositoryPath.resolve(EnvironmentVariables.DEFAULT_PROPERTIES)) || Files.exists(
        repositoryPath.resolve(EnvironmentVariables.LEGACY_PROPERTIES));
  }

  /**
   * Checks whether te given repository is a code repository by checking for the presence of ide.properties or devon.properties within a settings folder on the
   * top level.
   *
   * @param repositoryPath - The path of the repository to be checked.
   * @return true if the repo in the given path is a code repository
   */
  public static boolean isCodeRepository(Path repositoryPath) {
    return isSettingsRepository(repositoryPath.resolve(IdeContext.FOLDER_SETTINGS));
  }
}

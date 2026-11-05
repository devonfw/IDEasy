package com.devonfw.tools.ide.git.repository;

import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.io.FileAccess;

/**
 * Enum representation of a detected {@link RepositoryType}.
 */
public enum RepositoryType {

  /** Git Repository is a settings repository. */
  SETTINGS,

  /** A combined code & settings repository contains both the settings-folder and the code within the workspace folder. */
  CODE_SETTINGS_COMBINED,

  /** The type of the repository could not be determined. */
  UNKNOWN;

  /**
   * Checks whether the given git repository is a settings repository, a combined settings and code repository, or a typical code repository. A combined code
   * and settings repository is detected by a top-level {@code settings} folder that itself is a valid settings folder.
   *
   * @param repositoryPath the {@link Path} to the repository to check.
   * @param ideContext a {@link IdeContext}
   * @return the {@link RepositoryType} of the repository.
   */
  public static RepositoryType ofGitRoot(Path repositoryPath, IdeContext ideContext) {

    if (repositoryPath == null || !Files.isDirectory(repositoryPath)) {
      return RepositoryType.UNKNOWN;
    }
    if (isSettingsFolder(repositoryPath) && ideContext.getGitContext().isGitRepo(repositoryPath)) {
      return RepositoryType.SETTINGS;
    }
    Path settingsFolder = repositoryPath.resolve(IdeContext.FOLDER_SETTINGS);
    if (isSettingsFolder(settingsFolder)) {
      return RepositoryType.CODE_SETTINGS_COMBINED;
    }
    // there is no valid settings folder to be found.
    return RepositoryType.UNKNOWN;
  }

  /**
   * @param settingsPath
   * @param context
   * @return
   */
  public static RepositoryType ofSettingsPath(Path settingsPath, IdeContext context) {

    if (settingsPath == null || context == null) {
      return RepositoryType.UNKNOWN;
    }
    if (context.getGitContext().isGitRepo(settingsPath)) {
      return RepositoryType.SETTINGS;
    } else if (isCombinedSettingsCodeRepository(settingsPath, context)) {
      return RepositoryType.CODE_SETTINGS_COMBINED;
    }
    return RepositoryType.UNKNOWN;
  }

  /**
   * @return true if repository is either of type {@code SETTINGS} or {@code CODE_SETTINGS_COMBINED}
   */
  public boolean isValid() {
    return this == SETTINGS || this == CODE_SETTINGS_COMBINED;
  }

  /**
   * @param folder the {@link Path} to check.
   * @return {@code true} if the given {@code folder} is the root of a settings repository, {@code false} otherwise.
   */
  private static boolean isSettingsFolder(Path folder) {

    return (Files.exists(folder.resolve(EnvironmentVariables.DEFAULT_PROPERTIES))
        || Files.exists(folder.resolve(EnvironmentVariables.LEGACY_PROPERTIES)));
  }

  private static boolean isCombinedSettingsCodeRepository(Path settingsPath, IdeContext context) {

    FileAccess fileAccess = context.getFileAccess();
    if (settingsPath != null) {
      boolean settingsIsLink = Files.isSymbolicLink(settingsPath) || fileAccess.isJunction(settingsPath);
      if (settingsIsLink) {
        Path realPath = fileAccess.toRealPath(settingsPath);
        if (realPath != null) {
          return context.getGitContext().isGitRepo(realPath.getParent());
        }
        return true;
      }
    }
    return false;
  }
}

package com.devonfw.tools.ide.git.repository;

import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.git.GitContext;

/**
 * Enum representation of a detected {@link RepositoryType}.
 */
public enum RepositoryType {

  /** Git Repository is a code repository. */
  PLAIN_CODE,

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
   * @param gitContext a {@link GitContext}
   * @return the {@link RepositoryType} of the repository.
   */
  public static RepositoryType of(Path repositoryPath, GitContext gitContext) {

    if (repositoryPath == null || !Files.isDirectory(repositoryPath)) {
      return RepositoryType.UNKNOWN;
    }
    if (isSettingsFolder(repositoryPath) && gitContext.isGitRepo(repositoryPath)) {
      //TODO: review this for the case of code-settings repo. This could cause issues acc. to claude:
      // Combined code-settings repo + --force/--force-pull regression (source-trace-confirmed, niche path — please author-confirm).
      // For a combined repo (IDE_HOME/settings → symlink into <repo>/settings, .git one level up),
      // getRepositoryType(IDE_HOME/settings) classifies as PLAIN_CODE because .git isn't in that folder (RepositoryUtil.java:22-39).
      // The non-force case is safe (the guard at :170 skips the pull), but --force/--force-pull bypasses the guard and then checkClonedSettings
      // backs up the valid settings and re-clones from scratch instead of pulling; even a passing check would then fail in
      // applySettings (which re-derives PLAIN_CODE at :229). On main this path was a plain git pull. No test covers it.
      return RepositoryType.SETTINGS;
    }
    Path settingsFolder = repositoryPath.resolve(IdeContext.FOLDER_SETTINGS);
    if (isSettingsFolder(settingsFolder)) {
      return RepositoryType.CODE_SETTINGS_COMBINED;
    }
    if (!Files.exists(settingsFolder)) {
      return RepositoryType.PLAIN_CODE;
    }
    // there is no valid settings folder to be found.
    return RepositoryType.UNKNOWN;
  }

  /**
   * @return true if repository is either of type {@code SETTINGS} or {@code CODE_SETTINGS_COMBINED}
   */
  public boolean isSettingsOrCodeSettingsRepository() {
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
}

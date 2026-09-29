package com.devonfw.tools.ide.commandlet.update.settings;

import java.nio.file.Path;

import com.devonfw.tools.ide.git.repository.RepositoryType;

/**
 * Result of the settings {@link SettingsUpdater#checkSettings(Path)}  health check}.
 *
 * @param repositoryType the {@link RepositoryType} of the settings repository.
 * @param errorMessage the reason why the settings could not be updated or {@code null} if the health check succeeded.
 * @param settingsDirectory path to the temporary folder this health check was performed on.
 */
public record SettingsHealthCheckResult(RepositoryType repositoryType, Path settingsDirectory, String errorMessage,
                                        boolean isExistingProject) {

  /**
   * @param repositoryType the {@link RepositoryType}.
   * @param temporarySettingsDirectory path to the temporary folder this health check was performed on.
   * @param isExistingProject whether the settings we checked were freshly cloned or already existed
   * @return a {@link SettingsHealthCheckResult} for a successful health check.
   */
  public static SettingsHealthCheckResult ofSuccess(RepositoryType repositoryType, Path temporarySettingsDirectory,
      boolean isExistingProject) {

    return new SettingsHealthCheckResult(repositoryType, temporarySettingsDirectory, null, isExistingProject);
  }

  /**
   * @param repositoryType the {@link RepositoryType} of the settings that are already present.
   * @param errorMessage the reason why the settings could not be updated.
   * @param temporarySettingsDirectory path to the temporary folder this health check was performed on.
   * @param isExistingProject whether the settings we checked were cloned freshly or already existed
   * @return a {@link SettingsHealthCheckResult} for a failed but recoverable health check.
   */
  public static SettingsHealthCheckResult ofFailed(RepositoryType repositoryType, String errorMessage, Path temporarySettingsDirectory,
      boolean isExistingProject) {

    return new SettingsHealthCheckResult(repositoryType, temporarySettingsDirectory, errorMessage,
        isExistingProject);
  }
}

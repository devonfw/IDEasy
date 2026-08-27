package com.devonfw.tools.ide.commandlet.update.settings;

import com.devonfw.tools.ide.git.repository.RepositoryType;

import java.nio.file.Path;

/**
 * Result of the settings {@link SettingsUpdater#checkSettings() health check}.
 *
 * @param status the {@link HealthCheckResultStatus}.
 * @param repositoryType the {@link RepositoryType} of the settings repository.
 * @param errorMessage the reason why the settings could not be updated or {@code null} if the health check succeeded.
 */
public record SettingsHealthCheckResult(HealthCheckResultStatus status, RepositoryType repositoryType, Path temporarySettingsDirectory, String errorMessage) {

  /**
   * @param status the {@link HealthCheckResultStatus}.
   * @param repositoryType the {@link RepositoryType}.
   * @return a {@link SettingsHealthCheckResult} for a successful health check.
   */
  public static SettingsHealthCheckResult of(HealthCheckResultStatus status, RepositoryType repositoryType, Path temporaryRepoDirectory) {

    return new SettingsHealthCheckResult(status, repositoryType, temporaryRepoDirectory, null);
  }

  /**
   * @param repositoryType the {@link RepositoryType} of the settings that are already present.
   * @param errorMessage the reason why the settings could not be updated.
   * @return a {@link SettingsHealthCheckResult} for a failed but recoverable health check.
   */
  public static SettingsHealthCheckResult failed(RepositoryType repositoryType, String errorMessage, Path temporaryRepoDirectory) {

    return new SettingsHealthCheckResult(HealthCheckResultStatus.SETTINGS_INVALID, repositoryType, temporaryRepoDirectory, errorMessage);
  }
}

package com.devonfw.tools.ide.commandlet.update.settings;

/**
 * Status of the settings {@link SettingsUpdater#checkSettings() health check} describing what {@link SettingsUpdater#applySettings(SettingsHealthCheckResult)} has
 * to do.
 */
public enum HealthCheckResultStatus {
  /** The settings repository was cloned to a temporary directory and is valid - it can be moved to its final location. */
  SETTINGS_VALID,
  /** The settings repository already existed and was cloned to a temporary directory and is valid - it can be moved to its final location. */
  SETTINGS_VALID_EXISTING,
  /** The settings repository is invalid */
  SETTINGS_INVALID
}

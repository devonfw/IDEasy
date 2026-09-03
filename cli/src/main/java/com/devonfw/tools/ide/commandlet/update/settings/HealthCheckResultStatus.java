package com.devonfw.tools.ide.commandlet.update.settings;

import java.nio.file.Path;

/**
 * Status of the settings {@link SettingsUpdater#checkSettings(Path)}  health check} describing what {@link SettingsUpdater#applySettings(boolean, Path)} has to
 * do.
 */
public enum HealthCheckResultStatus {
  /** The settings repository was cloned to a temporary directory and is valid - it can be moved to its final location. */
  SETTINGS_VALID,
  /** The settings repository is invalid */
  SETTINGS_INVALID
}

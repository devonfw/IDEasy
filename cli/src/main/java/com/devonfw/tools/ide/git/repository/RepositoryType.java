package com.devonfw.tools.ide.git.repository;

/**
 * Enum representation of a detected {@link RepositoryType}.
 */
public enum RepositoryType {

  /** Git Repository is a code repository. */
  CODE,

  /** Git Repository is a settings repository. */
  SETTINGS,

  /** A combined code & settings repository contains both the settings-folder and the code within the workspace folder. */
  CODE_SETTINGS_COMBINED,

  /** The type of the repository could not be determined. */
  UNKNOWN;

  /**
   * @return true if repository is either of type {@code SETTINGS} or {@code CODE_SETTINGS_COMBINED}
   */
  public boolean isSettingsOrCodeSettingsRepository() {
    return this == SETTINGS || this == CODE_SETTINGS_COMBINED;
  }
}

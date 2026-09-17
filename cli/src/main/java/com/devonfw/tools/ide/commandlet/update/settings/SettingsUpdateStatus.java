package com.devonfw.tools.ide.commandlet.update.settings;

/// Status of the update action of a settings repo.
public enum SettingsUpdateStatus {
  /** Existing settings have been successfully updated **/
  SETTINGS_UPDATED,
  /** Freshly cloned settings have been successfully applied **/
  SETTINGS_CLONED,
  /** Error occurred **/
  SETTINGS_UPDATE_FAILED
}

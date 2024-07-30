package com.devonfw.tools.ide.environment;

/**
 * The subset of {@link EnvironmentVariables} types to be used for settings files.
 *
 * @see EnvironmentVariables#getType()
 */
public enum EnvironmentVariablesFiles {
  /**
   * Type of {@link EnvironmentVariables} from the {@link com.devonfw.tools.ide.context.IdeContext#getUserHome() users HOME directory}.
   */
  USER,

  /**
   * Type of {@link EnvironmentVariables} from the {@link com.devonfw.tools.ide.context.IdeContext#getSettingsPath() settings directory}.
   */
  SETTINGS,

  /**
   * Type of {@link EnvironmentVariables} from the {@link com.devonfw.tools.ide.context.IdeContext#getWorkspacePath() workspace directory}.
   */
  WORKSPACE,

  /**
   * Type of {@link EnvironmentVariables} from the {@link com.devonfw.tools.ide.context.IdeContext#getConfPath() conf directory}. Allows the user to override or
   * customize project specific variables.
   */
  CONF;

  private EnvironmentVariablesType type;

  static {
    USER.type = EnvironmentVariablesType.USER;
    SETTINGS.type = EnvironmentVariablesType.SETTINGS;
    WORKSPACE.type = EnvironmentVariablesType.WORKSPACE;
    CONF.type = EnvironmentVariablesType.CONF;
  }

  /**
   * @return the corresponding {@link EnvironmentVariablesType}
   */
  public EnvironmentVariablesType toType() {
    return type;
  }

}

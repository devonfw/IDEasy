package com.devonfw.tools.ide.environment;

import java.util.Map;

import com.devonfw.tools.ide.log.IdeLogger;

/**
 * Implementation of {@link EnvironmentVariables} using {@link System#getenv()}.
 *
 * @see EnvironmentVariablesType#SYSTEM
 */
public final class EnvironmentVariablesSystem extends EnvironmentVariablesMap {

  private EnvironmentVariablesSystem(IdeLogger logger) {

    super(null, logger);
  }

  @Override
  public EnvironmentVariablesType getType() {

    return EnvironmentVariablesType.SYSTEM;
  }

  @Override
  protected Map<String, String> getVariables() {

    return System.getenv();
  }

  /**
   * @param logger the {@link IdeLogger}.
   * @return the {@link EnvironmentVariablesSystem} instance.
   */
  static EnvironmentVariablesSystem of(IdeLogger logger) {

    return new EnvironmentVariablesSystem(logger);
  }

}

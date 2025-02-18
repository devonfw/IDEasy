package com.devonfw.tools.ide.environment;

import java.util.Map;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Implementation of {@link EnvironmentVariables} using {@link System#getenv()}.
 *
 * @see EnvironmentVariablesType#SYSTEM
 */
public final class EnvironmentVariablesSystem extends EnvironmentVariablesMap {

  private EnvironmentVariablesSystem(IdeContext context) {

    super(null, context);
  }

  @Override
  public EnvironmentVariablesType getType() {

    return EnvironmentVariablesType.SYSTEM;
  }

  @Override
  public Map<String, String> getVariables() {

    return this.context.getSystem().getEnv();
  }

  /**
   * @param context the {@link IdeContext}.
   * @return the {@link EnvironmentVariablesSystem} instance.
   */
  static EnvironmentVariablesSystem of(IdeContext context) {

    return new EnvironmentVariablesSystem(context);
  }
}

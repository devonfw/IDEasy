package com.devonfw.tools.ide.environment;

import java.util.Map;

import com.devonfw.tools.ide.log.IdeLogger;

/**
 * Implementation of {@link EnvironmentVariables}.
 */
abstract class EnvironmentVariablesMap extends AbstractEnvironmentVariables {

  /**
   * The constructor.
   *
   * @param parent the parent {@link EnvironmentVariables} to inherit from.
   * @param logger the {@link IdeLogger}.
   */
  EnvironmentVariablesMap(AbstractEnvironmentVariables parent, IdeLogger logger) {

    super(parent, logger);
  }

  /**
   * @return the {@link Map} with the underlying variables. Internal method do not call from outside and never
   *         manipulate this {@link Map} externally.
   */
  protected abstract Map<String, String> getVariables();

  @Override
  public String getFlat(String name) {

    String value = getVariables().get(name);
    if (value == null) {
      this.logger.trace("{}: Variable {} is undefined.", getSource(), name);
    } else {
      this.logger.trace("{}: Variable {}={}", getSource(), name, value);
    }
    return value;
  }

  @Override
  public String toString() {

    return getSource() + ":\n" + getVariables();
  }

}

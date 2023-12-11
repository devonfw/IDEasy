package com.devonfw.tools.ide.environment;

/**
 * Implementation of {@link EnvironmentVariables} that resolves variables recursively.
 */
public class EnvironmentVariablesResolved extends AbstractEnvironmentVariables {

  /**
   * The constructor.
   *
   * @param parent the parent {@link EnvironmentVariables} to inherit from.
   */
  EnvironmentVariablesResolved(AbstractEnvironmentVariables parent) {

    super(parent, parent.context);
  }

  @Override
  public EnvironmentVariablesType getType() {

    return EnvironmentVariablesType.RESOLVED;
  }

  @Override
  public String getFlat(String name) {

    return null;
  }

  @Override
  public String get(String name) {

    String value = getValue(name);
    if (value != null) {
      value = resolve(value, name);
    }
    return value;
  }

  @Override
  public EnvironmentVariables resolved() {

    return this;
  }

}

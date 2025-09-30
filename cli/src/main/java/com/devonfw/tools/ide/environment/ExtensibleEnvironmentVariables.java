package com.devonfw.tools.ide.environment;

import java.util.HashMap;
import java.util.Map;

/**
 * Subclass of {@link EnvironmentVariablesResolved} that resolves variables recursively and allows new variables to be added to the resolver.
 */
public class ExtensibleEnvironmentVariables extends EnvironmentVariablesResolved {

  private final Map<String, String> additionalEnvironmentVariables;

  /**
   * The constructor.
   *
   * @param parent the parent {@link EnvironmentVariables} to inherit from.
   */
  public ExtensibleEnvironmentVariables(AbstractEnvironmentVariables parent) {
    super(parent);
    this.additionalEnvironmentVariables = new HashMap<>();
  }

  /**
   * @param name the variable string to be resolved
   * @param value the string the variable should be resolved into
   */
  public void addVariableResolver(String name, String value) {
    this.additionalEnvironmentVariables.put(name, value);
  }

  @Override
  protected String getValue(String name, boolean ignoreDefaultValue) {
    if (this.additionalEnvironmentVariables.containsKey(name)) {
      return this.additionalEnvironmentVariables.get(name);
    }
    return super.getValue(name, ignoreDefaultValue);
  }
}

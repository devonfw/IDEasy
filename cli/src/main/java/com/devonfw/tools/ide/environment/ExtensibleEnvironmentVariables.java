package com.devonfw.tools.ide.environment;

import java.util.HashMap;
import java.util.Map;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Subclass of {@link EnvironmentVariablesMap} that resolves variables recursively and allows new variables to be added to the resolver.
 */
public class ExtensibleEnvironmentVariables extends EnvironmentVariablesMap {

  private final Map<String, String> additionalEnvironmentVariables;

  /**
   * The constructor.
   *
   * @param parent the parent {@link EnvironmentVariables} to inherit from.
   * @param context the context to use.
   */
  public ExtensibleEnvironmentVariables(AbstractEnvironmentVariables parent, IdeContext context) {
    super(parent, context);
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
    String value = this.additionalEnvironmentVariables.get(name);
    if (value != null) {
      return value;
    }
    return super.getValue(name, ignoreDefaultValue);
  }

  @Override
  protected Map<String, String> getVariables() {
    return this.additionalEnvironmentVariables;
  }

  @Override
  public EnvironmentVariablesType getType() {
    return EnvironmentVariablesType.TOOL;
  }
}

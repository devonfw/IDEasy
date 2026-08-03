package com.devonfw.tools.ide.environment;

import java.util.HashMap;
import java.util.Map;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Subclass of {@link EnvironmentVariablesMap} that resolves variables recursively and allows new variables to be added to the resolver.
 */
public class ExtensibleEnvironmentVariables extends EnvironmentVariablesMap {

  private final Map<String, String> variables;

  /**
   * The constructor.
   *
   * @param parent the parent {@link EnvironmentVariables} to inherit from.
   * @param context the context to use.
   */
  public ExtensibleEnvironmentVariables(AbstractEnvironmentVariables parent, IdeContext context) {
    super(parent, context);
    this.variables = new HashMap<>();
  }

  /**
   * @param name the name of the variable to set.
   * @param value the value of the variable to set.
   */
  public void setValue(String name, String value) {
    this.variables.put(name, value);
  }

  @Override
  protected String getValue(String name, boolean ignoreDefaultValue) {
    String value = this.variables.get(name);
    if (value != null) {
      return value;
    }
    return super.getValue(name, ignoreDefaultValue);
  }

  @Override
  protected Map<String, String> getVariables() {
    return this.variables;
  }

  @Override
  public EnvironmentVariablesType getType() {
    return EnvironmentVariablesType.TOOL;
  }
}

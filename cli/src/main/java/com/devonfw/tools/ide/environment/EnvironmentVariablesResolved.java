package com.devonfw.tools.ide.environment;

import java.util.Set;

import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.variable.VariableDefinition;

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
  public String get(String name, boolean ignoreDefaultValue) {

    String value = getValue(name, ignoreDefaultValue);
    if (value != null) {
      value = resolve(value, name);
    }
    return value;
  }

  @Override
  public EnvironmentVariables resolved() {

    return this;
  }

  @Override
  protected void collectVariables(Set<String> variables) {

    for (VariableDefinition<?> var : IdeVariables.VARIABLES) {
      if (var.isExport() || var.isForceDefaultValue()) {
        variables.add(var.getName());
      }
    }
    super.collectVariables(variables);
  }

  @Override
  protected boolean isExported(String name) {

    VariableDefinition<?> var = IdeVariables.get(name);
    if ((var != null) && var.isExport()) {
      return true;
    }
    return super.isExported(name);
  }
}

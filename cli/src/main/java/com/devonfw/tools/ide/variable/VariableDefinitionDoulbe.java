package com.devonfw.tools.ide.variable;

import java.util.function.Function;

import com.devonfw.tools.ide.context.IdeContext;

public class VariableDefinitionDoulbe extends AbstractVariableDefinition<Double> {

  public VariableDefinitionDoulbe(String name) {
    super(name);
  }

  public VariableDefinitionDoulbe(String name, String legacyName) {
    super(name, legacyName);
  }

  public VariableDefinitionDoulbe(String name, String legacyName, Function<IdeContext, Double> defaultFactory) {
    super(name, legacyName, defaultFactory);
  }

  public VariableDefinitionDoulbe(String name, String legacyName, Function<IdeContext, Double> defaultFactory, boolean forceDefaultValue) {
    super(name, legacyName, defaultFactory, forceDefaultValue);
  }

  public VariableDefinitionDoulbe(String name, String legacyName, Function<IdeContext, Double> defaultFactory, boolean forceDefaultValue, boolean export) {
    super(name, legacyName, defaultFactory, forceDefaultValue, export);
  }

  @Override
  public Class<Double> getValueType() {
    return null;
  }

  @Override
  public Double fromString(String value, IdeContext context) {
    return 0.0;
  }
}

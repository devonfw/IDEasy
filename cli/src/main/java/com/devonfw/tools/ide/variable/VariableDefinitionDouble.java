package com.devonfw.tools.ide.variable;

import java.util.function.Function;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Implementation of {@link VariableDefinition} for a variable with the {@link #getValueType() value type} {@link Double}.
 */
public class VariableDefinitionDouble extends AbstractVariableDefinition<Double> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   */
  public VariableDefinitionDouble(String name) {
    super(name);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   */
  public VariableDefinitionDouble(String name, String legacyName) {
    super(name, legacyName);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   * @param defaultValueFactory the factory {@link Function} for the {@link #getDefaultValue(IdeContext) default value}.
   */
  public VariableDefinitionDouble(String name, String legacyName, Function<IdeContext, Double> defaultValueFactory) {
    super(name, legacyName, defaultValueFactory);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   * @param defaultValueFactory the factory {@link Function} for the {@link #getDefaultValue(IdeContext) default value}.
   * @param forceDefaultValue the {@link #isForceDefaultValue() forceDefaultValue} flag.
   */
  public VariableDefinitionDouble(String name, String legacyName, Function<IdeContext, Double> defaultValueFactory, boolean forceDefaultValue) {
    super(name, legacyName, defaultValueFactory, forceDefaultValue);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   * @param defaultValueFactory the factory {@link Function} for the {@link #getDefaultValue(IdeContext) default value}.
   * @param forceDefaultValue the {@link #isForceDefaultValue() forceDefaultValue} flag.
   * @param export the {@link #isExport() export} flag.
   */
  public VariableDefinitionDouble(String name, String legacyName, Function<IdeContext, Double> defaultValueFactory, boolean forceDefaultValue, boolean export) {
    super(name, legacyName, defaultValueFactory, forceDefaultValue, export);
  }

  @Override
  public Class<Double> getValueType() {
    return Double.class;
  }

  @Override
  public Double fromString(String value, IdeContext context) {
    return Double.valueOf(value);
  }
}

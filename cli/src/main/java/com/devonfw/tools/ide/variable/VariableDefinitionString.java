package com.devonfw.tools.ide.variable;

import java.util.function.Function;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Implementation of {@link VariableDefinition} for a variable with the {@link #getValueType() value type}
 * {@link String}.
 */
public class VariableDefinitionString extends AbstractVariableDefinition<String> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   */
  public VariableDefinitionString(String name) {

    super(name);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   */
  public VariableDefinitionString(String name, String legacyName) {

    super(name, legacyName);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   * @param defaultValueFactory the factory {@link Function} for the {@link #getDefaultValue(IdeContext) default value}.
   */
  public VariableDefinitionString(String name, String legacyName, Function<IdeContext, String> defaultValueFactory) {

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
  public VariableDefinitionString(String name, String legacyName, Function<IdeContext, String> defaultValueFactory,
      boolean forceDefaultValue) {

    super(name, legacyName, defaultValueFactory, forceDefaultValue);
  }

  @Override
  public Class<String> getValueType() {

    return String.class;
  }

  @Override
  public String fromString(String value, IdeContext context) {

    return value;
  }
}

package com.devonfw.tools.ide.variable;

import java.util.function.Function;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Implementation of {@link VariableDefinition} for a variable with the {@link #getValueType() value type} as an Enum.
 *
 * @param <E> the enum type.
 */
public class VariableDefinitionEnum<E extends Enum<E>> extends AbstractVariableDefinition<E> {

  private final Class<E> enumType;

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param enumType the class of the enum.
   */
  public VariableDefinitionEnum(String name, Class<E> enumType) {
    super(name);
    this.enumType = enumType;
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   * @param enumType the class of the enum.
   */
  public VariableDefinitionEnum(String name, String legacyName, Class<E> enumType) {
    super(name, legacyName);
    this.enumType = enumType;
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   * @param enumType the class of the enum.
   * @param defaultValueFactory the factory {@link Function} for the {@link #getDefaultValue(IdeContext) default value}.
   */
  public VariableDefinitionEnum(String name, String legacyName, Class<E> enumType, Function<IdeContext, E> defaultValueFactory) {
    super(name, legacyName, defaultValueFactory);
    this.enumType = enumType;
  }

  @Override
  public Class<E> getValueType() {
    return enumType;
  }

  /**
   * Converts a string value to the corresponding enum constant. Converts the string to upper case before matching with the enum values.
   *
   * @param value the string representation of the enum.
   * @param context the context for the current operation.
   * @return the corresponding enum constant.
   * @throws IllegalArgumentException if the string doesn't match any enum constant.
   */
  @Override
  public E fromString(String value, IdeContext context) {
    return Enum.valueOf(enumType, value.toUpperCase(java.util.Locale.ROOT));
  }
}

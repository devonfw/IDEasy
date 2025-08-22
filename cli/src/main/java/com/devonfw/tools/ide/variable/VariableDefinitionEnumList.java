package com.devonfw.tools.ide.variable;

import java.util.List;
import java.util.function.Function;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Implementation of {@link VariableDefinition} for a variable with the {@link #getValueType() value type} as an Enum.
 *
 * @param <E> the enum type.
 */
public class VariableDefinitionEnumList<E extends Enum<E>> extends AbstractVariableDefinitionList<E> {

  private final Class<E> enumType;

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param enumType the class of the enum.
   */
  public VariableDefinitionEnumList(String name, Class<E> enumType) {
    this(name, null, enumType);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   * @param enumType the class of the enum.
   */
  public VariableDefinitionEnumList(String name, String legacyName, Class<E> enumType) {
    this(name, legacyName, enumType, c -> List.of());
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   * @param enumType the class of the enum.
   * @param defaultValueFactory the factory {@link Function} for the {@link #getDefaultValue(IdeContext) default value}.
   */
  public VariableDefinitionEnumList(String name, String legacyName, Class<E> enumType, Function<IdeContext, List<E>> defaultValueFactory) {
    super(name, legacyName, defaultValueFactory);
    this.enumType = enumType;
  }

  @Override
  protected E parseValue(String value, IdeContext context) {

    return Enum.valueOf(this.enumType, value.toUpperCase(java.util.Locale.ROOT));
  }

}

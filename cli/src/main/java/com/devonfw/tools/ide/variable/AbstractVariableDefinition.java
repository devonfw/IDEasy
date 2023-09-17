package com.devonfw.tools.ide.variable;

import java.util.Objects;
import java.util.function.Function;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Abstract base implementation of {@link VariableDefinition}.
 *
 * @param <V> the {@link #getValueType() value type}.
 */
public abstract class AbstractVariableDefinition<V> implements VariableDefinition<V> {

  @SuppressWarnings("rawtypes")
  private static final Function NO_DEFAULT_VALUE = context -> null;

  private final String name;

  private final String legacyName;

  private final Function<IdeContext, V> defaultValueFactory;

  private final boolean forceDefaultValue;

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   */
  public AbstractVariableDefinition(String name) {

    this(name, null, NO_DEFAULT_VALUE, false);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   */
  public AbstractVariableDefinition(String name, String legacyName) {

    this(name, legacyName, NO_DEFAULT_VALUE, false);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   * @param defaultValueFactory the factory {@link Function} for the {@link #getDefaultValue(IdeContext) default value}.
   */
  public AbstractVariableDefinition(String name, String legacyName, Function<IdeContext, V> defaultValueFactory) {

    this(name, legacyName, defaultValueFactory, false);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() variable name}.
   * @param legacyName the {@link #getLegacyName() legacy name}.
   * @param defaultValueFactory the factory {@link Function} for the {@link #getDefaultValue(IdeContext) default value}.
   * @param forceDefaultValue the {@link #isForceDefaultValue() forceDefaultValue} flag.
   */
  public AbstractVariableDefinition(String name, String legacyName, Function<IdeContext, V> defaultValueFactory,
      boolean forceDefaultValue) {

    super();
    this.name = name;
    this.legacyName = legacyName;
    this.defaultValueFactory = defaultValueFactory;
    this.forceDefaultValue = forceDefaultValue;
  }

  @Override
  public boolean isForceDefaultValue() {

    return this.forceDefaultValue;
  }

  @Override
  public String getName() {

    return this.name;
  }

  @Override
  public String getLegacyName() {

    return this.legacyName;
  }

  @Override
  public V getDefaultValue(IdeContext context) {

    return this.defaultValueFactory.apply(context);
  }

  @Override
  public V get(IdeContext context) {

    Objects.requireNonNull(context);
    String valueAsString = null;
    if (!this.forceDefaultValue) {
      valueAsString = context.getVariables().get(this.name);
      if (valueAsString == null) {
        if (this.legacyName != null) {
          valueAsString = context.getVariables().get(this.legacyName);
        }
      }
    }
    V value;
    if (valueAsString == null) {
      value = getDefaultValue(context);
    } else {
      value = fromString(valueAsString, context);
    }
    return value;
  }

  @Override
  public String toString() {

    Class<V> valueType = getValueType();
    if (valueType == String.class) {
      return this.name;
    } else {
      return this.name + "[" + valueType.getSimpleName() + "]";
    }
  }

}

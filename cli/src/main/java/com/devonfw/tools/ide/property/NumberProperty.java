package com.devonfw.tools.ide.property;

import java.util.function.Consumer;

/**
 * {@link Property} with {@link #getValueType() value type} {@link Long}.
 */
public class NumberProperty extends Property<Long> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public NumberProperty(String name, boolean required, String alias) {

    this(name, required, alias, null);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   * @param validator the {@link Consumer} used to {@link #validate() validate} the {@link #getValue() value}.
   */
  public NumberProperty(String name, boolean required, String alias, Consumer<Long> validator) {

    super(name, required, alias, validator);
  }

  @Override
  public Class<Long> getValueType() {

    return Long.class;
  }

  @Override
  public Long parse(String valueAsString) {

    try {
      return Long.valueOf(valueAsString);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid number for property " + getName(), e);
    }
  }

}

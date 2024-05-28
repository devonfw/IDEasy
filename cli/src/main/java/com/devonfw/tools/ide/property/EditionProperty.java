package com.devonfw.tools.ide.property;

import com.devonfw.tools.ide.context.IdeContext;

import java.util.function.Consumer;

public class EditionProperty extends Property<String> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public EditionProperty(String name, boolean required, String alias) {

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
  public EditionProperty(String name, boolean required, String alias, Consumer<String> validator) {

    super(name, required, alias, false, validator);
  }

  @Override
  public Class<String> getValueType() {

    return String.class;
  }

  @Override
  public String parse(String valueAsString, IdeContext context) {

    return valueAsString;
  }
}

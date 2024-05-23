package com.devonfw.tools.ide.property;

import com.devonfw.tools.ide.context.IdeContext;

import java.util.function.Consumer;

/**
 * {@link Property} with {@link #getValueType() value type} {@link String}.
 */
public class StringProperty extends Property<String> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   * @param multiValued
   */
  public StringProperty(String name, boolean required, String alias, boolean multiValued) {

    this(name, required, alias, null, multiValued);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   * @param validator the {@link Consumer} used to {@link #validate() validate} the {@link #getValue() value}.
   * @param multiValued
   */
  public StringProperty(String name, boolean required, String alias, Consumer<String> validator, boolean multiValued) {

    super(name, required, alias, validator, multiValued);
  }

  @Override
  public Class<String> getValueType() {

    return String.class;
  }

  @Override
  public String parse(String valueAsString, IdeContext context) {

    return valueAsString;
  }

  public String[] asArray() {

    return this.value.toArray(new String[0]);
  }

}

package com.devonfw.tools.ide.property;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * {@link Property} with {@link Locale} as {@link #getValueType() value type}.
 */
public class LocaleProperty extends Property<Locale> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public LocaleProperty(String name, boolean required, String alias) {

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
  public LocaleProperty(String name, boolean required, String alias, Consumer<Locale> validator) {

    super(name, required, alias, validator);
  }

  @Override
  public Class<Locale> getValueType() {

    return Locale.class;
  }

  @Override
  public Locale parse(String valueAsString) {

    return Locale.forLanguageTag(valueAsString);
  }

}

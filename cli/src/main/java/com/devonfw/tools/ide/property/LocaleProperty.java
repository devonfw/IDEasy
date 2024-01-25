package com.devonfw.tools.ide.property;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;

/**
 * {@link Property} with {@link Locale} as {@link #getValueType() value type}.
 */
public class LocaleProperty extends Property<Locale> {

  private static String[] availableLocales;

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
  public Locale parse(String valueAsString, IdeContext context) {

    return Locale.forLanguageTag(valueAsString);
  }

  @Override
  protected boolean completeValue(String arg, IdeContext context, Commandlet commandlet,
      CompletionCandidateCollector collector) {

    int count = collector.addAllMatches(arg, getAvailableLocales(), this, commandlet);
    return count > 0;
  }

  private static String[] getAvailableLocales() {

    if (availableLocales == null) {
      Locale[] locales = Locale.getAvailableLocales();
      availableLocales = new String[locales.length];
      for (int i = 0; i < locales.length; i++) {
        availableLocales[i] = locales[i].toLanguageTag();
      }
      Arrays.sort(availableLocales);
    }
    return availableLocales;
  }

}

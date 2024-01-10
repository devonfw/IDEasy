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

    if (arg.isEmpty()) {
      for (String locale : getAvailableLocales()) {
        collector.add(locale, this, commandlet);
      }
      return true;
    }
    boolean match = false;
    int index = Arrays.binarySearch(getAvailableLocales(), arg);
    if (index >= 0) {
      collector.add(availableLocales[index], this, commandlet);
      index++;
      match = true;
    } else {
      index = -index;
    }
    while (index < availableLocales.length) {
      if (availableLocales[index].startsWith(arg)) {
        collector.add(availableLocales[index], this, commandlet);
        match = true;
      } else {
        break;
      }
      index++;
    }
    return match;
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

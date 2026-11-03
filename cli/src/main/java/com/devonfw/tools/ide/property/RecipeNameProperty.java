package com.devonfw.tools.ide.property;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import com.devonfw.tools.ide.commandlet.AbstractCommandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.validation.PropertyValidator;

/**
 * {@link Property} holding the name of an OpenRewrite recipe. The set of valid names is data-driven
 * (loaded from the {@code ide-settings} settings folder) rather than a fixed {@code enum}, so the
 * {@link #completeValue(String, IdeContext, AbstractCommandlet, CompletionCandidateCollector) auto-completion}
 * is backed by a {@link Supplier} of candidate names and invalid values are rejected via an optional
 * {@link PropertyValidator}.
 */
public class RecipeNameProperty extends Property<String> {

  private final Supplier<List<String>> candidates;

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   * @param candidates the {@link Supplier} of the valid recipe names used for
   *     {@link #completeValue(String, IdeContext, AbstractCommandlet, CompletionCandidateCollector) auto-completion}.
   * @param validator the {@link PropertyValidator} used to {@link #validate() validate} the value, or
   *     {@code null} for none.
   */
  public RecipeNameProperty(String name, boolean required, String alias, Supplier<List<String>> candidates, PropertyValidator<String> validator) {

    super(name, required, alias, false, validator);
    this.candidates = candidates;
  }

  @Override
  public Class<String> getValueType() {

    return String.class;
  }

  @Override
  public String parse(String valueAsString, IdeContext context) {

    return valueAsString;
  }

  @Override
  protected void completeValue(String arg, IdeContext context, AbstractCommandlet commandlet, CompletionCandidateCollector collector) {

    List<String> names = this.candidates == null ? List.of() : this.candidates.get();
    if (names == null) {
      return;
    }
    for (String name : names) {
      if (name.toLowerCase(Locale.ROOT).startsWith(arg.toLowerCase(Locale.ROOT))) {
        collector.add(name.toLowerCase(Locale.ROOT), null, this, commandlet);
      }
    }
  }
}

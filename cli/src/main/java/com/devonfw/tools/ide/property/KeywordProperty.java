package com.devonfw.tools.ide.property;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;

/**
 * {@link BooleanProperty} for a keyword (e.g. "install" in "ide install mvn 3.9.4").
 */
public class KeywordProperty extends BooleanProperty {

  private final String optionName;

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public KeywordProperty(String name, boolean required, String alias) {

    super(getNormalizedName(name), required, alias);
    this.optionName = name;
  }

  private static String getNormalizedName(String name) {

    assert !name.isEmpty();
    if (name.startsWith("--")) {
      return name.substring(2);
    }
    return name;
  }

  /**
   * @return the option name (e.g. "--help") or the {@link #getName() name} if not an option (e.g. "install").
   */
  public String getOptionName() {

    return this.optionName;
  }

  @Override
  public boolean isValue() {

    return true;
  }

  @Override
  public boolean isExpectValue() {

    return false;
  }

  @Override
  public boolean matches(String nameOrAlias) {

    if (super.matches(nameOrAlias)) {
      return true;
    }
    return this.optionName.equals(nameOrAlias);
  }

  @Override
  public boolean apply(CliArguments args, IdeContext context, Commandlet commandlet, CompletionCandidateCollector collector) {

    String normalizedName = this.name;
    if (args.current().isOption()) {
      normalizedName = this.optionName;
    }
    return apply(normalizedName, args, context, commandlet, collector);
  }

  @Override
  protected boolean applyValue(String argValue, boolean lookahead, CliArguments args, IdeContext context, Commandlet commandlet,
      CompletionCandidateCollector collector) {

    setValue(true);
    return true;
  }
}

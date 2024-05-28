package com.devonfw.tools.ide.property;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;

/**
 * {@link Property} with {@link #getValueType() value type} {@link Boolean}.
 */
public class BooleanProperty extends Property<Boolean> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public BooleanProperty(String name, boolean required, String alias) {

    super(name, required, alias);
  }

  @Override
  public Class<Boolean> getValueType() {

    return Boolean.class;
  }

  /**
   * @return the value as primitive boolean.
   */
  public boolean isTrue() {

    return Boolean.TRUE.equals(getValue());
  }

  /**
   * @param value the {@link #getValue() value} as primitive boolean.
   */
  public void setValue(boolean value) {

    setValue(Boolean.valueOf(value));
  }

  @Override
  public Boolean parse(String valueAsString, IdeContext context) {

    Boolean result = parse(valueAsString);
    if (result == null) {
      throw new IllegalArgumentException("Illegal boolean value '" + valueAsString + "' for property " + getName());
    }
    return result;
  }

  private Boolean parse(String valueAsString) {

    if (valueAsString == null) {
      return null;
    }
    valueAsString = valueAsString.toLowerCase();
    if ("true".equals(valueAsString) || "yes".equals(valueAsString)) {
      return Boolean.TRUE;
    } else if ("false".equals(valueAsString) || "no".equals(valueAsString)) {
      return Boolean.FALSE;
    }
    return null;
  }

  @Override
  public void setValueAsString(String valueAsString, IdeContext context) {

    Boolean b;
    if (matches(valueAsString)) {
      // allow e.g. "--force" to enable "--force" option
      b = Boolean.TRUE;
    } else {
      b = parse(valueAsString, context);
    }
    setValue(b);
  }

  @Override
  protected boolean applyValue(String argValue, boolean lookahead, CliArguments args, IdeContext context, Commandlet commandlet,
      CompletionCandidateCollector collector) {

    if (lookahead) {
      Boolean b = parse(argValue);
      if (b == null) {
        setValue(true);
      } else {
        setValue(b);
        args.next();
      }
      return true;
    }
    return super.applyValue(argValue, lookahead, args, context, commandlet, collector);
  }

}

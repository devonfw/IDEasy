package com.devonfw.tools.ide.property;

import com.devonfw.tools.ide.commandlet.AbstractCommandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.validation.PropertyValidator;

/**
 * {@link Property} with {@link #getValueType() value type} {@link AbstractCommandlet}.
 */
public class CommandletProperty extends Property<AbstractCommandlet> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public CommandletProperty(String name, boolean required, String alias) {

    this(name, required, alias, null);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   * @param validator the {@link PropertyValidator} used to {@link #validate() validate} the {@link #getValue() value}.
   */
  public CommandletProperty(String name, boolean required, String alias, PropertyValidator<AbstractCommandlet> validator) {

    super(name, required, alias, false, validator);
  }

  @Override
  public Class<AbstractCommandlet> getValueType() {

    return AbstractCommandlet.class;
  }

  @Override
  protected String format(AbstractCommandlet valueToFormat) {

    return valueToFormat.getName();
  }

  @Override
  protected void completeValue(String arg, IdeContext context, AbstractCommandlet commandlet, CompletionCandidateCollector collector) {

    for (AbstractCommandlet cmd : context.getCommandletManager().getCommandlets()) {
      String cmdName = cmd.getName();
      if (cmdName.startsWith(arg)) {
        collector.add(cmdName, null, null, cmd);
      }
    }
  }

  @Override
  public AbstractCommandlet parse(String valueAsString, IdeContext context) {

    AbstractCommandlet commandlet = context.getCommandletManager().getCommandlet(valueAsString);
    if (commandlet == null) {
      throw new IllegalArgumentException(valueAsString);
    }
    return commandlet;
  }

}

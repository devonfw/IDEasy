package com.devonfw.tools.ide.property;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;

import java.util.function.Consumer;

/**
 * {@link Property} with {@link #getValueType() value type} {@link Commandlet}.
 */
public class CommandletProperty extends Property<Commandlet> {

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
   * @param validator the {@link Consumer} used to {@link #validate() validate} the {@link #getValue() value}.
   */
  public CommandletProperty(String name, boolean required, String alias, Consumer<Commandlet> validator) {

    super(name, required, alias, false, validator);
  }

  @Override
  public Class<Commandlet> getValueType() {

    return Commandlet.class;
  }

  @Override
  protected String format(Commandlet valueToFormat) {

    return valueToFormat.getName();
  }

  @Override
  protected void completeValue(String arg, IdeContext context, Commandlet commandlet, CompletionCandidateCollector collector) {

    for (Commandlet cmd : context.getCommandletManager().getCommandlets()) {
      String cmdName = cmd.getName();
      if (cmdName.startsWith(arg)) {
        collector.add(cmdName, null, null, cmd);
      }
    }
  }

  @Override
  public Commandlet parse(String valueAsString, IdeContext context) {

    Commandlet commandlet = context.getCommandletManager().getCommandlet(valueAsString);
    if (commandlet == null) {
      throw new IllegalArgumentException(valueAsString);
    }
    return commandlet;
  }

}

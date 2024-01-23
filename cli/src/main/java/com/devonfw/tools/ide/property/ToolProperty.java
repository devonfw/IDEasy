package com.devonfw.tools.ide.property;

import java.util.function.Consumer;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link Property} with {@link #getValueType() value type} {@link ToolCommandlet}.
 */
public class ToolProperty extends Property<ToolCommandlet> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public ToolProperty(String name, boolean required, String alias) {

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
  public ToolProperty(String name, boolean required, String alias, Consumer<ToolCommandlet> validator) {

    super(name, required, alias, validator);
  }

  @Override
  public Class<ToolCommandlet> getValueType() {

    return ToolCommandlet.class;
  }

  @Override
  protected String format(ToolCommandlet valueToFormat) {

    return valueToFormat.getName();
  }

  @Override
  public ToolCommandlet parse(String valueAsString, IdeContext context) {

    return context.getCommandletManager().getToolCommandlet(valueAsString);
  }

  @Override
  protected boolean completeValue(String arg, IdeContext context, Commandlet commandlet,
      CompletionCandidateCollector collector) {

    boolean matches = false;
    for (Commandlet cmd : context.getCommandletManager().getCommandlets()) {
      if (cmd instanceof ToolCommandlet) {
        String cmdName = cmd.getName();
        if (cmdName.startsWith(arg)) {
          collector.add(cmdName, this, commandlet);
          matches = true;
        }
      }
    }
    return matches;
  }

}

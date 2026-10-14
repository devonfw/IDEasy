package com.devonfw.tools.ide.property;

import com.devonfw.tools.ide.commandlet.AbstractCommandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.AbstractToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.validation.PropertyValidator;

/**
 * {@link Property} with {@link #getValueType() value type} {@link AbstractToolCommandlet}.
 */
public class ToolProperty extends Property<AbstractToolCommandlet> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public ToolProperty(String name, boolean required, String alias) {

    this(name, required, alias, false, null);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param multivalued the boolean flag about multiple arguments
   * @param alias the {@link #getAlias() property alias}.
   */
  public ToolProperty(String name, boolean required, boolean multivalued, String alias) {

    this(name, required, alias, multivalued, null);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   * @param multivalued the boolean flag about multiple arguments
   * @param validator the {@link PropertyValidator} used to {@link #validate() validate} the {@link #getValue() value}.
   */
  public ToolProperty(String name, boolean required, String alias, boolean multivalued, PropertyValidator<AbstractToolCommandlet> validator) {

    super(name, required, alias, multivalued, validator);
  }

  @Override
  public Class<AbstractToolCommandlet> getValueType() {

    return AbstractToolCommandlet.class;
  }

  @Override
  protected String format(AbstractToolCommandlet valueToFormat) {

    return valueToFormat.getName();
  }

  @Override
  public AbstractToolCommandlet parse(String valueAsString, IdeContext context) {

    return context.getCommandletManager().getRequiredToolCommandlet(valueAsString);
  }

  @Override
  protected void completeValue(String arg, IdeContext context, AbstractCommandlet commandlet, CompletionCandidateCollector collector) {

    for (AbstractCommandlet cmd : context.getCommandletManager().getCommandlets()) {
      if (cmd instanceof ToolCommandlet) {
        String cmdName = cmd.getName();
        if (cmdName.startsWith(arg)) {
          collector.add(cmdName, null, null, cmd);
        }
      }
    }
  }

}

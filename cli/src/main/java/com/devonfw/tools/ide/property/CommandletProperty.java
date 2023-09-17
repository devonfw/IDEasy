package com.devonfw.tools.ide.property;

import java.util.function.Consumer;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.CommandletManagerImpl;

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

    super(name, required, alias, validator);
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
  public Commandlet parse(String valueAsString) {

    // needs to be initialized before calling this...
    Commandlet commandlet = CommandletManagerImpl.of(null).getCommandlet(valueAsString);
    if (commandlet == null) {
      throw new IllegalArgumentException(valueAsString);
    }
    return commandlet;
  }

}

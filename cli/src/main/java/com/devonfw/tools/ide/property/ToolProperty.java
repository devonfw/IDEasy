package com.devonfw.tools.ide.property;

import java.util.function.Consumer;

import com.devonfw.tools.ide.commandlet.CommandletManagerImpl;
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
  public ToolCommandlet parse(String valueAsString) {

    // needs to be initialized before calling this...
    return CommandletManagerImpl.of(null).getToolCommandlet(valueAsString);
  }

}

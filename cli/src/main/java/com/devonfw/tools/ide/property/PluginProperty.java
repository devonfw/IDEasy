package com.devonfw.tools.ide.property;

import java.util.function.Consumer;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.PluginBasedCommandlet;

/**
 * {@link Property} representing the plugin of a {@link PluginBasedCommandlet}.
 */
public class PluginProperty extends Property<String> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public PluginProperty(String name, boolean required, String alias) {

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
  public PluginProperty(String name, boolean required, boolean multivalued, String alias) {

    this(name, required, alias, multivalued, null);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   * @param multivalued the boolean flag about multiple arguments
   * @param validator the {@link Consumer} used to {@link #validate() validate} the {@link #getValue() value}.
   */
  public PluginProperty(String name, boolean required, String alias, boolean multivalued, Consumer<String> validator) {

    super(name, required, alias, multivalued, validator);
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
  protected void completeValue(String arg, IdeContext context, Commandlet commandlet, CompletionCandidateCollector collector) {
    //TODO
  }

}

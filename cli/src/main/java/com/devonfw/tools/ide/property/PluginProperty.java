package com.devonfw.tools.ide.property;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.plugin.PluginBasedCommandlet;
import com.devonfw.tools.ide.tool.plugin.PluginDescriptor;
import com.devonfw.tools.ide.tool.plugin.PluginMaps;
import com.devonfw.tools.ide.validation.PropertyValidator;

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
   * @param alias the {@link #getAlias() property alias}.
   * @param multivalued the boolean flag about multiple arguments
   * @param validator the {@link PropertyValidator} used to {@link #validate() validate} the {@link #getValue() value}.
   */
  public PluginProperty(String name, boolean required, String alias, boolean multivalued, PropertyValidator<String> validator) {

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

    ToolCommandlet cmd = commandlet.getToolForCompletion();
    if (cmd instanceof PluginBasedCommandlet) {
      PluginMaps pluginMap = ((PluginBasedCommandlet) cmd).getPluginsMap();
      for (PluginDescriptor pluginDescriptor : pluginMap.getPlugins()) {
        if (pluginDescriptor.getName().toLowerCase().startsWith(arg.toLowerCase())) {
          collector.add(pluginDescriptor.getName(), null, null, commandlet);
        }
      }
    }
  }
}

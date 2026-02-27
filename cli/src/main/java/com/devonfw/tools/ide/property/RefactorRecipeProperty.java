package com.devonfw.tools.ide.property;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.plugin.PluginBasedCommandlet;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;
import com.devonfw.tools.ide.tool.plugin.ToolPlugins;
import com.devonfw.tools.ide.validation.PropertyValidator;

public class RefactorRecipeProperty extends Property<String> {

  public RefactorRecipeProperty(String name) {

    this(name, null);
  }

  public RefactorRecipeProperty(String name, PropertyValidator<String> validator) {

    super(name, true, null, true, validator);
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
    if (cmd instanceof PluginBasedCommandlet pbc) {
      ToolPlugins plugins = pbc.getPlugins();
      for (ToolPluginDescriptor pluginDescriptor : plugins.getPlugins()) {
        if (pluginDescriptor.name().toLowerCase().startsWith(arg.toLowerCase())) {
          collector.add(pluginDescriptor.name(), null, null, commandlet);
        }
      }
    }
  }

}

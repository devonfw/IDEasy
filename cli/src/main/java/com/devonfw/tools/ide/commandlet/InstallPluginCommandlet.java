package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.PluginProperty;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.plugin.PluginBasedCommandlet;

/**
 * {@link Commandlet} to install a tool.
 *
 * @see ToolCommandlet#install()
 */
public class InstallPluginCommandlet extends Commandlet {

  /** The tool to install. */
  public final ToolProperty tool;

  /** The optional version to set and install. */
  public final PluginProperty plugin;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public InstallPluginCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.tool = add(new ToolProperty("", true, "tool"));
    this.plugin = add(new PluginProperty("", true, "plugin"));
  }

  @Override
  public String getName() {

    return "install-plugin";
  }

  @Override
  protected void doRun() {
    ToolCommandlet commandlet = this.tool.getValue();
    String plugin = this.plugin.getValue();

    if (commandlet instanceof PluginBasedCommandlet cmd) {
      Step step = context.newStep("Install plugin: " + plugin);
      step.run(() -> cmd.installPlugin(cmd.getPlugin(plugin), step));
    } else {
      context.warning("Tool {} does not support installation of plugins.", commandlet.getName());
    }

  }

  @Override
  public ToolCommandlet getToolForCompletion() {

    return this.tool.getValue();
  }

}

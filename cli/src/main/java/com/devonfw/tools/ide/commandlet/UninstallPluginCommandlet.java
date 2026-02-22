package com.devonfw.tools.ide.commandlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.PluginProperty;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.plugin.PluginBasedCommandlet;

/**
 * {@link Commandlet} to install a tool.
 *
 * @see ToolCommandlet#install()
 */
public class UninstallPluginCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(UninstallPluginCommandlet.class);

  /** The tool to install. */
  public final ToolProperty tool;

  /** The optional version to set and install. */
  public final PluginProperty plugin;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public UninstallPluginCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.tool = add(new ToolProperty("", true, "tool"));
    this.plugin = add(new PluginProperty("", true, "plugin"));
  }

  @Override
  public String getName() {

    return "uninstall-plugin";
  }

  @Override
  protected void doRun() {
    ToolCommandlet commandlet = this.tool.getValue();
    String plugin = this.plugin.getValue();

    if (commandlet instanceof PluginBasedCommandlet cmd) {
      cmd.uninstallPlugin(cmd.getPlugin(plugin));
    } else {
      LOG.warn("Tool {} does not support plugins.", tool.getName());
    }
  }

  @Override
  public ToolCommandlet getToolForCompletion() {
    return this.tool.getValue();
  }
}

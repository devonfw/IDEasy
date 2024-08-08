package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.PluginProperty;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link Commandlet} to install a tool.
 *
 * @see ToolCommandlet#install()
 */
public class UninstallPluginCommandlet extends Commandlet {

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
    this.plugin = add(new PluginProperty("", false, "plugin"));
  }

  @Override
  public String getName() {

    return "uninstall-plugin";
  }

  @Override
  public void run() {
    throw new UnsupportedOperationException("Not yet implemented");
  }

}

package com.devonfw.tools.ide.tool.intellij;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeaBasedPluginInstaller;

/**
 * Plugin Installer for {@link Intellij}.
 */
public class IntellijPluginInstaller extends IdeaBasedPluginInstaller {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param commandlet the {@link IdeToolCommandlet}
   */
  public IntellijPluginInstaller(IdeContext context, IdeToolCommandlet commandlet) {
    super(context, commandlet);
  }

  @Override
  public String getMacToolApp() {

    String edition = "";
    if (commandlet.getConfiguredEdition().equals("intellij")) {
      edition = " CE";
    }
    return "IntelliJ IDEA" + edition + ".app";
  }

}

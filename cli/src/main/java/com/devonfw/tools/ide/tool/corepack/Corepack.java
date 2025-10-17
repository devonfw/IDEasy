package com.devonfw.tools.ide.tool.corepack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.npm.NpmBasedCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://www.npmjs.com/package/corepack">corepack</a>.
 */
public class Corepack extends NpmBasedCommandlet {

  private static final String COREPACK_HOME_FOLDER = "corepack";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Corepack(IdeContext context) {

    super(context, "corepack", Set.of(Tag.JAVA_SCRIPT, Tag.BUILD));
  }

  @Override
  public String getToolHelpArguments() {

    return "help";
  }

  @Override
  protected Path getToolHomePath(ToolInstallation toolInstallation) {
    return getOrCreateCorepackHomeFolder();
  }

  /**
   * @return the {@link Path} to the corepack home folder, creates the folder if it was not existing.
   */
  public Path getOrCreateCorepackHomeFolder() {
    Path confPath = this.context.getConfPath();
    Path corepackConfigFolder = confPath.resolve(COREPACK_HOME_FOLDER);
    if (!Files.isDirectory(corepackConfigFolder)) {
      this.context.getFileAccess().mkdirs(corepackConfigFolder);
    }
    return corepackConfigFolder;
  }
}

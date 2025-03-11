package com.devonfw.tools.ide.tool.npm;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.node.Node;

/**
 * {@link ToolCommandlet} for <a href="https://www.npmjs.com/">npm</a>.
 */
public class Npm extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Npm(IdeContext context) {

    super(context, "npm", Set.of(Tag.JAVA_SCRIPT, Tag.BUILD));
  }

  @Override
  protected void postExtract(Path extractedDir) {

    FileAccess fileAccess = this.context.getFileAccess();
    if (this.context.getSystemInfo().isWindows()) {
      Path nodeHomePath = this.context.getSoftwarePath().resolve("node/");
      Path npmBinBath = nodeHomePath.resolve("node_modules/npm/bin/");
      String npm = "npm";
      String npx = "npx";
      String cmd = ".cmd";

      fileAccess.delete(nodeHomePath.resolve(npm));
      fileAccess.delete(nodeHomePath.resolve(npm + cmd));
      fileAccess.delete(nodeHomePath.resolve(npx));
      fileAccess.delete(nodeHomePath.resolve(npx + cmd));

      fileAccess.copy(npmBinBath.resolve(npm), nodeHomePath);
      fileAccess.copy(npmBinBath.resolve(npm + cmd), nodeHomePath);
      fileAccess.copy(npmBinBath.resolve(npx), nodeHomePath);
      fileAccess.copy(npmBinBath.resolve(npx + cmd), nodeHomePath);
    }
  }

  @Override
  public String getToolHelpArguments() {

    return "help";
  }
}

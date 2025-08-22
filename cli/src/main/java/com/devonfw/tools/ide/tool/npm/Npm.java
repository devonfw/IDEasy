package com.devonfw.tools.ide.tool.npm;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

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
    Path nodeHomePath = this.context.getSoftwarePath().resolve("node");
    Path npmBinBath = nodeHomePath.resolve("node_modules/npm/bin");
    String npm = "npm";
    String npx = "npx";
    String cmd = ".cmd";
    if (context.getSystemInfo().isWindows()) {

      fileAccess.delete(nodeHomePath.resolve(npm));
      fileAccess.delete(nodeHomePath.resolve(npm + cmd));
      fileAccess.delete(nodeHomePath.resolve(npx));
      fileAccess.delete(nodeHomePath.resolve(npx + cmd));

      fileAccess.copy(npmBinBath.resolve(npm), nodeHomePath);
      fileAccess.copy(npmBinBath.resolve(npm + cmd), nodeHomePath);
      fileAccess.copy(npmBinBath.resolve(npx), nodeHomePath);
      fileAccess.copy(npmBinBath.resolve(npx + cmd), nodeHomePath);
    }

    if (SystemInfoImpl.INSTANCE.isMac() || SystemInfoImpl.INSTANCE.isLinux()) {
      fileAccess.delete(nodeHomePath.resolve(npm));
      fileAccess.symlink(npmBinBath.resolve(npm), nodeHomePath.resolve(npm));
      fileAccess.delete(nodeHomePath.resolve(npx));
      fileAccess.symlink(npmBinBath.resolve(npx), nodeHomePath.resolve(npx));
    }
  }

  @Override
  public String getToolHelpArguments() {

    return "help";
  }
}

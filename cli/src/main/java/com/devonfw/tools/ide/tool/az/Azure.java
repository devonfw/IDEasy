package com.devonfw.tools.ide.tool.az;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;

/**
 * {@link ToolCommandlet} for azure CLI (azure).
 */

public class Azure extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Azure(IdeContext context) {

    super(context, "az", Set.of(Tag.CLOUD));
  }

  @Override
  protected void postExtract(Path extractedDir) {

    super.postExtract(extractedDir);
    Path bin = extractedDir.resolve("bin");
    if (Files.isDirectory(bin)) {
      return;
    }
    Path wbin = extractedDir.resolve("wbin");
    if (Files.isDirectory(wbin)) {
      this.context.getFileAccess().symlink(wbin, bin);
    }
  }

  @Override
  public String getToolHelpArguments() {

    return "-h";
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, additionalInstallation);
    environmentContext.withEnvVar("AZURE_CONFIG_DIR", this.context.getConfPath().resolve(".azure").toString());
  }
}

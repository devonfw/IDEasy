package com.devonfw.tools.ide.tool.az;

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
  public void postInstall() {

    super.postInstall();
    this.context.getFileAccess().symlink(Path.of("wbin"), getToolPath().resolve("bin"));
  }

  @Override
  public String getToolHelpArguments() {

    return "-h";
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean extraInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, extraInstallation);
    environmentContext.withEnvVar("AZURE_CONFIG_DIR", this.context.getConfPath().resolve(".azure").toString());
  }
}

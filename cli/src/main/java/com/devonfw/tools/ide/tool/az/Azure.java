package com.devonfw.tools.ide.tool.az;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

import java.nio.file.Path;
import java.util.Set;

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

    EnvironmentVariables variables = this.context.getVariables();
    EnvironmentVariables typeVariables = variables.getByType(EnvironmentVariablesType.CONF);
    typeVariables.set("AZURE_CONFIG_DIR", this.context.getConfPath().resolve(".azure").toString(), true);
    typeVariables.save();
    this.context.getFileAccess().symlink(Path.of("wbin"), getToolPath().resolve("bin"));
  }

  @Override
  public String getHelpCommand() {

    return "-h";
  }
}

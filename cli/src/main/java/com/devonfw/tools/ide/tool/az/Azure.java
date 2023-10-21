package com.devonfw.tools.ide.tool.az;

import java.nio.file.Paths;
import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for azure CLI (azure).
 */

public class Azure extends ToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Azure(IdeContext context) {

    super(context, "az", Set.of(TAG_CLOUD));
  }

  @Override
  public void postInstall() {

    super.postInstall();

    EnvironmentVariables variables = this.context.getVariables();
    EnvironmentVariables typeVariables = variables.getByType(EnvironmentVariablesType.CONF);
    typeVariables.set("AZURE_CONFIG_DIR", this.context.getConfPath().resolve(".azure").toString(), true);
    typeVariables.save();
    this.context.getFileAccess().symlink(Paths.get("wbin"), this.getToolPath().resolve("bin"));
  }
}

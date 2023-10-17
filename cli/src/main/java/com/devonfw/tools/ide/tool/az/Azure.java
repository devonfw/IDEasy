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
    EnvironmentVariables settingsVariables = variables.getByType(EnvironmentVariablesType.WORKSPACE);
    // TODO oder EnvironmentVariablesType.CONF
    // TODO I think there is nothing written to a file, just the env var exported and thereby only active in current
    // session. Should this env var be written to a file?
    settingsVariables.set("AZURE_CONFIG_DIR", this.context.getConfPath().resolve(".azure").toString(), true);

    this.context.getFileAccess().symlink(Paths.get("wbin"), this.getToolPath().resolve("bin"));
  }
}

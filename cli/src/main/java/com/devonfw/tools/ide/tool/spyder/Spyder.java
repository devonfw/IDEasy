package com.devonfw.tools.ide.tool.spyder;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.pip.PipBasedIdeToolCommandlet;

/**
 * {@link PipBasedIdeToolCommandlet} for <a href="https://www.spyder-ide.org/">Spyder</a>.
 */
public class Spyder extends PipBasedIdeToolCommandlet {

  /** Environment variable that tells Spyder to use an IDEasy-managed config directory instead of the shared user config. */
  private static final String SPYDER_CONFIG_DIR = "SPYDER_CONFIG_DIR";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Spyder(IdeContext context) {
    super(context, "spyder", Set.of(Tag.SPYDER));
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {
    super.setEnvironment(environmentContext, toolInstallation, additionalInstallation);

    // Point Spyder to an IDEasy-managed config directory so its settings stay isolated per IDE_HOME.
    if (this.context.getConfPath() != null) {
      environmentContext.withEnvVar(SPYDER_CONFIG_DIR, this.context.getConfPath().resolve("spyder").toString());
    }
  }
}

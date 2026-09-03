package com.devonfw.tools.ide.tool.spyder;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.pip.PipBasedIdeToolCommandlet;

/**
 * {@link PipBasedIdeToolCommandlet} for <a href="https://www.spyder-ide.org/">Spyder</a>.
 */
public class Spyder extends PipBasedIdeToolCommandlet {

  /** The name of the Spyder config folder (defaults to <code>.spyder-py3</code> in user home). */
  private static final String SPYDER_CONFDIR_NAME = ".spyder-py3";

  /** Environment variable to override Spyder's default config directory. */
  private static final String SPYDER_CONFDIR = "SPYDER_CONFDIR";

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

    Path spyderConfig = this.context.getWorkspacePath().resolve(SPYDER_CONFDIR_NAME);
    environmentContext.withEnvVar(SPYDER_CONFDIR, spyderConfig.toString());
  }

  @Override
  protected void configureToolArgs(ProcessContext pc, ProcessMode processMode, List<String> args) {
    Path workspacePath = this.context.getWorkspacePath();
    if (workspacePath != null) {
      pc.addArg("--project");
      pc.addArg(workspacePath.toString());
    }
    super.configureToolArgs(pc, processMode, args);
  }
}

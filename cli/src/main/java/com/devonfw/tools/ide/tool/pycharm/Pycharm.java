package com.devonfw.tools.ide.tool.pycharm;

import java.nio.file.Files;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeaBasedIdeToolCommandlet;

/**
 * {@link IdeToolCommandlet} for <a href="https://www.jetbrains.com/pycharm/">Pycharm</a>.
 */
public class Pycharm extends IdeaBasedIdeToolCommandlet {

  private static final String PYCHARM = "pycharm";

  private static final String PYCHARM_EXE = PYCHARM + "64.exe";

  private static final String PYCHARM_BASH_SCRIPT = PYCHARM + ".sh";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Pycharm(IdeContext context) {

    super(context, "pycharm", Set.of(Tag.PYCHARM, Tag.PYTHON));
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean extraInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, extraInstallation);
    environmentContext.withEnvVar("PYCHARM_PROPERTIES", this.context.getWorkspacePath().resolve("pycharm.properties").toString());
  }

  @Override
  protected String getBinaryName() {

    if (this.context.getSystemInfo().isWindows()) {
      return PYCHARM_EXE;
    } else {
      if (Files.exists(this.getToolBinPath().resolve(PYCHARM))) {
        return PYCHARM;
      } else if (Files.exists(this.getToolBinPath().resolve(PYCHARM_BASH_SCRIPT))) {
        return PYCHARM_BASH_SCRIPT;
      } else {
        return PYCHARM;
      }
    }
  }
}

package com.devonfw.tools.ide.tool.intellij;

import java.nio.file.Files;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeaBasedIdeToolCommandlet;

/**
 * {@link IdeToolCommandlet} for <a href="https://www.jetbrains.com/idea/">IntelliJ</a>.
 */
public class Intellij extends IdeaBasedIdeToolCommandlet {

  private static final String IDEA = "idea";

  private static final String IDEA64_EXE = IDEA + "64.exe";

  private static final String IDEA_BASH_SCRIPT = IDEA + ".sh";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Intellij(IdeContext context) {

    super(context, "intellij", Set.of(Tag.INTELLIJ));
  }

  @Override
  protected String getBinaryName() {

    if (this.context.getSystemInfo().isWindows()) {
      return IDEA64_EXE;
    } else {
      if (Files.exists(this.getToolBinPath().resolve(IDEA))) {
        return IDEA;
      } else if (Files.exists(this.getToolBinPath().resolve(IDEA_BASH_SCRIPT))) {
        return IDEA_BASH_SCRIPT;
      } else {
        return IDEA;
      }
    }
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean extraInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, extraInstallation);
    environmentContext.withEnvVar("IDEA_PROPERTIES", this.context.getWorkspacePath().resolve("idea.properties").toString());
  }

}

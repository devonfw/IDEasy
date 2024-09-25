package com.devonfw.tools.ide.tool.intellij;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeaBasedIdeToolCommandlet;
import com.devonfw.tools.ide.tool.java.Java;

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
  protected void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean extraInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, extraInstallation);
    environmentContext.withEnvVar("IDEA_PROPERTIES", this.context.getWorkspacePath().resolve("idea.properties").toString());
  }

  @Override
  protected void installDependencies() {

    // TODO create intellij/intellij/dependencies.json file in ide-urls and delete this method
    // TODO create intellij/ultimate/dependencies.json file in ide-urls and delete this method
    getCommandlet(Java.class).install();
  }

  @Override
  protected void postExtract(Path extractedDir) {

    super.postExtract(extractedDir);
    String binaryName;
    if (this.context.getSystemInfo().isWindows()) {
      binaryName = IDEA64_EXE;
    } else if (this.context.getSystemInfo().isMac()) {
      binaryName = IDEA;
    } else {
      binaryName = IDEA_BASH_SCRIPT;
    }
    createStartScript(extractedDir, binaryName);
  }

}

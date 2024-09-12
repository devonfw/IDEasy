package com.devonfw.tools.ide.tool.intellij;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeaBasedIdeToolCommandlet;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.version.VersionIdentifier;

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
  public void runTool(ProcessMode processMode, VersionIdentifier toolVersion, String... args) {

    install(true);
    args = CliArgument.prepend(args, this.context.getWorkspacePath().toString());
    ProcessContext pc = createProcessContext(Path.of(getBinaryName()), args);
    pc.withEnvVar("IDEA_PROPERTIES", this.context.getWorkspacePath().resolve("idea.properties").toString());
    pc.run(processMode);

  }

  @Override
  public boolean install(boolean silent) {

    getCommandlet(Java.class).install();
    return super.install(silent);
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

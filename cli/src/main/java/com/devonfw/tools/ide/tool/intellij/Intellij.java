package com.devonfw.tools.ide.tool.intellij;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.os.MacOsHelper;
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
    super.runTool(ProcessMode.BACKGROUND, toolVersion, args);
  }

  @Override
  public boolean install(boolean silent) {

    getCommandlet(Java.class).install();
    return super.install(silent);
  }

  @Override
  protected void postInstall() {

    super.postInstall();
    EnvironmentVariables envVars = this.context.getVariables().getByType(EnvironmentVariablesType.CONF);
    envVars.set("IDEA_PROPERTIES", this.context.getWorkspacePath().resolve("idea.properties").toString(), true);
    envVars.save();
  }

  @Override
  protected void postExtract(Path extractedDir) {

    super.postExtract(extractedDir);
    Path binFolder = extractedDir.resolve("bin");
    if (!Files.exists(binFolder)) {
      if (this.context.getSystemInfo().isMac()) {
        MacOsHelper macOsHelper = getMacOsHelper();
        Path appDir = macOsHelper.findAppDir(extractedDir);
        binFolder = macOsHelper.findLinkDir(appDir, IDEA);
      } else {
        binFolder = extractedDir;
      }
      assert (Files.exists(binFolder));
    }
    Path bashFile = binFolder.resolve(getName());
    String bashFileContentStart = "#!/usr/bin/env bash\n\"$(dirname $0)/";
    String bashFileContentEnd = "\" $*";
    try {
      String OsType;
      if (this.context.getSystemInfo().isWindows()) {
        OsType = IDEA64_EXE;
      } else if (this.context.getSystemInfo().isMac()) {
        OsType = IDEA;
      } else {
        OsType = IDEA_BASH_SCRIPT;
      }
      Files.writeString(bashFile, bashFileContentStart + OsType + bashFileContentEnd);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    assert (Files.exists(bashFile));
    context.getFileAccess().makeExecutable(bashFile);
  }

}

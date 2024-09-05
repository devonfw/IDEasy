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
    createStartupCommandScript();
  }

  private void createStartupCommandScript() {
    this.context.getFileAccess().mkdirs(getToolPath().resolve("bin"));
    Path bashFile;
    String bashFileContentStart = "#!/usr/bin/env bash\n'";
    String bashFileContentEnd = "' $*";
    if (this.context.getSystemInfo().isMac()) {
      try {
        bashFile = Files.createFile(getToolBinPath().resolve(getName()));
        MacOsHelper macOsHelper = new MacOsHelper(context);
        Path appPath = macOsHelper.findAppDir(macOsHelper.findRootToolPath(this, context));
        this.context.getFileAccess().makeExecutable(getToolPath().resolve(appPath).resolve(IdeContext.FOLDER_CONTENTS).resolve("MacOS").resolve(IDEA));
        Files.writeString(bashFile,
            bashFileContentStart + getToolPath().resolve(appPath).resolve(IdeContext.FOLDER_CONTENTS).resolve("MacOS").resolve(IDEA) + bashFileContentEnd);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else if (this.context.getSystemInfo().isWindows()) {
      try {
        bashFile = Files.createFile(getToolBinPath().resolve(getName()));
        this.context.getFileAccess().makeExecutable(getToolBinPath().resolve(IDEA64_EXE));
        Files.writeString(bashFile,
            bashFileContentStart + getToolBinPath().resolve(IDEA64_EXE) + bashFileContentEnd);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      try {
        bashFile = Files.createFile(getToolBinPath().resolve(getName()));
        this.context.getFileAccess().makeExecutable(getToolBinPath().resolve(IDEA_BASH_SCRIPT));
        Files.writeString(bashFile,
            bashFileContentStart + getToolBinPath().resolve(IDEA_BASH_SCRIPT) + bashFileContentEnd);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    context.getFileAccess().makeExecutable(bashFile);
  }

}

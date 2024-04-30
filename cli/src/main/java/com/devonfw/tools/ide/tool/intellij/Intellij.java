package com.devonfw.tools.ide.tool.intellij;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.version.VersionIdentifier;

import java.nio.file.Path;
import java.util.Set;

/**
 * {@link IdeToolCommandlet} for <a href="https://www.jetbrains.com/idea/">IntelliJ</a>.
 */
public class Intellij extends IdeToolCommandlet {

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

    if (this.context.getSystemInfo().isMac()) {
      Path studioPath = getToolPath().resolve("Contents").resolve("MacOS").resolve(IDEA);
      args = CliArgument.prepend(args, "-na", studioPath.toString(), "--args", this.context.getWorkspacePath().toString());
    } else {
      args = CliArgument.prepend(args, this.context.getWorkspacePath().toString());
    }

    super.runTool(processMode, toolVersion, args);
  }

  @Override
  protected String getBinaryName() {

    if (this.context.getSystemInfo().isWindows()) {
      return IDEA64_EXE;
    } else if (this.context.getSystemInfo().isLinux()) {
      return IDEA_BASH_SCRIPT;
    } else {
      return "open";
    }
  }

  @Override
  public boolean install(boolean silent) {

    getCommandlet(Java.class).install();
    return super.install(silent);
  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {

  }
}

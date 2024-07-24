package com.devonfw.tools.ide.tool.intellij;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link IdeToolCommandlet} for <a href="https://www.jetbrains.com/idea/">IntelliJ</a>.
 */
public class Intellij extends IdeToolCommandlet {

  private static final String IDEA = "idea";

  private static final String IDEA64_EXE = IDEA + "64.exe";

  private static final String IDEA_BASH_SCRIPT = IDEA + ".sh";

  private final IntellijPluginManager pluginManager;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Intellij(IdeContext context) {

    super(context, "intellij", Set.of(Tag.INTELLIJ));
    this.pluginManager = new IntellijPluginManager(context, this);
  }

  @Override
  public void runTool(ProcessMode processMode, VersionIdentifier toolVersion, String... args) {

    install(true);
    args = CliArgument.prepend(args, this.context.getWorkspacePath().toString());
    super.runTool(ProcessMode.BACKGROUND, toolVersion, args);
  }

  @Override
  protected String getBinaryName() {

    Path toolBinPath = getToolBinPath();
    if (this.context.getSystemInfo().isWindows()) {
      return IDEA64_EXE;
    } else if (this.context.getSystemInfo().isLinux()) {
      return IDEA_BASH_SCRIPT;
    } else {
      return IDEA;
    }
  }

  @Override
  public boolean install(boolean silent) {

    //pluginTest();

    getCommandlet(Java.class).install();
    return super.install(silent);
  }

  private void pluginTest() {

    for (PluginDescriptor plugin : super.getPluginsMap().values()) {
      if (plugin.isActive()) {
        installPlugin(plugin);
      } else {
        handleInstall4InactivePlugin(plugin);
      }
    }
  }

  @Override
  protected void postInstall() {

    super.postInstall();
    if (this.context.getSystemInfo().isMac()) {
      setMacOsFilePermissions(getToolPath().resolve("IntelliJ IDEA" + generateMacEditionString() + ".app").resolve("Contents").resolve("MacOS").resolve(IDEA));
    }
  }

  String generateMacEditionString() {

    String edition = "";
    if (getConfiguredEdition().equals("intellij")) {
      edition = " CE";
    }
    return edition;
  }

  private void setMacOsFilePermissions(Path binaryFile) {

    if (Files.exists(binaryFile)) {
      FileAccess fileAccess = this.context.getFileAccess();
      try {
        fileAccess.makeExecutable(binaryFile);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {

    pluginManager.installPlugin(plugin);
  }

}

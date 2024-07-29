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
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import com.devonfw.tools.ide.tool.ide.PluginInstaller;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link IdeToolCommandlet} for <a href="https://www.jetbrains.com/idea/">IntelliJ</a>.
 */
public class Intellij extends IdeToolCommandlet {

  private static final String IDEA = "idea";

  private static final String IDEA64_EXE = IDEA + "64.exe";

  private static final String IDEA_BASH_SCRIPT = IDEA + ".sh";

  private static final String BUILD_FILE = "build.txt";

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

    getCommandlet(Java.class).install();
    return super.install(silent);
  }

  @Override
  protected void postInstall() {

    super.postInstall();
    EnvironmentVariables envVars = this.context.getVariables().getByType(EnvironmentVariablesType.CONF);
    envVars.set("IDEA_PROPERTIES", this.context.getWorkspacePath().resolve("idea.properties").toString(), true);
    envVars.save();
    if (this.context.getSystemInfo().isMac()) {
      setMacOsFilePermissions(getToolPath().resolve("IntelliJ IDEA" + generateMacEditionString() + ".app").resolve("Contents").resolve("MacOS").resolve(IDEA));
    }
  }

  private String generateMacEditionString() {

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

    String downloadUrl = plugin.getUrl();
    String pluginId = plugin.getId();

    String buildVersion = readBuildVersion();

    if (downloadUrl == null || downloadUrl.isEmpty()) {
      downloadUrl = String.format("https://plugins.jetbrains.com/pluginManager?action=download&id=%s&build=%s", pluginId, buildVersion);
    }

    PluginInstaller pluginInstaller = this.getPluginManager();
    pluginInstaller.installPlugin(plugin, downloadUrl);

    //pluginManager.installPlugin(plugin);
  }

  private String readBuildVersion() {
    Path buildFile = getToolPath().resolve(BUILD_FILE);
    if (context.getSystemInfo().isMac()) {
      buildFile = context.getSoftwareRepositoryPath().resolve("default").resolve("intellij/intellij").resolve(getInstalledVersion().toString())
          .resolve("IntelliJ IDEA" + generateMacEditionString() + ".app").resolve("Contents/Resources").resolve(BUILD_FILE);
    }
    try {
      return Files.readString(buildFile);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read IntelliJ build version: " + buildFile, e);
    }
  }

}

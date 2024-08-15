package com.devonfw.tools.ide.tool.intellij;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.io.FileAccessImpl;
import com.devonfw.tools.ide.os.SystemInfoImpl;
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
        Files.writeString(bashFile,
            bashFileContentStart + getToolPath().resolve("IntelliJ IDEA CE.app").resolve("Contents").resolve("MacOS").resolve(IDEA) + bashFileContentEnd);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      // Setting execute permissions is only required if executed on a real MacOS, won't work on Windows.
      if (SystemInfoImpl.INSTANCE.isMac()) {
        setMacOsFilePermissions(bashFile);
      }
    } else if (this.context.getSystemInfo().isWindows()) {
      try {
        bashFile = Files.createFile(getToolBinPath().resolve(getName()));
        Files.writeString(bashFile,
            bashFileContentStart + getToolBinPath().resolve(IDEA64_EXE) + bashFileContentEnd);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      try {
        bashFile = Files.createFile(getToolBinPath().resolve(getName()));
        Files.writeString(bashFile,
            bashFileContentStart + getToolBinPath().resolve(IDEA_BASH_SCRIPT) + bashFileContentEnd);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void setMacOsFilePermissions(Path binaryFile) {

    if (Files.exists(binaryFile)) {
      String permissionStr = FileAccessImpl.generatePermissionString(111);
      Set<PosixFilePermission> permissions = PosixFilePermissions.fromString(permissionStr);
      try {
        Files.setPosixFilePermissions(binaryFile, permissions);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {

    IntellijPluginInstaller pluginInstaller = this.getPluginInstaller();
    String downloadUrl = pluginInstaller.getDownloadUrl(plugin);
    pluginInstaller.installPlugin(plugin, downloadUrl);
  }

  @Override
  public IntellijPluginInstaller getPluginInstaller() {
    return new IntellijPluginInstaller(context, this);
  }

}

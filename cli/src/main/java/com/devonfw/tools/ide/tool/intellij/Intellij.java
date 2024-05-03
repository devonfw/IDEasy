package com.devonfw.tools.ide.tool.intellij;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccessImpl;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.version.VersionIdentifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
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

    args = CliArgument.prepend(args, this.context.getWorkspacePath().toString());
    super.runTool(processMode, toolVersion, args);
  }

  @Override
  protected String getBinaryName() {

    Path toolBinPath = getToolBinPath();
    if (this.context.getSystemInfo().isWindows()) {
      return toolBinPath.resolve(IDEA64_EXE).toString();
    } else if (this.context.getSystemInfo().isLinux()) {
      return toolBinPath.resolve(IDEA_BASH_SCRIPT).toString();
    } else {
      return getToolPath().resolve("IDEA.app").resolve("Contents").resolve("MacOS").resolve(IDEA).toString();
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
    if (this.context.getSystemInfo().isMac()) {
      setMacOsFilePermissions(getToolPath().resolve("IDEA.app").resolve("Contents").resolve("MacOS").resolve(IDEA));
    }
  }

  private static void setMacOsFilePermissions(Path binaryFile) {

    if (Files.exists(binaryFile)) {
      String permissionStr = FileAccessImpl.generatePermissionString(493);
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

  }
}

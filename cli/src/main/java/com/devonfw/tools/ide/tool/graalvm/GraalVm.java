package com.devonfw.tools.ide.tool.graalvm;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link LocalToolCommandlet} for <a href="https://www.graalvm.org/">GraalVM</a>, an advanced JDK with ahead-of-time
 * Native Image compilation.
 */
public class GraalVm extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public GraalVm(IdeContext context) {

    super(context, "graalvm", Set.of(Tag.JAVA));
  }

  @Override
  public Path getToolPath() {

    return this.context.getSoftwarePath().resolve("extra").resolve(getName());
  }

  @Override
  public VersionIdentifier getInstalledVersion() {

    return super.getInstalledVersion(getToolPath());
  }

  @Override
  public void run() {

    Path toolPath = getToolPath();
    try {
      if (!Files.exists(toolPath)) {
        Files.createDirectories(toolPath);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to create new directory:" + e);
    }

    String[] args = this.arguments.asArray();

    runTool(ProcessMode.BACKGROUND, null, args);
    runCommand(args, toolPath);
  }

  @Override
  public void runTool(ProcessMode processMode, VersionIdentifier toolVersion, String... args) {

    if (toolVersion == null) {
      install(true);
    } else {
      throw new UnsupportedOperationException("Not yet implemented!");
    }
  }

  @Override
  public void postInstall() {

    super.postInstall();
    Path devonIdeHome = this.context.getConfPath().resolve("devon.properties");
    final String graalvmExport = "export GRAALVM_HOME=" + getToolPath();

    if (!isTextInFile(devonIdeHome, graalvmExport)) {
      addTextToFile(devonIdeHome, graalvmExport);
    }
  }

  private void runCommand(String[] args, Path toolPath) {

    if (args.length > 0) {
      Path binaryPath = this.context.getUserHome().resolve(toolPath + "/bin");

      FileAccess fileAccess = this.context.getFileAccess();
      Path executableFile = fileAccess.findFirst(binaryPath,
          p -> p.getFileName().toString().startsWith(args[0] + ".exe")
              | p.getFileName().toString().startsWith(args[0] + ".cmd"),
          false);

      if (executableFile != null) {
        ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.WARNING)
            .executable(executableFile).addArgs(Arrays.copyOfRange(args, 1, args.length));
        pc.run(ProcessMode.BACKGROUND);
      } else {
        this.context.warning("Unknown command '" + args[0] + "'");
      }
    }
  }

  private boolean isTextInFile(Path path, String textToSearch) {

    try (BufferedReader br = Files.newBufferedReader(path)) {
      String line;
      while ((line = br.readLine()) != null) {
        if (line.contains(textToSearch)) {
          return true;
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(("Failed to open file for reading:" + e));
    }
    return false;
  }

  private void addTextToFile(Path path, String textToAdd) {

    try {
      Files.write(path, textToAdd.getBytes(), StandardOpenOption.APPEND);
    } catch (IOException e) {
      throw new RuntimeException("Unable to write text to file:" + e);
    }
  }

}

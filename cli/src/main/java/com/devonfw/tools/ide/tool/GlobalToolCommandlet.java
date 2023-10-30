package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.repo.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public abstract class GlobalToolCommandlet extends ToolCommandlet {
  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of}
   *        method.
   */
  public GlobalToolCommandlet(IdeContext context, String tool, Set<String> tags) {

    super(context, tool, tags);
  }

  /**
   * @return the {@link Path} where the main executable file of this tool is installed.
   */
  public Path getToolBinary() {

    String path = System.getenv("PATH");
    String[] pathDirs = path.split(File.pathSeparator);
    for (String dir : pathDirs) {
      Path toolPath = Paths.get(dir, getName());
      if (Files.isExecutable(toolPath)) {
        return toolPath;
      }
    }
    return null;
  }

  /**
   * Override this if the global tool comes with a file that has to be extracted.
   * @return {@code true} to extract (unpack) the downloaded binary file, {@code false} otherwise.
   */
  @Override
  protected boolean isExtract() {

    return true;
  }

  /**
   * Installs {@link #getName() tool}, if force mode is enabled it proceeds with the installation even if the tool
   * is already installed
   * @param silent - {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   * @return {@code true} if the tool was newly installed, {@code false} if the tool was already installed before and
   *         nothing has changed.
   */
  @Override
  protected boolean doInstall(boolean silent) {

    Path binaryPath = getToolBinary();
    if (binaryPath != null && Files.exists(binaryPath) && !this.context.isForceMode()) {
      this.context.debug("{} is already installed at {}", this.tool, binaryPath);
      return false;
    }
    String edition = getEdition();
    ToolRepository toolRepository = this.context.getDefaultToolRepository();
    VersionIdentifier configuredVersion = getConfiguredVersion();
    VersionIdentifier resolvedVersion = toolRepository.resolveVersion(this.tool, edition, configuredVersion);
    // download and install the global tool
    FileAccess fileAccess = this.context.getFileAccess();
    Path target = Path.of("C:\\Users\\saboucha\\Downloads\\devonfw-ide\\terraform-1.6.2-windows.zip");
    //Path target = toolRepository.download(this.tool, edition, resolvedVersion);
    //Path tmpPath = this.context.getTempDownloadPath().resolve(target.getFileName());
    //Path tmpPath = fileAccess.createTempDir(target.getFileName().toString());
    //this.context.getFileAccess().delete(tmpPath);
    //extract(target, tmpPath);
    Path tmpPath = this.context.getTempPath().resolve(target.getFileName());
    extract(target, tmpPath);
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.WARNING).executable(tmpPath);
    int exitCode = pc.run();
    fileAccess.delete(tmpPath);
    if (exitCode == 0) {
      this.context.success("Successfully installed {} in version {}", this.tool, resolvedVersion);
    } else {
      this.context.warning("{} in version {} was not successfully installed", this.tool, resolvedVersion);
      return false;
    }
    postInstall();
    return true;
  }



}

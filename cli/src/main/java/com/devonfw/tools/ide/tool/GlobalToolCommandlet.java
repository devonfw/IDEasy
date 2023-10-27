package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.repo.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
  @Override
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
   * @return the {@link Path} where the tool is located (installed).
   */
  @Override
  public Path getToolPath() {

    // TODO: get rootDir of global tool? is RootDir accessible from BinDir (getToolBinary())?
    return null;
  }


  /**
   * @return {@code true} to extract (unpack) the downloaded binary file, {@code false} otherwise.
   */
  @Override
  protected boolean isExtract() {

    return false;
  }

  /**
   * Installs or updates the managed {@link #getName() tool}.
   *
   * @param silent - {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   * @return {@code true} if the tool was newly installed, {@code false} if the tool was already installed before and
   *         nothing has changed.
   */
  @Override
  protected boolean doInstall(boolean silent) {

    Path binaryPath = getToolBinary();
    if (Files.exists(binaryPath) && !this.context.isForceMode()) {
      // tool already installed
      this.context.debug("{} is already installed at {}", this.tool, binaryPath);
      return false;
    }
    String edition = getEdition();
    ToolRepository toolRepository = this.context.getDefaultToolRepository();
    VersionIdentifier configuredVersion = getConfiguredVersion();
    VersionIdentifier resolvedVersion = toolRepository.resolveVersion(this.tool, edition, configuredVersion);
    // download and install the global tool
    Path target = toolRepository.download(this.tool, edition, resolvedVersion);

    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.WARNING).executable(target);
    if (pc.run() == 0) {
      this.context.success("Successfully installed {} in version {}", this.tool, resolvedVersion);
    } else {
      this.context.warning("{} in version {} was not successfully installed", this.tool, resolvedVersion);
      return false;
    }
    postInstall();
    return true;
  }



}

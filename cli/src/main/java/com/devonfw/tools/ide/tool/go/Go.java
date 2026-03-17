package com.devonfw.tools.ide.tool.go;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link ToolCommandlet} for the Go programming language.
 */
public class Go extends LocalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Go.class);

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Go(IdeContext context) {

    super(context, "go", Set.of(Tag.GO));
  }

  @Override
  public String getToolHelpArguments() {

    return "help";
  }

  @Override
  protected void performToolInstallation(ToolInstallRequest request, Path installationPath) {

    FileAccess fileAccess = this.context.getFileAccess();
    VersionIdentifier resolvedVersion = request.getRequested().getResolvedVersion();

    // Backup existing installation if present
    if (Files.isDirectory(installationPath)) {
      fileAccess.backup(installationPath);
    }

    // Download and extract the Go source archive
    Path downloadedToolFile = downloadTool(request.getRequested().getEdition().edition(), resolvedVersion);
    fileAccess.mkdirs(installationPath.getParent());
    fileAccess.extract(downloadedToolFile, installationPath, this::postExtract, true);

    // Build Go from source by running make.bash
    buildGoFromSource(installationPath);

    // Write version file
    this.context.writeVersionFile(resolvedVersion, installationPath);
    LOG.debug("Installed {} in version {} at {}", this.tool, resolvedVersion, installationPath);
  }

  /**
   * Builds Go from source by executing the make.bash script located in the go/src directory.
   *
   * @param installationPath the {@link Path} where Go source has been extracted.
   */
  private void buildGoFromSource(Path installationPath) {

    Path goSrcDir = installationPath.resolve("src");
    if (!Files.isDirectory(goSrcDir)) {
      throw new IllegalStateException("Go source directory not found at " + goSrcDir);
    }

    Path makeScript = goSrcDir.resolve("make.bash");
    if (!Files.exists(makeScript)) {
      throw new IllegalStateException("make.bash script not found at " + makeScript);
    }

    LOG.debug("Building Go from source using make.bash at {}", makeScript);

    if (this.context.getSystemInfo().isWindows()) {
      // On Windows, execute make.bash via bash since it's a POSIX shell script
      Path bash = this.context.findBashRequired();
      this.context.newProcess()
          .executable(bash)
          .addArgs("-c", "cd \"" + goSrcDir + "\" && ./make.bash")
          .run(ProcessMode.DEFAULT);
    } else {
      // On Linux/macOS, execute make.bash directly
      this.context.newProcess()
          .executable(makeScript)
          .directory(goSrcDir)
          .run(ProcessMode.DEFAULT);
    }

    LOG.debug("Go build completed successfully");
  }

}

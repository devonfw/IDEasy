package com.devonfw.tools.ide.tool.rust;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileCopyMode;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link LocalToolCommandlet} for <a href="https://www.rust-lang.org/">Rust</a>.
 */
public class Rust extends LocalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Rust.class);

  private static final String MSVC_SETUP_URL = "https://aka.ms/vs/17/release/vs_BuildTools.exe";

  private static final String WINDOWS_RUSTUP_INIT_EXE = "rustup-init.exe";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Rust(IdeContext context) {

    super(context, "rust", Set.of(Tag.RUST));
  }

  @Override
  public String getBinaryName() {

    return "rustc";
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }

  @Override
  protected boolean isExtract() {

    // The rustup installer script is an executable script and must not be extracted.
    return false;
  }

  private void installDependencies() {

    if (this.context.getSystemInfo().isWindows()) {
      installWindowsMsvcBuildTools();
    }
  }

  protected String getMsvcSetupUrl() {

    return MSVC_SETUP_URL;
  }

  protected List<String> getMsvcInstallerArgs() {

    return List.of("--quiet", "--wait", "--norestart", "--nocache", "--add", "Microsoft.VisualStudio.Workload.VCTools");
  }

  protected List<String> getRustupInstallerArgs(VersionIdentifier version) {

    return List.of("-y", "--no-modify-path", "--profile", "default", "--default-toolchain", version.toString());
  }

  private void installWindowsMsvcBuildTools() {

    FileAccess fileAccess = this.context.getFileAccess();
    Path tempDir = fileAccess.createTempDir("msvc-setup");
    Path installer = tempDir.resolve("vs_BuildTools.exe");
    fileAccess.download(getMsvcSetupUrl(), installer);

    ProcessContext process = this.context.newProcess().errorHandling(ProcessErrorHandling.THROW_CLI).executable(installer)
        .withExitCodeAcceptor(code -> (code == 0) || (code == 3010)).addArgs(getMsvcInstallerArgs());
    process.run();
  }

  @Override
  protected void performToolInstallation(ToolInstallRequest request, Path installationPath) {

    installDependencies();
    VersionIdentifier resolvedVersion = request.getRequested().getResolvedVersion();
    FileAccess fileAccess = this.context.getFileAccess();

    if (Files.isDirectory(installationPath)) {
      fileAccess.backup(installationPath);
    }
    fileAccess.mkdirs(installationPath);

    Path cargoHome = installationPath.resolve(".cargo");
    Path rustupHome = installationPath.resolve(".rustup");
    fileAccess.mkdirs(cargoHome);
    fileAccess.mkdirs(rustupHome);

    Path installerScript = downloadTool(request.getRequested().getEdition().edition(), resolvedVersion);
    if (Files.isDirectory(installerScript)) {
      // ToolRepositoryMock may provide an unpacked folder instead of a single download file.
      installerScript = installerScript.resolve("content.sh");
    }

    List<String> installerArgs = getRustupInstallerArgs(resolvedVersion);
    ProcessContext process = request.getProcessContext().createChild().errorHandling(ProcessErrorHandling.THROW_CLI).directory(installationPath)
        .withEnvVar("CARGO_HOME", cargoHome.toString()).withEnvVar("RUSTUP_HOME", rustupHome.toString());
    if (isWindowsExeInstaller(installerScript)) {
      Path installerExecutable = installerScript;
      String fileName = installerScript.getFileName().toString();
      if (!WINDOWS_RUSTUP_INIT_EXE.equalsIgnoreCase(fileName)) {
        Path canonicalInstaller = installerScript.resolveSibling(WINDOWS_RUSTUP_INIT_EXE);

        // Handle corrupted installations where rustup-init.exe might exist as a file or directory
        if (Files.exists(canonicalInstaller, LinkOption.NOFOLLOW_LINKS)) {
          LOG.info("Found existing installer at {}, checking type", canonicalInstaller);
          boolean isDirectory = Files.isDirectory(canonicalInstaller, LinkOption.NOFOLLOW_LINKS);
          LOG.info("Existing installer is {} (directory: {}), deleting it", canonicalInstaller, isDirectory);

          try {
            // First attempt: delete using fileAccess which handles files, directories, and symlinks
            fileAccess.delete(canonicalInstaller);
            LOG.debug("Successfully deleted existing installer at {}", canonicalInstaller);
          } catch (IllegalStateException e) {
            LOG.warn("First deletion attempt failed for {}, retrying: {}", canonicalInstaller, e.getMessage());
            // Retry in case of transient lock issues
            try {
              fileAccess.delete(canonicalInstaller);
              LOG.debug("Successfully deleted existing installer on retry at {}", canonicalInstaller);
            } catch (IllegalStateException e2) {
              LOG.error("Failed to delete {} after retry: {}", canonicalInstaller, e2.getMessage(), e2);
              throw new IllegalStateException("Failed to clean up corrupted installer at " + canonicalInstaller
                  + " (may be locked by another process or permission denied)", e2);
            }
          }

          // Verify deletion was successful
          if (Files.exists(canonicalInstaller, LinkOption.NOFOLLOW_LINKS)) {
            boolean stillIsDirectory = Files.isDirectory(canonicalInstaller, LinkOption.NOFOLLOW_LINKS);
            throw new IllegalStateException("Failed to delete corrupted installer at " + canonicalInstaller
                + " (is directory: " + stillIsDirectory + ", may be locked by another process)");
          }
        }

        fileAccess.copy(installerScript, canonicalInstaller, FileCopyMode.COPY_FILE_TO_TARGET_OVERRIDE);
        installerExecutable = canonicalInstaller;
      }
      process.executable(installerExecutable).addArgs(installerArgs);
    } else {
      String installerScriptArg = installerScript.toAbsolutePath().toString();
      process.executable(this.context.findBashRequired()).addArgs(installerScriptArg).addArgs(installerArgs);
    }
    process.run();

    Path cargoBin = cargoHome.resolve("bin");
    Path toolBin = installationPath.resolve("bin");
    if (Files.exists(toolBin, LinkOption.NOFOLLOW_LINKS)) {
      fileAccess.delete(toolBin);
    }
    if (Files.isDirectory(cargoBin)) {
      fileAccess.symlink(cargoBin, toolBin);
    }

    this.context.writeVersionFile(resolvedVersion, installationPath);
    LOG.debug("Installed {} in version {} at {}", this.tool, resolvedVersion, installationPath);
  }

  private boolean isWindowsExeInstaller(Path installerPath) {

    if (!this.context.getSystemInfo().isWindows()) {
      return false;
    }
    Path fileName = installerPath.getFileName();
    return (fileName != null) && fileName.toString().toLowerCase().endsWith(".exe");
  }
}

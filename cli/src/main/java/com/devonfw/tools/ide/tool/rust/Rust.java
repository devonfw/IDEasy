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
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;
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

  @Override
  protected void installDependencies(ToolInstallRequest request) {

    if (this.context.getSystemInfo().isWindows()) {
      installWindowsMsvcBuildTools();
    }
  }

  /**
   * Performs the actual installation of the tool bits.
   *
   * @param request the {@link ToolInstallRequest}.
   * @param installationPath the target {@link Path} where the tool should be installed.
   * @param installerScript the {@link Path} to the downloaded tool file.
   */
  @Override
  protected void onInstall(ToolInstallRequest request, Path installationPath, Path installerScript) {

    VersionIdentifier resolvedVersion = request.getRequested().getResolvedVersion();
    FileAccess fileAccess = this.context.getFileAccess();

    Path cargoHome = installationPath.resolve(".cargo");
    Path rustupHome = installationPath.resolve(".rustup");
    fileAccess.mkdirs(cargoHome);
    fileAccess.mkdirs(rustupHome);

    if (Files.isDirectory(installerScript)) {
      // ToolRepositoryMock may provide an unpacked folder instead of a single download file.
      installerScript = installerScript.resolve("content.sh");
    }

    ProcessContext process = request.getProcessContext().createChild().errorHandling(ProcessErrorHandling.THROW_CLI).directory(installationPath)
        .withEnvVar("CARGO_HOME", cargoHome.toString()).withEnvVar("RUSTUP_HOME", rustupHome.toString());

    List<String> installerArgs = List.of("-y", "--no-modify-path", "--profile", "default", "--default-toolchain", resolvedVersion.toString());

    if (isWindowsExeInstaller(installerScript)) {
      Path installerExecutable = prepareWindowsInstaller(fileAccess, installerScript);
      process.executable(installerExecutable).addArgs(installerArgs);
    } else {
      process.executable(this.context.findBashRequired()).addArgs(installerScript.toAbsolutePath().toString()).addArgs(installerArgs);
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
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, additionalInstallation);
    Path rootDir = toolInstallation.rootDir();
    environmentContext.withEnvVar("CARGO_HOME", rootDir.resolve(".cargo").toString());
    environmentContext.withEnvVar("RUSTUP_HOME", rootDir.resolve(".rustup").toString());
  }

  private void installWindowsMsvcBuildTools() {

    FileAccess fileAccess = this.context.getFileAccess();
    Path tempDir = fileAccess.createTempDir("msvc-setup");
    Path installer = tempDir.resolve("vs_BuildTools.exe");
    fileAccess.download(MSVC_SETUP_URL, installer);

    this.context.newProcess().errorHandling(ProcessErrorHandling.THROW_CLI).executable(installer)
        .withExitCodeAcceptor(code -> (code == 0) || (code == 3010))
        .addArgs("--quiet", "--wait", "--norestart", "--nocache", "--add", "Microsoft.VisualStudio.Workload.VCTools")
        .run();
  }

  private Path prepareWindowsInstaller(FileAccess fileAccess, Path installerScript) {

    String fileName = installerScript.getFileName().toString();
    if (WINDOWS_RUSTUP_INIT_EXE.equalsIgnoreCase(fileName)) {
      return installerScript;
    }
    Path canonicalInstaller = installerScript.resolveSibling(WINDOWS_RUSTUP_INIT_EXE);
    if (Files.exists(canonicalInstaller, LinkOption.NOFOLLOW_LINKS)) {
      LOG.info("Found existing installer at {}, checking type", canonicalInstaller);
      boolean isDirectory = Files.isDirectory(canonicalInstaller, LinkOption.NOFOLLOW_LINKS);
      LOG.info("Existing installer is {} (directory: {}), deleting it", canonicalInstaller, isDirectory);
      fileAccess.delete(canonicalInstaller);
    }
    fileAccess.copy(installerScript, canonicalInstaller, FileCopyMode.COPY_FILE_TO_TARGET_OVERRIDE);
    return canonicalInstaller;
  }

  private boolean isWindowsExeInstaller(Path installerPath) {

    if (!this.context.getSystemInfo().isWindows()) {
      return false;
    }
    Path fileName = installerPath.getFileName();
    return (fileName != null) && fileName.toString().toLowerCase().endsWith(".exe");
  }
}

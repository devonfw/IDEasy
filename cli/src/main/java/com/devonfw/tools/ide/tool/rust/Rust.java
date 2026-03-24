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
import com.devonfw.tools.ide.os.WindowsPathSyntax;
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
  protected void installDependencies() {

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

  private void installWindowsMsvcBuildTools() {

    FileAccess fileAccess = this.context.getFileAccess();
    Path tempDir = fileAccess.createTempDir("msvc-setup");
    Path installer = tempDir.resolve("vs_BuildTools.exe");
    fileAccess.download(getMsvcSetupUrl(), installer);

    ProcessContext process = this.context.newProcess().errorHandling(ProcessErrorHandling.THROW_CLI).executable(installer)
        .withExitCodeAcceptor(code -> (code.intValue() == 0) || (code.intValue() == 3010)).addArgs(getMsvcInstallerArgs());
    process.run();
  }

  @Override
  protected void performToolInstallation(ToolInstallRequest request, Path installationPath) {

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
    String installerScriptArg = installerScript.toAbsolutePath().toString();
    if (this.context.getSystemInfo().isWindows()) {
      installerScriptArg = WindowsPathSyntax.normalize(installerScriptArg, true);
    }

    ProcessContext process = request.getProcessContext().createChild().errorHandling(ProcessErrorHandling.THROW_CLI).directory(installationPath)
        .withEnvVar("CARGO_HOME", cargoHome.toString()).withEnvVar("RUSTUP_HOME", rustupHome.toString())
        .executable(this.context.findBashRequired()).addArgs(installerScriptArg, "-y", "--no-modify-path", "--profile", "default",
            "--default-toolchain", resolvedVersion.toString());
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
}

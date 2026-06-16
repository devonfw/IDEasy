package com.devonfw.tools.ide.tool.rust;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
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
  protected void installDownloadedToolPayload(ToolInstallRequest request, Path installationPath, Path installerScript) {

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

    process.executable(this.context.findBashRequired()).addArgs(installerScript.toAbsolutePath().toString()).addArgs(installerArgs);
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

}

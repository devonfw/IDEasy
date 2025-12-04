package com.devonfw.tools.ide.tool.pip;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.repository.PipRepository;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.tool.uv.Uv;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link ToolCommandlet} for <a href="https://pip.pypa.io/">pip</a>.
 * <p>
 * Pip is installed via uv using the command {@code uv pip install pip==<version>}.
 */
public class Pip extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Pip(IdeContext context) {

    super(context, "pip", Set.of(Tag.PYTHON));
  }

  @Override
  public ToolRepository getToolRepository() {

    return this.context.getPipRepository();
  }

  /**
   * @return the package name for this tool in PyPI.
   */
  public String getPackageName() {

    return this.tool;
  }

  @Override
  protected boolean isIgnoreSoftwareRepo() {

    return true;
  }

  @Override
  protected boolean isIgnoreMissingSoftwareVersionFile() {

    return true;
  }

  @Override
  protected void performToolInstallation(ToolInstallRequest request, Path installationPath) {

    VersionIdentifier resolvedVersion = request.getRequested().getResolvedVersion();

    Uv uv = this.context.getCommandletManager().getCommandlet(Uv.class);
    uv.install();

    // Install pip using uv pip install pip==<version>
    String pipPackageSpec = getPackageName() + "==" + resolvedVersion;
    this.context.info("Installing {} via uv pip install", pipPackageSpec);

    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.THROW_ERR);
    ProcessResult result = uv.runTool(pc, ProcessMode.DEFAULT, "pip", "install", pipPackageSpec);

    if (!result.isSuccessful()) {
      throw new IllegalStateException("Failed to install pip version " + resolvedVersion + " via uv");
    }

    // Create installation path and write version file
    this.context.getFileAccess().mkdirs(installationPath);
    this.context.writeVersionFile(resolvedVersion, installationPath);
    this.context.debug("Installed {} in version {} at {}", this.tool, resolvedVersion, installationPath);
  }

  @Override
  public VersionIdentifier getInstalledVersion() {

    Path toolPath = getToolPath();
    if ((toolPath == null) || !Files.isDirectory(toolPath)) {
      return null;
    }

    // Try to get version from pip --version
    try {
      ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.NONE);
      pc.executable("pip");
      pc.addArg("--version");
      ProcessResult result = pc.run(ProcessMode.DEFAULT_CAPTURE);

      if (result.isSuccessful()) {
        List<String> output = result.getOut();
        if (!output.isEmpty()) {
          // Output format: "pip 24.0 from /path/to/pip (python 3.x)"
          String versionLine = output.get(0);
          String[] parts = versionLine.split("\\s+");
          if (parts.length >= 2 && "pip".equals(parts[0])) {
            return VersionIdentifier.of(parts[1]);
          }
        }
      }
    } catch (Exception e) {
      this.context.trace("Failed to get pip version: {}", e.getMessage());
    }

    return null;
  }

  @Override
  protected void configureToolBinary(ProcessContext pc, ProcessMode processMode) {

    // pip runs through uv pip
    Uv uv = this.context.getCommandletManager().getCommandlet(Uv.class);
    uv.install();
    pc.executable(Path.of("uv"));
  }

  @Override
  protected void configureToolArgs(ProcessContext pc, ProcessMode processMode, String... args) {

    // Prepend "pip" to the arguments since we're running "uv pip <args>"
    pc.addArg("pip");
    pc.addArgs(args);
  }

}

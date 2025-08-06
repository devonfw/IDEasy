package com.devonfw.tools.ide.tool.python;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.tool.uv.Uv;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link ToolCommandlet} for <a href="https://www.python.org/">python</a>.
 */
public class Python extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Python(IdeContext context) {

    super(context, "python", Set.of(Tag.PYTHON));
  }

  /**
   * Installs {@code python} using the {@link Uv#installPython(Path, VersionIdentifier, ProcessContext)} method.
   * <p>
   * Unlike the base implementation, this method requires the {@link ProcessContext} to perform the installation logic specific to {@code python}.
   *
   * @param toolRepository the {@link ToolRepository}, unused in this implementation.
   * @param resolvedVersion the {@link VersionIdentifier} of the {@code python} tool to install.
   * @param installationPath the target {@link Path} where the tool should be installed.
   * @param fileAccess the {@link FileAccess} utility for file operations.
   * @param edition the edition of the tool to install, unused in this implementation.
   * @param processContext the {@link ProcessContext} required to install the Python environment.
   */
  @Override
  protected void performToolInstallation(ToolRepository toolRepository, VersionIdentifier resolvedVersion, Path installationPath, FileAccess fileAccess,
      String edition, ProcessContext processContext) {

    if (Files.exists(installationPath)) {
      fileAccess.backup(installationPath);
    }
    fileAccess.mkdirs(installationPath);
    Uv uv = this.context.getCommandletManager().getCommandlet(Uv.class);
    uv.installPython(installationPath, resolvedVersion, processContext);
    this.context.writeVersionFile(resolvedVersion, installationPath);
    this.context.debug("Installed {} in version {} at {}", this.tool, resolvedVersion, installationPath);
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean extraInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, extraInstallation);
    environmentContext.withEnvVar("VIRTUAL_ENV", toolInstallation.rootDir().resolve(".venv").toString());
  }


  @Override
  protected boolean isIgnoreSoftwareRepo() {

    return true;
  }
}

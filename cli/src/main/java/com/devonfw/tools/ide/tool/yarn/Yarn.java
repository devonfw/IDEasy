package com.devonfw.tools.ide.tool.yarn;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.tool.npm.NpmBasedCommandlet;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link NpmBasedCommandlet} for <a href="https://yarnpkg.com">yarn</a>.
 */
public class Yarn extends NpmBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Yarn(IdeContext context) {

    super(context, "yarn", Set.of(Tag.TYPE_SCRIPT, Tag.BUILD));
  }

  @Override
  public String getInstalledEdition() {

    if (hasNodeBinary(getName())) {
      return "yarn";
    }
    return null;
  }

  @Override
  protected void performToolInstallation(ToolRepository toolRepository, VersionIdentifier resolvedVersion, Path installationPath, String edition,
      ProcessContext processContext) {
    runPackageInstall("yarn@" + resolvedVersion);
  }

  @Override
  protected void performUninstall(Path toolPath) {
    runPackageUninstall("yarn");
  }

  @Override
  protected VersionIdentifier computeInstalledVersion() {
    if (hasNodeBinary("yarn")) {
      VersionIdentifier version = VersionIdentifier.of(this.context.newProcess().runAndGetSingleOutput("yarn", "--version"));
      this.context.debug("Determined installed version of yarn: {}", version);
      return version;
    }
    this.context.debug("Yarn is not installed yet.");
    return null;
  }
}

package com.devonfw.tools.ide.tool.yarn;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessResult;
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
    if (hasNodeBinary("corepack")) {
      runCorepack("prepare", getName() + "@" + resolvedVersion, "--activate");
      runCorepack("install", "-g", getName() + "@" + resolvedVersion);
    }
  }

  @Override
  public void uninstall() {
    if (hasNodeBinary("corepack")) {
      ProcessResult result = runCorepack("disable", "yarn");
      if (result.isSuccessful()) {
        this.context.success("Successfully uninstalled {}", this.tool);
      }
    }
  }

  @Override
  protected VersionIdentifier computeInstalledVersion() {
    return getVersion();
  }

  @Override
  public VersionIdentifier getInstalledVersion() {
    return getVersion();
  }
  
  private VersionIdentifier getVersion() {
    if (hasNodeBinary("yarn")) {
      return VersionIdentifier.of(this.context.newProcess().runAndGetSingleOutput("yarn", "--version"));
    }
    return null;
  }

}

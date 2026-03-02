package com.devonfw.tools.ide.tool.npm;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link NpmBasedCommandlet} for <a href="https://www.npmjs.com/">npm</a>.
 */
public class Npm extends NpmBasedCommandlet {

  private static final String NPM_HOME_FOLDER = "npm";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Npm(IdeContext context) {

    super(context, "npm", Set.of(Tag.JAVA_SCRIPT, Tag.BUILD));
  }

  @Override
  protected boolean canBeUninstalled() {
    return false;
  }

  @Override
  protected VersionIdentifier computeInstalledVersion() {
    if (hasNodeBinary("npm")) {
      return VersionIdentifier.of(this.context.newProcess().runAndGetSingleOutput("npm", "--version"));
    }
    return null;
  }

  @Override
  public String getToolHelpArguments() {

    return "help";
  }

  /**
   * @return the {@link Path} to the npm user configuration file, creates the folder and configuration file if it was not existing.
   */
  public Path getOrCreateNpmConfigUserConfig() {
    Path confPath = this.context.getConfPath().resolve(NPM_HOME_FOLDER);
    Path npmConfigFile = confPath.resolve(".npmrc");
    if (!Files.isDirectory(confPath)) {
      this.context.getFileAccess().mkdirs(confPath);
      this.context.getFileAccess().touch(npmConfigFile);
    }
    return npmConfigFile;
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {
    super.setEnvironment(environmentContext, toolInstallation, additionalInstallation);
    environmentContext.withEnvVar("npm_config_prefix", this.context.getSoftwarePath().resolve("node").toString());
  }
}

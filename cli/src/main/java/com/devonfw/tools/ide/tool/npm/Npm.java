package com.devonfw.tools.ide.tool.npm;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;

/**
 * {@link LocalToolCommandlet} for <a href="https://www.npmjs.org/">npm</a>.
 * <p>
 * npm is installed as a pristine, versioned installation in the software repository (same model as the other tools) and is
 * linked into each project's {@code software} folder. Global npm packages are installed into a per-project directory
 * (see {@link #NPM_GLOBAL_FOLDER}) so that projects do not interfere with each other (see <a href=
 * "https://github.com/devonfw/IDEasy/issues/352">issue #352</a> and <a href=
 * "https://github.com/devonfw/IDEasy/issues/2381">issue #2381</a>).
 */
public class Npm extends LocalToolCommandlet {

  private static final String NPM_HOME_FOLDER = "npm";

  /** File name of the {@link #findBuildDescriptor(Path) build descriptor} of an npm project. */
  private static final String PACKAGE_JSON = "package.json";

  /** The folder name for the per-project global npm packages inside {@link IdeContext#getIdeHome() IDE_HOME}. */
  public static final String NPM_GLOBAL_FOLDER = ".npm-global";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Npm(IdeContext context) {

    super(context, "npm", Set.of(Tag.JAVA_SCRIPT, Tag.BUILD));
  }

  @Override
  public String getToolHelpArguments() {

    return "help";
  }

  /**
   * Detects an npm project by its {@code package.json} build descriptor so that the {@code build} commandlet (and the
   * {@link com.devonfw.tools.ide.commandlet.BuildCommandlet BuildCommandlet}) can dispatch it to npm. npm is a standalone tool
   * (not a child of {@link com.devonfw.tools.ide.tool.node.Node node}) now, so it must declare its build descriptor itself.
   *
   * @param directory the {@link Path} to the build directory.
   * @return the {@code package.json} {@link Path} if it exists, or {@code null} otherwise.
   */
  @Override
  public Path findBuildDescriptor(Path directory) {

    Path buildDescriptor = directory.resolve(PACKAGE_JSON);
    if (Files.exists(buildDescriptor)) {
      return buildDescriptor;
    }
    return super.findBuildDescriptor(directory);
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
    // Global npm packages must not be installed into the shared node installation (issue #352) - they go into a per-project
    // directory instead so that projects do not interfere with each other. Outside of a project we do not pin the prefix so
    // that the system npm (if the tool is not installed by IDEasy) keeps its own behavior.
    Path ideHome = this.context.getIdeHome();
    if (ideHome != null) {
      Path npmGlobalPath = ideHome.resolve(NPM_GLOBAL_FOLDER);
      environmentContext.withEnvVar("npm_config_prefix", npmGlobalPath.toString());
      environmentContext.withPathEntry(npmGlobalPath.resolve(IdeContext.FOLDER_BIN));
    }
  }
}

package com.devonfw.tools.ide.tool.npm;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
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

  private static final Logger LOG = LoggerFactory.getLogger(Npm.class);

  private static final String NPM_HOME_FOLDER = "npm";

  /** The npm CLI entry point inside a flat npm installation ({@code <tool>/bin/npm-cli.js}). */
  static final String NPM_CLI_JS = "npm-cli.js";

  /** The npx CLI entry point inside a flat npm installation ({@code <tool>/bin/npx-cli.js}). */
  static final String NPX_CLI_JS = "npx-cli.js";

  /** The command name of the {@link com.devonfw.tools.ide.tool.node.Node node.js} runtime to launch. */
  static final String NODE = "node";

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

  /**
   * Repairs the npm launcher shims ({@code npm}/{@code npx}) so that they resolve to this pristine installation instead of the npm that is bundled with
   * node.
   * <p>
   * The npm registry tarball extracts a flat layout ({@code bin/npm-cli.js}) but ships the launcher shims in the layout npm uses when it is bundled inside
   * a node distribution: {@code bin/npm.cmd}, {@code bin/npx.cmd}, {@code bin/npm.ps1}, {@code bin/npx.ps1}, {@code bin/npm} and {@code bin/npx} all point
   * at a non-existent {@code node_modules/npm/bin/npm-cli.js} (or a sibling {@code node.exe}). Consequently, on Windows the {@code npm}/{@code npx} shims
   * fail with {@code MODULE_NOT_FOUND}, and on Linux they silently run the npm that is bundled with the node distribution rather than this pristine npm.
   * Since the npm bin folder is first on the PATH (see
   * {@link com.devonfw.tools.ide.common.SystemPath#getToolPathsInResolutionOrder()}), the broken shims would otherwise shadow the correct tool for every
   * invocation of {@code npm}/{@code npx}.
   * <p>
   * This hook rewrites the shims to launch this installation's own CLI entry points ({@code bin/npm-cli.js}/{@code bin/npx-cli.js}) with the {@code node}
   * runtime that is already on the PATH. This is a no-op for installations that do not contain a flat {@code bin/npm-cli.js} (e.g. a node-bundled npm or the
   * pre-seeded test fixtures) so their shims are left untouched.
   *
   * @param extractedDir the {@link Path} to the folder with the unpacked npm tool (the package root, containing {@code bin/}).
   */
  @Override
  protected void postExtract(Path extractedDir) {

    super.postExtract(extractedDir);
    Path bin = extractedDir.resolve(IdeContext.FOLDER_BIN);
    if (!Files.isRegularFile(bin.resolve(NPM_CLI_JS))) {
      return;
    }
    FileAccess fileAccess = this.context.getFileAccess();
    boolean windows = this.context.getSystemInfo().isWindows();
    repairShim(fileAccess, bin.resolve("npm.cmd"), cmdShim(NPM_CLI_JS));
    repairShim(fileAccess, bin.resolve("npx.cmd"), cmdShim(NPX_CLI_JS));
    repairShim(fileAccess, bin.resolve("npm.ps1"), psShim(NPM_CLI_JS));
    repairShim(fileAccess, bin.resolve("npx.ps1"), psShim(NPX_CLI_JS));
    if (!windows) {
      repairShim(fileAccess, bin.resolve("npm"), posixShim(NPM_CLI_JS));
      repairShim(fileAccess, bin.resolve("npx"), posixShim(NPX_CLI_JS));
    }
    LOG.debug("Repaired the npm launcher shims of the pristine npm installation at {} to use the flat layout.", bin);
  }

  /**
   * Overwrites the given launcher shim with the provided flat-layout content and, on non-Windows systems, marks it executable so that the POSIX shims
   * {@code bin/npm}/{@code bin/npx} are runnable.
   *
   * @param fileAccess the {@link FileAccess} to use.
   * @param shim the {@link Path} of the shim to repair.
   * @param content the new flat-layout content for the shim.
   */
  private void repairShim(FileAccess fileAccess, Path shim, String content) {

    fileAccess.writeFileContent(content, shim, false);
    // On non-Windows, make the POSIX shims (bin/npm, bin/npx) executable so they can be launched from the PATH.
    // The .cmd/.ps1 shims are not launched as POSIX executables and do not need the execute bit.
    if (!this.context.getSystemInfo().isWindows() && !shim.getFileName().toString().contains(".")) {
      fileAccess.makeExecutable(shim);
    }
  }

  /**
   * @param cliJs the {@link #NPM_CLI_JS npm CLI entry point} (or {@link #NPX_CLI_JS} for npx) to launch.
   * @return the {@code .cmd} (Windows) shim content for the flat npm layout.
   */
  private static String cmdShim(String cliJs) {

    return "@ECHO OFF\r\n" //
        + "SET \"NODE_CMD=" + NODE + "\"\r\n" //
        + "\"%NODE_CMD%\" \"%~dp0" + cliJs + "\" %*\r\n";
  }

  /**
   * @param cliJs the {@link #NPM_CLI_JS npm CLI entry point} (or {@link #NPX_CLI_JS} for npx) to launch.
   * @return the {@code .ps1} (PowerShell) shim content for the flat npm layout.
   */
  private static String psShim(String cliJs) {

    return "$node = \"" + NODE + "\"\r\n" //
        + "& $node (Join-Path $PSScriptRoot \"" + cliJs + "\") @args\r\n";
  }

  /**
   * @param cliJs the {@link #NPM_CLI_JS npm CLI entry point} (or {@link #NPX_CLI_JS} for npx) to launch.
   * @return the POSIX shim content for the flat npm layout.
   */
  private static String posixShim(String cliJs) {

    return "#!/usr/bin/env bash\r\n" //
        + "basedir=\"$(dirname \"$0\")\"\r\n" //
        + "exec node \"$basedir/" + cliJs + "\" \"$@\"\r\n";
  }
}

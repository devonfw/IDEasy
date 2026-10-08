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
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;

/**
 * {@link AbstractLocalToolCommandlet} for <a href="https://www.npmjs.org/">npm</a>.
 * <p>
 * npm is installed as a pristine, versioned installation in the software repository (same model as the other tools) and is linked into each project's
 * {@code software} folder. Global npm packages are installed into a per-project directory (see {@link #NPM_GLOBAL_FOLDER}) so that projects do not interfere
 * with each other (see <a href= "https://github.com/devonfw/IDEasy/issues/352">issue #352</a> and <a href=
 * "https://github.com/devonfw/IDEasy/issues/2381">issue #2381</a>). The prefix lives inside the {@code software} folder as a regular sub-folder, so the
 * executables of the globally installed packages are also picked up by the generic {@code software} PATH scan (the {@code bin} folder on POSIX, the folder
 * itself on Windows); in addition {@link #setEnvironment} adds that folder to the PATH explicitly so that packages installed within the same IDEasy run are
 * already resolvable.
 */
public class Npm extends AbstractLocalToolCommandlet {

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

  /**
   * The folder name for the per-project global npm packages, located inside the {@link IdeContext#getSoftwarePath() software} folder.
   */
  public static final String NPM_GLOBAL_FOLDER = "node_modules";

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
   * {@link com.devonfw.tools.ide.commandlet.BuildCommandlet BuildCommandlet}) can dispatch it to npm. npm is a standalone tool (not a child of
   * {@link com.devonfw.tools.ide.tool.node.Node node}) now, so it must declare its build descriptor itself.
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

  /**
   * @param context the {@link IdeContext}.
   * @return the {@link Path} to the per-project global npm prefix (see {@link #NPM_GLOBAL_FOLDER}), or {@code null} if not running inside a project.
   */
  static Path getGlobalNpmPrefix(IdeContext context) {

    Path softwarePath = context.getSoftwarePath();
    return softwarePath == null ? null : softwarePath.resolve(NPM_GLOBAL_FOLDER);
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, additionalInstallation);
    // Global npm packages must not be installed into the shared node installation (issue #352) - they go into a per-project
    // directory instead so that projects do not interfere with each other. Outside of a project we do not pin the prefix so
    // that the system npm (if the tool is not installed by IDEasy) keeps its own behavior. The folder is not created here on purpose:
    // npm creates it when the first global package is installed and IDEasy removes it again via cleanupGlobalPackagesFolder()
    // once the last package has been uninstalled.
    Path npmGlobalPath = getGlobalNpmPrefix(this.context);
    if (npmGlobalPath == null) {
      return;
    }
    environmentContext.withEnvVar("npm_config_prefix", npmGlobalPath.toString());

    // npm places the global shims in the prefix root on Windows but in <prefix>/bin on POSIX, so the PATH entry must
    // differ per platform - otherwise the globally installed packages (e.g. task, cdk) are not resolvable.
    Path npmGlobalBin = this.context.getSystemInfo().isWindows() ? npmGlobalPath : npmGlobalPath.resolve(IdeContext.FOLDER_BIN);
    environmentContext.withPathEntry(npmGlobalBin);
  }

  /**
   * Removes the per-project global npm packages folder (see {@link #NPM_GLOBAL_FOLDER}) if no global package is installed there anymore.
   * <p>
   * {@code npm uninstall -g} removes the package but leaves an empty folder behind (and possibly a {@code .package-lock.json}), so the prefix folder would
   * otherwise linger in the {@code software} folder after the last global package of a project is uninstalled. A global package is considered installed if a
   * {@code package.json} remains anywhere in the prefix - every installed package has one. This is independent of npm's platform-specific package layout
   * (Windows: {@code node_modules}, POSIX: {@code lib/node_modules}), so the whole prefix is searched. No-op if the prefix does not exist or other packages are
   * still installed.
   */
  public void cleanupGlobalPackagesFolder() {

    Path npmGlobalPath = getGlobalNpmPrefix(this.context);
    if (npmGlobalPath == null || !Files.isDirectory(npmGlobalPath)) {
      return;
    }
    boolean noPackageInstalled = this.context.getFileAccess().findFirst(npmGlobalPath, this::isPackageJson, true) == null;
    if (noPackageInstalled) {
      this.context.getFileAccess().delete(npmGlobalPath);
      LOG.info("Removed the now empty global npm packages folder {}.", npmGlobalPath);
    }
  }

  /**
   * @param path the {@link Path} to check.
   * @return {@code true} if the given path is a {@code package.json} file, i.e. the marker of an installed npm package.
   */
  private boolean isPackageJson(Path path) {

    return Files.isRegularFile(path) && path.getFileName().toString().equals(PACKAGE_JSON);
  }

  /**
   * Repairs the npm launcher shims ({@code npm}/{@code npx}) so that they resolve to this pristine installation instead of the npm that is bundled with node.
   * <p>
   * The npm registry tarball extracts a flat layout ({@code bin/npm-cli.js}) but ships the launcher shims in the layout npm uses when it is bundled inside a
   * node distribution: {@code bin/npm.cmd}, {@code bin/npx.cmd}, {@code bin/npm.ps1}, {@code bin/npx.ps1}, {@code bin/npm} and {@code bin/npx} all point at a
   * non-existent {@code node_modules/npm/bin/npm-cli.js} (or a sibling {@code node.exe}). Consequently, on Windows the {@code npm}/{@code npx} shims fail with
   * {@code MODULE_NOT_FOUND}, and on Linux they silently run the npm that is bundled with the node distribution rather than this pristine npm. Since the npm
   * bin folder is first on the PATH (see {@link com.devonfw.tools.ide.common.SystemPath#getToolPathsInResolutionOrder()}), the broken shims would otherwise
   * shadow the correct tool for every invocation of {@code npm}/{@code npx}.
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
    if (windows) {
      // The .cmd/.ps1 shims are only ever executed on Windows; on other platforms they are not run and thus left untouched.
      repairShim(fileAccess, bin.resolve("npm.cmd"), cmdShim(NPM_CLI_JS));
      repairShim(fileAccess, bin.resolve("npx.cmd"), cmdShim(NPX_CLI_JS));
      repairShim(fileAccess, bin.resolve("npm.ps1"), psShim(NPM_CLI_JS));
      repairShim(fileAccess, bin.resolve("npx.ps1"), psShim(NPX_CLI_JS));
    } else {
      // The POSIX shims are only ever executed on non-Windows systems; the .cmd/.ps1 shims are not run there and are left untouched.
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

    return "#!/usr/bin/env bash\n" //
        + "basedir=\"$(dirname \"$0\")\"\n" //
        + "exec node \"$basedir/" + cliJs + "\" \"$@\"\n";
  }
}

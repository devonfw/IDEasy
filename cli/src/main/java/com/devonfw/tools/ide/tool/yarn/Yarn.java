package com.devonfw.tools.ide.tool.yarn;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.BuildTool;
import com.devonfw.tools.ide.tool.npm.NpmBasedCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link NpmBasedCommandlet} for <a href="https://yarnpkg.com">yarn</a>.
 */
public class Yarn extends NpmBasedCommandlet implements BuildTool {

  private static final String YARN_LOCK = "yarn.lock";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Yarn(IdeContext context) {

    super(context, "yarn", Set.of(Tag.TYPE_SCRIPT, Tag.BUILD));
  }

  /**
   * Runs yarn's own entry point via the IDEasy-managed node runtime instead of the PATH {@code yarn} launcher, so that corepack installing its own {@code yarn}
   * launcher does not hijack {@code ide yarn}.
   *
   * @param pc the {@link ProcessContext}.
   * @param processMode the {@link ProcessMode}.
   */
  @Override
  protected void configureToolBinary(ProcessContext pc, ProcessMode processMode) {

    Path yarnEntry = resolveYarnEntryPoint();
    if (yarnEntry != null && Files.exists(yarnEntry)) {
      pc.executable(this.context.getPath().findBinaryPathByName("node"));
      pc.addArg(yarnEntry.toString());
    } else {
      super.configureToolBinary(pc, processMode);
    }
  }

  /**
   * @return the {@link Path} to the yarn entry point of the installed yarn package, or {@code null} if the npm global prefix is not available.
   */
  private Path resolveYarnEntryPoint() {

    Path prefix = getInstalledLocation();
    if (prefix == null) {
      return null;
    }
    String packagesFolder = this.context.getSystemInfo().isWindows() ? "node_modules" : "lib/node_modules";
    return prefix.resolve(packagesFolder).resolve("yarn").resolve("bin").resolve("yarn.js");
  }

  @Override
  public Path findBuildDescriptor(Path directory) {

    Path lockFile = directory.resolve(YARN_LOCK);
    if (!Files.exists(lockFile)) {
      return null; // if we do not find a yarn.lock file, we let npm take over the package.json
    }
    return super.findBuildDescriptor(directory);
  }

  @Override
  public VersionIdentifier getProjectVersion(Path projectPath) {

    // TODO: release not yet implemented for yarn, see #2553
    return null;
  }

  @Override
  public void setProjectVersion(Path projectPath, VersionIdentifier version) {

    // TODO: release not yet implemented for yarn, see #2553
  }

  @Override
  public ProcessResult buildAndDeploy(List<String> additionalArgs) {

    // TODO: release not yet implemented for yarn, see #2553
    return null;
  }
}

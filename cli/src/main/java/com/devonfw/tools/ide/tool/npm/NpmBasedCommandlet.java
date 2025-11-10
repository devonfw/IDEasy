package com.devonfw.tools.ide.tool.npm;

import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.node.NodeBasedCommandlet;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link LocalToolCommandlet} for tools based on <a href="https://www.npmjs.com/">npm</a>.
 */
public abstract class NpmBasedCommandlet extends NodeBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  public NpmBasedCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
  }

  @Override
  public ToolRepository getToolRepository() {

    return this.context.getNpmRepository();
  }

  @Override
  protected boolean isIgnoreMissingSoftwareVersionFile() {

    return true;
  }

  @Override
  protected VersionIdentifier computeInstalledVersion() {
    return runPackageManagerGetInstalledVersion(getPackageName());
  }

  protected VersionIdentifier runPackageManagerGetInstalledVersion(String npmPackage) {
    if (!Files.isDirectory(this.context.getSoftwarePath().resolve("node"))) {
      this.context.trace("Since node is not installed, also package {} for tool {} cannot be installed.", npmPackage, this.tool);
      return null;
    }
    ProcessResult result = runPackageManager(ProcessMode.DEFAULT_CAPTURE, ProcessErrorHandling.NONE, "list", "-g", npmPackage, "--depth=0");
    if (result.isSuccessful()) {
      List<String> versions = result.getOut();
      String parsedVersion = null;
      for (String version : versions) {
        if (version.contains(npmPackage)) {
          parsedVersion = version.replaceAll(".*" + npmPackage + "@", "");
          break;
        }
      }
      if (parsedVersion != null) {
        return VersionIdentifier.of(parsedVersion);
      }
    } else {
      this.context.debug("The npm package {} for tool {} is not installed.", npmPackage, this.tool);
    }
    return null;
  }

  @Override
  protected ProcessResult runPackageManager(ProcessMode processMode, ProcessErrorHandling errorHandling, String... args) {

    ProcessContext pc = this.context.newProcess().errorHandling(errorHandling);
    Npm npm = this.context.getCommandletManager().getCommandlet(Npm.class);

    return npm.runTool(processMode, null, pc, args);
  }

}

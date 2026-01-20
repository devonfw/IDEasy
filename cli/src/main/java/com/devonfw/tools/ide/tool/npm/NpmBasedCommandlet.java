package com.devonfw.tools.ide.tool.npm;

import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.PackageManagerRequest;
import com.devonfw.tools.ide.tool.node.NodeBasedCommandlet;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link LocalToolCommandlet} for tools based on <a href="https://www.npmjs.com/">npm</a>.
 */
public abstract class NpmBasedCommandlet extends NodeBasedCommandlet<Npm> {

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
  protected Class<Npm> getPackageManagerClass() {

    return Npm.class;
  }

  @Override
  public ToolRepository getToolRepository() {

    return this.context.getNpmRepository();
  }

  @Override
  protected VersionIdentifier computeInstalledVersion() {
    return runPackageManagerGetInstalledVersion(getPackageName());
  }

  private VersionIdentifier runPackageManagerGetInstalledVersion(String npmPackage) {
    if (!Files.isDirectory(this.context.getSoftwarePath().resolve("node"))) {
      this.context.trace("Since node is not installed, also package {} for tool {} cannot be installed.", npmPackage, this.tool);
      return null;
    }
    PackageManagerRequest request = new PackageManagerRequest("list", npmPackage).addArg("list").addArg("-g").addArg(npmPackage).addArg("--depth=0")
        .setProcessMode(ProcessMode.DEFAULT_CAPTURE);
    ProcessResult result = runPackageManager(request);
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

}

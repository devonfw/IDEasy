package com.devonfw.tools.ide.tool.pip;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.PackageManagerBasedLocalToolCommandlet;
import com.devonfw.tools.ide.tool.PackageManagerRequest;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.python.Python;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.tool.uv.Uv;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link PackageManagerBasedLocalToolCommandlet} for python tools based on <a href="https://pip.pypa.io/">pip</a>.
 * <p>
 */
public abstract class PipBasedCommandlet extends PackageManagerBasedLocalToolCommandlet<ToolCommandlet> {

  private static final String PIP_SHOW_VERSION_PREFIX = "Version:";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  public PipBasedCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
  }

  @Override
  public ToolRepository getToolRepository() {

    return this.context.getPipRepository();
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected Class<ToolCommandlet> getPackageManagerClass() {

    String edition = IdeVariables.PIP_EDITION.get(this.context);
    Class<? extends ToolCommandlet> result =
        switch (edition) {
          case "pip" -> Pip.class;
          case "uv" -> Uv.class;
          default -> {
            this.context.warning("Undefined value: PIP_EDITION={}", edition);
            yield Pip.class;
          }
        };
    return (Class) result;
  }

  @Override
  protected String appendVersion(String tool, VersionIdentifier version) {

    return tool + "==" + version;
  }

  @Override
  protected void completeRequest(PackageManagerRequest request) {

    if (request.getTool().equals("pip")) {
      // we cannot install pip with pip if pip is not already installed!
      request.setPackageManager(this.context.getCommandletManager().getCommandlet(Uv.class));
    }
    super.completeRequest(request);
  }

  @Override
  protected void completeRequestArgs(PackageManagerRequest request) {

    if (request.getPackageManager() instanceof Uv) {
      request.addArg("pip");
    }
    super.completeRequestArgs(request);
  }

  @Override
  protected VersionIdentifier computeInstalledVersion() {

    Path toolBinPath = getToolBinPath();
    if ((toolBinPath == null) || !Files.isDirectory(toolBinPath)) {
      return null;
    }
    if (this.tool.equals("pip")) {
      // if pip is not installed, we shall not call pip to find that out
      String binary = "pip";
      if (this.context.getSystemInfo().isWindows()) {
        binary = "pip.exe";
      }
      if (!Files.exists(toolBinPath.resolve(binary))) {
        return null;
      }
    }
    PackageManagerRequest request = new PackageManagerRequest("show", getPackageName()).setProcessMode(ProcessMode.DEFAULT_CAPTURE)
        .setProcessContext(this.context.newProcess().errorHandling(ProcessErrorHandling.NONE));
    ProcessResult processResult = runPackageManager(request);
    if (processResult.isSuccessful()) {
      for (String line : processResult.getOut()) {
        // we are looking for something like "Version: 25.3"
        if (line.startsWith(PIP_SHOW_VERSION_PREFIX)) {
          String version = line.substring(PIP_SHOW_VERSION_PREFIX.length()).trim();
          return VersionIdentifier.of(version);
        }
      }
    }
    this.context.debug("Could not find version from pip show output:");
    processResult.log(IdeLogLevel.DEBUG, this.context);
    return null;
  }

  @Override
  protected Python getParentTool() {

    return this.context.getCommandletManager().getCommandlet(Python.class);
  }

}

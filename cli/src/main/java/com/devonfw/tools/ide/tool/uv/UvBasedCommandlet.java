package com.devonfw.tools.ide.tool.uv;

import java.nio.file.Files;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.PackageManagerBasedLocalToolCommandlet;
import com.devonfw.tools.ide.tool.PackageManagerRequest;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link PackageManagerBasedLocalToolCommandlet} for tools that are installed via {@code uv tool install} for PyPI. Concrete tools(e.g. {@code Just}) only need
 * a constructor and to override {@link #getPackageName()} if the PyPI package name differs from the IDEasy tool name.
 */
public abstract class UvBasedCommandlet extends PackageManagerBasedLocalToolCommandlet<Uv> {

  private static final Logger LOG = LoggerFactory.getLogger(UvBasedCommandlet.class);

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  public UvBasedCommandlet(IdeContext context, String tool, Set<Tag> tags) {
    super(context, tool, tags);
  }

  @Override
  protected Class<Uv> getPackageManagerClass() {
    return Uv.class;
  }

  @Override
  public ToolRepository getToolRepository() {
    return this.context.getUvRepository();
  }

  @Override
  protected Uv getParentTool() {
    return this.context.getCommandletManager().getCommandlet(Uv.class);
  }

  @Override
  protected String appendVersion(String tool, VersionIdentifier version) {
    return tool + "@" + version;
  }

  @Override
  protected void completeRequestArgs(PackageManagerRequest request) {
    request.addArg("tool");
    super.completeRequestArgs(request);
  }

  @Override
  protected VersionIdentifier computeInstalledVersion() {
    if (!Files.isDirectory(this.context.getSoftwarePath().resolve("uv"))) {
      LOG.trace("Since uv is not installed, the tool {} cannot be installed either.", this.tool);
      return null;
    }
    String packageName = getPackageName();
    PackageManagerRequest request = new PackageManagerRequest("list", packageName).addArg("tool").addArg("list")
        .setProcessMode(ProcessMode.DEFAULT_CAPTURE);
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.NONE);
    request.setProcessContext(pc);
    ProcessResult result = runPackageManager(request, true);
    if (result.isSuccessful()) {
      String prefix = packageName + " v";
      for (String line : result.getOut()) {
        if (line.startsWith(prefix)) {
          return VersionIdentifier.of(line.substring(prefix.length()).trim());
        }
      }
    }
    LOG.debug("Could not determine installed version from 'uv tool list' output.");
    result.log(IdeLogLevel.DEBUG);
    return null;
  }
}

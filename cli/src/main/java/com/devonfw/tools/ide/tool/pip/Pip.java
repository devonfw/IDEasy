package com.devonfw.tools.ide.tool.pip;

import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.uv.Uv;

/**
 * {@link ToolCommandlet} for <a href="https://pip.pypa.io/">pip</a>.
 * <p>
 * Pip is installed via uv using the command {@code uv pip install pip==<version>}.
 */
public class Pip extends PipBasedCommandlet {

  /** The available editions of pip. */
  public static final List<String> EDITIONS = List.of("pip", "uv");

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Pip(IdeContext context) {

    super(context, "pip", Set.of(Tag.PYTHON));
  }

  /**
   * @return the package name for this tool in PyPI.
   */
  @Override
  public String getPackageName() {

    return this.tool;
  }

  @Override
  public ProcessResult runTool(ToolInstallRequest request, ProcessMode processMode, List<String> args) {

    String edition = getConfiguredEdition();
    if ("uv".equals(edition) && (request.getRequested() == null)) {
      // default is PIP_EDITION=uv in settings so "ide pip «args»" should actually do "ide uv pip «args»"
      // this is not possible if the requested edition and version have been preconfigured
      // since that means we want to use a specific version of pip while "uv" would then be installed in that version which most likely does not exist.
      args.addFirst("pip");
      return this.context.getCommandletManager().getCommandlet(Uv.class).runTool(request, processMode, args);
    } else {
      return super.runTool(request, processMode, args);
    }
  }

}

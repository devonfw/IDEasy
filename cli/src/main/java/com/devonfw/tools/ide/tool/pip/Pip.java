package com.devonfw.tools.ide.tool.pip;

import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.uv.Uv;

/**
 * {@link AbstractToolCommandlet} for <a href="https://pip.pypa.io/">pip</a>.
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
    // a version query asks for the version of pip - the "pip" sub-command of "uv" has no version flag of its own (it only accepts package sub-commands such as
    // "install"), so "ide pip --version" must not be rewritten to "uv pip --version" which uv rejects (see #2561). It is handled by pip itself (running the
    // per-project virtual environment's pip).
    if ("uv".equals(edition) && (request.getRequested() == null) && !isVersionQuery(args)) {
      // default is PIP_EDITION=uv in settings so "ide pip «args»" should actually do "ide uv pip «args»"
      // this is not possible if the requested edition and version have been preconfigured
      // since that means we want to use a specific version of pip while "uv" would then be installed in that version which most likely does not exist.
      args.addFirst("pip");
      return this.context.getCommandletManager().getCommandlet(Uv.class).runTool(request, processMode, args);
    } else {
      return super.runTool(request, processMode, args);
    }
  }

  /**
   * @param args the {@link List} of arguments the pip tool will be invoked with.
   * @return {@code true} if the arguments request the version of pip itself (i.e. the first argument is pip's version flag), so that it must be handled by pip
   *     and not by the {@code pip} sub-command of {@code uv}.
   */
  private boolean isVersionQuery(List<String> args) {

    return (args.size() > 0) && args.get(0).equals("--version");
  }

}

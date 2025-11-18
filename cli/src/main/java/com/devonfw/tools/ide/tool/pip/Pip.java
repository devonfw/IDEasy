package com.devonfw.tools.ide.tool.pip;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.DelegatingToolCommandlet;
import com.devonfw.tools.ide.tool.uv.Uv;
import com.devonfw.tools.ide.version.GenericVersionRange;

/**
 * {@link DelegatingToolCommandlet} for <a href="https://pip.pypa.io/">pip</a>.
 */
public class Pip extends DelegatingToolCommandlet<Uv> {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Pip(IdeContext context) {

    super(context, "pip", Set.of(Tag.PYTHON), Uv.class);
  }

  @Override
  public ProcessResult runTool(ProcessMode processMode, GenericVersionRange toolVersion, ProcessErrorHandling errorHandling, String... args) {

    // Delegate to "uv pip" by prepending "pip" to the arguments
    String[] uvPipArgs = new String[args.length + 1];
    uvPipArgs[0] = "pip";
    System.arraycopy(args, 0, uvPipArgs, 1, args.length);

    return getCommandlet(Uv.class).runTool(processMode, toolVersion, errorHandling, uvPipArgs);
  }
}

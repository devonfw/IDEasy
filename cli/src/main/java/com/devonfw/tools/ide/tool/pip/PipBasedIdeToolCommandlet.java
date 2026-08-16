package com.devonfw.tools.ide.tool.pip;

import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;

/**
 * Base class for pip-based IDE tools that should launch in the background instead of blocking the terminal.
 */
public abstract class PipBasedIdeToolCommandlet extends PipBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool.
   */
  public PipBasedIdeToolCommandlet(IdeContext context, String tool, Set<Tag> tags) {
    super(context, tool, tags);
  }

  @Override
  public ProcessResult runTool(List<String> args) {
    return runTool(ProcessMode.BACKGROUND, null, args);
  }
}

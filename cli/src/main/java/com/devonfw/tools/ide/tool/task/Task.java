package com.devonfw.tools.ide.tool.task;


import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.npm.NpmBasedCommandlet;

/**
 * {@link ToolCommandlet} for Task CLI.
 */
public class Task extends NpmBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Task(IdeContext context) {
    super(context, "task", Set.of(Tag.TASK));
  }

  @Override
  public String getPackageName() {
    return "@go-task/cli";
  }
}

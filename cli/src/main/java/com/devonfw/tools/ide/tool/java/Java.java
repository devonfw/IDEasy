package com.devonfw.tools.ide.tool.java;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for Java (Java Virtual Machine and Java Development Kit).
 */
public class Java extends ToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Java(IdeContext context) {

    super(context, "java");
  }

}

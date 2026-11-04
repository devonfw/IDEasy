package com.devonfw.tools.ide.tool.java;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;

/**
 * {@link AbstractToolCommandlet} for Java (Java Virtual Machine and Java Development Kit).
 */
public class Java extends AbstractLocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Java(IdeContext context) {

    super(context, "java", Set.of(Tag.JAVA, Tag.RUNTIME));
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }
}

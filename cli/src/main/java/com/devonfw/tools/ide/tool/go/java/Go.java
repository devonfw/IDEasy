package com.devonfw.tools.ide.tool.go.java;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for the Go programming language.
 */
public class Go extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Go(IdeContext context) {

    super(context, "go", Set.of(Tag.JAVA, Tag.RUNTIME));
  }

  @Override
  public String getToolHelpArguments() {

    return "help";
  }
  
}

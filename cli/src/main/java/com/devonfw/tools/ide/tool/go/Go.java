package com.devonfw.tools.ide.tool.go;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;

/**
 * {@link AbstractToolCommandlet} for the Go programming language.
 */
public class Go extends AbstractLocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Go(IdeContext context) {

    super(context, "go", Set.of(Tag.GO));
  }

  @Override
  public String getToolHelpArguments() {

    return "help";
  }
}

package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://docs.openrewrite.org/">Refactor</a>.
 */
public class RefactorCommandlet extends Commandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public RefactorCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    add(new StringProperty("recipe", true, true, ""));
  }

  @Override
  public String getName() {
    return "refactor";
  }

  @Override
  public void run() {
    //3 branches
    //1. list available recipes
    //2. execute the exact recipe
  }

  @Override
  public boolean isIdeHomeRequired() {

    return false;
  }
}

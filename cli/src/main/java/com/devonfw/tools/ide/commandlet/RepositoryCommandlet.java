package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.StringProperty;

public class RepositoryCommandlet extends Commandlet {


  private StringProperty setup;

  private StringProperty project;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public RepositoryCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.setup = add(new StringProperty("setup", true, "setup"));
    this.project = add(new StringProperty("", false, "project"));


  }

  @Override
  public String getName() {

    return "repository";
  }

  @Override
  public void run() {

  }
}

package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;

public class TestModalsCommandlet extends Commandlet {

  IdeContext context;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public TestModalsCommandlet(IdeContext context) {
    super(context);
    addKeyword("testQuestion");
    this.context = context;
  }

  @Override
  public String getName() {
    return "testQuestion";
  }

  @Override
  protected void doRun() {
    context.question("Test Question", "Yes", "No", "Maybe");
  }
}

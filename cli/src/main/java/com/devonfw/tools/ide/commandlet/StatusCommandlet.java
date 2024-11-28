package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * {@link Commandlet} to print a status report about IDEasy.
 */
public class StatusCommandlet extends Commandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public StatusCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
  }

  @Override
  public String getName() {

    return "status";
  }

  @Override
  public void run() {
  }
}

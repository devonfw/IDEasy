package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * {@link Commandlet} to update settings, software and repositories
 */
public class UpdateCommandlet extends AbstractUpdateCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public UpdateCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
  }

  @Override
  public String getName() {

    return "update";
  }

  @Override
  public void run() {

    super.run();
  }
}

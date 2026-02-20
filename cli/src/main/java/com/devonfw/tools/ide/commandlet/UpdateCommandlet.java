package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.migration.IdeMigrator;

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
  }

  @Override
  public String getName() {

    return "update";
  }

  @Override
  protected void doRun() {
    new IdeMigrator().run(this.context);
    super.run();
  }
}

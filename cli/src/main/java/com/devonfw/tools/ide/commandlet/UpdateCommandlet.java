package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.StringProperty;

/**
 * {@link Commandlet} to update settings, software and repositories
 */
public class UpdateCommandlet extends BaseCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public UpdateCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    settingsRepo = add(new StringProperty("", false, "settingsRepository"));
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
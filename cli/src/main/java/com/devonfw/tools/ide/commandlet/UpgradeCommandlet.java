package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.IdeasyCommandlet;

/**
 * {@link Commandlet} to upgrade the version of IDEasy
 */
public class UpgradeCommandlet extends Commandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public UpgradeCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
  }

  @Override
  public String getName() {

    return "upgrade";
  }

  @Override
  public void run() {

    IdeasyCommandlet ideasy = new IdeasyCommandlet(this.context);
    ideasy.install(false);
  }

}

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

    this.context.logIdeHomeAndRootStatus();
    if (this.context.isOfflineMode()) {
      this.context.warning("You have configured offline mode via CLI.");
    } else if (this.context.isOnline()) {
      this.context.success("You are online.");
    } else {
      this.context.warning("You are offline. Check your internet connection and potential proxy settings.");
    }
  }
}

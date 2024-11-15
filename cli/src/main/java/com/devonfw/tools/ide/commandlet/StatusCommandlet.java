package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.CommandletProperty;


public class StatusCommandlet extends Commandlet {

  public final CommandletProperty commandlet;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public StatusCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.commandlet = add(new CommandletProperty("", false, "commandlet"));
  }

  @Override
  public String getName() {

    return "status";
  }

  @Override
  public void run() {
  }
}

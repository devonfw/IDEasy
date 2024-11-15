package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.FlagProperty;


public class StatusCommandlet extends Commandlet {


  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public StatusCommandlet(IdeContext context) {

    super(context);
    addKeyword(new FlagProperty(getName(), true, "-s"));
  }

  @Override
  public String getName() {

    return "status";
  }

  @Override
  public void run() {
  }
}

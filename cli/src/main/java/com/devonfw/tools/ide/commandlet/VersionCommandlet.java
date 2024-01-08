package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * Prints the IDEasy version and exits
 */
public class VersionCommandlet extends Commandlet {

  public VersionCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
  }

  @Override
  public String getName() {

    return "IDEasy-version";
  }

  @Override
  public void run() {

    context.info(IdeVersion.get());
  }
}

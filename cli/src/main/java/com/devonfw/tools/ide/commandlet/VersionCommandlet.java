package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * Prints the IDEasy version and exits
 */
public class VersionCommandlet extends Commandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public VersionCommandlet(IdeContext context) {

    super(context);
    addKeyword("--version", "-v");
  }

  @Override
  public String getName() {

    return "version";
  }

  @Override
  public boolean isIdeRootRequired() {

    return false;
  }

  @Override
  public boolean isProcessableOutput() {

    return true;
  }

  @Override
  public void run() {

    this.context.level(IdeLogLevel.PROCESSABLE).log(IdeVersion.getVersionString());
  }
}

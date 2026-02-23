package com.devonfw.tools.ide.commandlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.version.IdeVersion;

/**
 * Prints the IDEasy version and exits
 */
public class VersionCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(VersionCommandlet.class);

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
  protected void doRun() {

    IdeLogLevel.PROCESSABLE.log(LOG, IdeVersion.getVersionString());
  }
}

package com.devonfw.tools.ide.context;

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.io.IdeProgressBar;
import com.devonfw.tools.ide.io.IdeProgressBarConsole;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogListenerNone;

/**
 * Default implementation of {@link IdeContext} using the console.
 */
public class IdeContextConsole extends AbstractIdeContext {

  private static final Logger LOG = LoggerFactory.getLogger(IdeContextConsole.class);

  private final Scanner scanner;

  /**
   * The constructor.
   */
  public IdeContextConsole() {
    this(new IdeStartContextImpl(IdeLogLevel.INFO, IdeLogListenerNone.INSTANCE));
  }

  /**
   * The constructor.
   *
   * @param startContext the {@link IdeStartContextImpl}.
   */
  public IdeContextConsole(IdeStartContextImpl startContext) {

    super(startContext, null);
    if (System.console() == null) {
      LOG.debug("System console not available - using System.in as fallback");
      this.scanner = new Scanner(System.in);
    } else {
      this.scanner = null;
    }
  }

  @Override
  protected String readLine() {

    if (this.scanner == null) {
      return System.console().readLine();
    } else {
      return this.scanner.nextLine();
    }
  }

  @Override
  public IdeProgressBar newProgressBar(String title, long size, String unitName, long unitSize) {

    return new IdeProgressBarConsole(getSystemInfo(), title, size, unitName, unitSize);
  }

}

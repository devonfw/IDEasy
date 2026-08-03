package com.devonfw.tools.ide.log;

import java.util.List;

import com.devonfw.tools.ide.context.IdeStartContextImpl;

/**
 * Extends {@link IdeStartContextImpl} for testing.
 */
public class IdeTestStartContext extends IdeStartContextImpl {

  private final IdeLogListenerCollector collector;

  /**
   * The constructor.
   *
   * @param logLevelConsole the {@link IdeLogLevel} acting as threshold for the console.
   */
  public IdeTestStartContext(IdeLogLevel logLevelConsole) {

    this(logLevelConsole, new IdeLogListenerCollector());
  }

  private IdeTestStartContext(IdeLogLevel logLevelConsole, IdeLogListenerCollector logListener) {

    super(logLevelConsole, logListener);
    this.collector = logListener;
  }

  /**
   * @return the {@link List} of {@link IdeLogEntry} that have been logged for test assertions.
   */
  public List<IdeLogEntry> getEntries() {

    return this.collector.getEntries();
  }
}

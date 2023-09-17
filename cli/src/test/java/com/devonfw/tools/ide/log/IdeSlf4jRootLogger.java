package com.devonfw.tools.ide.log;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link IdeLogger} that delegates to SLF4J used for testing.
 */
public class IdeSlf4jRootLogger implements IdeLogger {

  private static final IdeSlf4jRootLogger INSTANCE = new IdeSlf4jRootLogger();

  private final Map<IdeLogLevel, IdeSubLogger> loggers;

  /**
   * The constructor.
   */
  public IdeSlf4jRootLogger() {

    super();
    this.loggers = new HashMap<>();
    for (IdeLogLevel level : IdeLogLevel.values()) {
      this.loggers.put(level, new IdeSlf4jLogger(level));
    }
  }

  @Override
  public IdeSubLogger level(IdeLogLevel level) {

    return this.loggers.get(level);
  }

  /**
   * @return the singleton instance.
   */
  public static IdeSlf4jRootLogger of() {

    return INSTANCE;
  }

}

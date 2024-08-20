package com.devonfw.tools.ide.log;

import java.util.Objects;
import java.util.function.Function;

/**
 * Implementation of {@link IdeLogger}.
 */
public class IdeLoggerImpl implements IdeLogger {

  private final Function<IdeLogLevel, IdeSubLogger> loggerFactory;

  private final IdeSubLogger[] loggers;

  /**
   * @param minLogLevel the minimum enabled {@link IdeLogLevel}.
   * @param factory the factory to create active {@link IdeSubLogger} instances.
   */
  public IdeLoggerImpl(IdeLogLevel minLogLevel, Function<IdeLogLevel, IdeSubLogger> factory) {

    super();
    this.loggerFactory = factory;
    this.loggers = new IdeSubLogger[IdeLogLevel.values().length];
    setLogLevel(minLogLevel);
  }

  @Override
  public IdeSubLogger level(IdeLogLevel level) {

    IdeSubLogger logger = this.loggers[level.ordinal()];
    Objects.requireNonNull(logger);
    return logger;
  }

  /**
   * Sets the log level.
   *
   * @param logLevel {@link IdeLogLevel}
   */
  public void setLogLevel(IdeLogLevel logLevel) {

    for (IdeLogLevel level : IdeLogLevel.values()) {
      boolean enabled = level.ordinal() >= logLevel.ordinal();
      setLogLevel(level, enabled);
    }
  }

  /**
   * @param logLevel the {@link IdeLogLevel} to modify.
   * @param enabled - {@code true} to enable, {@code false} to disable.
   */
  public void setLogLevel(IdeLogLevel logLevel, boolean enabled) {

    IdeSubLogger logger;
    if (enabled) {
      logger = this.loggerFactory.apply(logLevel);
    } else {
      logger = IdeSubLoggerNone.of(logLevel);
    }
    this.loggers[logLevel.ordinal()] = logger;
  }

}

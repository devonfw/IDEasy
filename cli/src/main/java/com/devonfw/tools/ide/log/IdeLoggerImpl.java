package com.devonfw.tools.ide.log;

import java.util.Objects;
import java.util.function.Function;

/**
 * Implementation of {@link IdeLogger}.
 */
public class IdeLoggerImpl implements IdeLogger {

  private final AbstractIdeSubLogger[] loggers;

  protected final IdeLogListener listener;

  /**
   * @param minLogLevel the minimum enabled {@link IdeLogLevel}.
   * @param factory the factory to create active {@link IdeSubLogger} instances.
   */
  public IdeLoggerImpl(IdeLogLevel minLogLevel, Function<IdeLogLevel, AbstractIdeSubLogger> factory) {

    super();
    IdeLogLevel[] levels = IdeLogLevel.values();
    this.loggers = new AbstractIdeSubLogger[levels.length];
    IdeLogListener listener = null;
    for (IdeLogLevel level : levels) {
      this.loggers[level.ordinal()] = factory.apply(level);
      if (listener == null) {
        listener = this.loggers[level.ordinal()].listener;
      }
    }
    this.listener = listener;
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

    this.loggers[logLevel.ordinal()].setEnabled(enabled);
  }

  /**
   * Ensure the logging system is initialized.
   */
  public void activateLogging() {

    if (this.listener instanceof IdeLogListenerBuffer buffer) {
      // https://github.com/devonfw/IDEasy/issues/754
      buffer.flushAndDisable(this);
    }
  }

}

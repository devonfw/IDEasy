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
   * @return the previous set logLevel {@link IdeLogLevel}
   */
  public IdeLogLevel setLogLevel(IdeLogLevel logLevel) {

    IdeLogLevel previousLogLevel = null;
    for (IdeLogLevel level : IdeLogLevel.values()) {
      boolean enabled = level.ordinal() >= logLevel.ordinal();
      if ((previousLogLevel == null) && this.loggers[level.ordinal()].isEnabled()) {
        previousLogLevel = level;
      }
      setLogLevel(level, enabled);
    }
    if ((previousLogLevel == null) || (previousLogLevel.ordinal() > IdeLogLevel.INFO.ordinal())) {
      previousLogLevel = IdeLogLevel.INFO;
    }
    return previousLogLevel;
  }

  /**
   * @param colored the new {@link AbstractIdeSubLogger#isColored() colored flag}.
   */
  protected void setLogColors(boolean colored) {

    for (IdeLogLevel level : IdeLogLevel.values()) {
      this.loggers[level.ordinal()].setColored(colored);
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
      buffer.flushAndEndBuffering(this);
    }
  }

  /**
   * Disables the logging system (temporary).
   *
   * @param threshold the {@link IdeLogLevel} acting as threshold.
   * @see com.devonfw.tools.ide.context.IdeContext#runWithoutLogging(Runnable, IdeLogLevel)
   */
  public void deactivateLogging(IdeLogLevel threshold) {

    if (this.listener instanceof IdeLogListenerBuffer buffer) {
      buffer.startBuffering(threshold);
    } else {
      throw new IllegalStateException();
    }
  }

  /**
   * Internal method to set the {@link IdeLogArgFormatter}.
   *
   * @param argFormatter the {@link IdeLogArgFormatter}.
   */
  public void setArgFormatter(IdeLogArgFormatter argFormatter) {

    for (AbstractIdeSubLogger logger : this.loggers) {
      logger.setArgFormatter(argFormatter);
    }
  }

}

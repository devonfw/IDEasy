package com.devonfw.tools.ide.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

/**
 * Implementation of {@link IdeSubLogger} for testing that delegates to slf4j.
 */
public class IdeSubLoggerSlf4j extends AbstractIdeSubLogger {

  private static final Logger LOG = LoggerFactory.getLogger(IdeSubLoggerSlf4j.class);

  private final Level logLevel;

  /**
   * The constructor.
   *
   * @param level the {@link #getLevel() log-level}.
   */
  public IdeSubLoggerSlf4j(IdeLogLevel level) {
    this(level, null);
  }

  /**
   * The constructor.
   *
   * @param level the {@link #getLevel() log-level}.
   */
  public IdeSubLoggerSlf4j(IdeLogLevel level, IdeLogListener listener) {

    super(level, false, IdeLogExceptionDetails.NONE, listener);
    this.logLevel = switch (level) {
      case TRACE -> Level.TRACE;
      case DEBUG -> Level.DEBUG;
      case WARNING -> Level.WARN;
      case ERROR -> Level.ERROR;
      default -> Level.INFO;
    };
  }

  @Override
  protected void doLog(String message, Throwable error) {

    LoggingEventBuilder builder = LOG.atLevel(this.logLevel);
    String msg = message;
    if (error != null) {
      builder = builder.setCause(error);
      if (msg == null) {
        msg = error.toString();
      }
    }
    if (this.level.isCustom()) {
      msg = this.level.name() + ":" + message;
    }
    builder.log(msg);
  }

}

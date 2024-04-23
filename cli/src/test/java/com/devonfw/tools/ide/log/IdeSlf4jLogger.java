package com.devonfw.tools.ide.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

/**
 * Implementation of {@link IdeSubLogger} for testing that delegates to slf4j.
 */
public class IdeSlf4jLogger extends AbstractIdeSubLogger {

  private static final Logger LOG = LoggerFactory.getLogger(IdeSlf4jLogger.class);

  private final Level logLevel;

  /**
   * The constructor.
   *
   * @param level the {@link #getLevel() log-level}.
   */
  public IdeSlf4jLogger(IdeLogLevel level) {

    super(level);
    this.logLevel = switch (level) {
      case TRACE -> Level.TRACE;
      case DEBUG -> Level.DEBUG;
      case INFO, STEP, INTERACTION, SUCCESS -> Level.INFO;
      case WARNING -> Level.WARN;
      case ERROR -> Level.ERROR;
      default -> throw new IllegalArgumentException("" + level);
    };
  }

  @Override
  public String log(Throwable error, String message, Object... args) {

    if ((message == null) && (error != null)) {
      message = error.getMessage();
      if (message == null) {
        message = error.toString();
      }
    }
    String msg = message;
    if ((this.level == IdeLogLevel.STEP) || (this.level == IdeLogLevel.INTERACTION) || (this.level == IdeLogLevel.SUCCESS)) {
      msg = this.level.name() + ":" + message;
    }
    LoggingEventBuilder builder = LOG.atLevel(this.logLevel);
    if (error != null) {
      builder.setCause(error);
    }
    if (args == null) {
      builder.log(msg);
    } else {
      builder.log(msg, args);
    }
    if (message == null) {
      if (error == null) {
        return null;
      } else {
        return error.toString();
      }
    } else if (args == null) {
      return message;
    } else {
      return compose(message, args);
    }
  }

  @Override
  public boolean isEnabled() {

    return LOG.isEnabledForLevel(this.logLevel);
  }

}

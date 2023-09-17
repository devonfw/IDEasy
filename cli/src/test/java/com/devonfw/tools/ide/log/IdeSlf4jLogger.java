package com.devonfw.tools.ide.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

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
  public void log(String message) {

    if ((this.level == IdeLogLevel.STEP) || (this.level == IdeLogLevel.INTERACTION)
        || (this.level == IdeLogLevel.SUCCESS)) {
      message = this.level.name() + ":" + message;
    }
    LOG.atLevel(this.logLevel).log(message);
  }

  @Override
  public boolean isEnabled() {

    return LOG.isEnabledForLevel(this.logLevel);
  }

}

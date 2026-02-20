package com.devonfw.tools.ide.log;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

/**
 * {@link IdeSubLogger} that actually adapts to SLF4J that should again adapt to {@link IdeLogger} and JUL as needed.
 */
public class IdeSubLoggerAdapter implements IdeSubLogger {

  private final IdeLogLevel level;

  private final Logger LOG;

  /**
   * The constructor.
   *
   * @param level the {@link IdeLogLevel}.
   * @param logger the SLF4J {@link Logger}.
   */
  public IdeSubLoggerAdapter(IdeLogLevel level, Logger logger) {
    this.level = level;
    this.LOG = logger;
  }

  @Override
  public String log(Throwable error, String message, Object... args) {

    Marker marker = level.getSlf4jMarker();
    if (marker != null) {
      assert level.getSlf4jLevel() == Level.INFO;
      if (error == null) {
        if (Slf4jLoggerAdapter.isEmpty(args)) {
          LOG.info(marker, message);
        } else {
          LOG.info(marker, message, args);
        }
      } else if (Slf4jLoggerAdapter.isEmpty(args)) {
        LOG.info(marker, message, error);
      } else {
        LOG.info(marker, Slf4jLoggerAdapter.compose(message, args));
      }
    } else {
      LoggingEventBuilder builder = LOG.atLevel(level.getSlf4jLevel());
      if (error != null) {
        builder = builder.setCause(error);
      }
      if (Slf4jLoggerAdapter.isEmpty(args)) {
        builder.log(message);
      } else {
        builder.log(message, args);
      }
    }
    return message;
  }

  @Override
  public boolean isEnabled() {

    Level slf4jLevel = level.getSlf4jLevel();
    Marker marker = level.getSlf4jMarker();
    if (marker != null) {
      assert slf4jLevel == Level.INFO;
      return LOG.isInfoEnabled(marker);
    }
    return LOG.isEnabledForLevel(slf4jLevel);
  }

  @Override
  public IdeLogLevel getLevel() {

    return level;
  }

}

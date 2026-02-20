package com.devonfw.tools.ide.log;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;

import com.devonfw.tools.ide.context.IdeStartContextImpl;

/**
 * Implementation of {@link Logger}.
 */
public class Slf4jLoggerAdapter extends AbstractLogger {

  /** Package prefix of IDEasy: {@value} */
  public static final String IDEASY_PACKAGE_PREFIX = "com.devonfw.tools.ide.";
  private final String name;

  private final boolean internal;

  private java.util.logging.Logger julLogger;

  /**
   * The constructor.
   *
   * @param name of the logger.
   */
  public Slf4jLoggerAdapter(String name) {

    this.name = name;
    this.internal = name.startsWith(IDEASY_PACKAGE_PREFIX);
  }

  @Override
  public String getName() {

    return this.name;
  }

  private String compose(String message, Object... args) {
    if ((args == null) || args.length == 0) {
      return message;
    }
    return AbstractIdeSubLogger.compose(IdeLogArgFormatter.DEFAULT, InvalidLogMessageHandler.NONE, message, args);
  }

  @Override
  protected String getFullyQualifiedCallerName() {

    return null;
  }

  private java.util.logging.Logger getJulLogger() {

    if (this.julLogger == null) {
      IdeLogger ideLogger = IdeLogger.get();
      if (ideLogger == null) {
        return null; // initialization issue, logging happens too early
      } else {
        boolean create = false;
        if (ideLogger instanceof IdeStartContextImpl startContext) {
          boolean prod = (ideLogger.getClass() == IdeStartContextImpl.class);
          if (startContext.isWriteLogfile()) {
            create = this.internal || !prod;
          } else if (!prod) {
            create = !this.internal; // in test we create external loggers, for prod we suppress them
          }
        }
        if (create) {
          this.julLogger = java.util.logging.Logger.getLogger(name);
        }
      }
    }
    return this.julLogger;
  }

  private IdeLogger getIdeLogger() {

    if (this.internal) {
      return IdeLogger.get();
    }
    return null;
  }

  @Override
  protected void handleNormalizedLoggingCall(Level level, Marker marker, String message, Object[] args, Throwable error) {
    IdeLogLevel ideLevel = IdeLogLevel.of(level, marker);
    IdeLogger intLogger = getIdeLogger();
    if (intLogger != null) {
      intLogger.level(ideLevel).log(error, message, args);
    }
    java.util.logging.Logger extLogger = getJulLogger();
    if (extLogger != null) {
      java.util.logging.Level julLevel = ideLevel.getJulLevel();
      if (extLogger.isLoggable(julLevel)) {
        extLogger.log(julLevel, compose(message, args), error);
      }
    }
  }

  private boolean isLevelEnabled(Level level, Marker marker) {
    IdeLogLevel ideLevel = IdeLogLevel.of(level, marker);
    IdeLogger intLogger = getIdeLogger();
    if (intLogger != null) {
      if (intLogger.level(ideLevel).isEnabled()) {
        return true;
      }
    }
    java.util.logging.Logger extLogger = getJulLogger();
    if (extLogger != null) {
      return extLogger.isLoggable(ideLevel.getJulLevel());
    }
    return false;
  }

  @Override
  public boolean isTraceEnabled() {
    return isLevelEnabled(Level.TRACE, null);
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return isLevelEnabled(Level.TRACE, marker);
  }

  @Override
  public boolean isDebugEnabled() {
    return isLevelEnabled(Level.DEBUG, null);
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return isLevelEnabled(Level.DEBUG, marker);
  }

  @Override
  public boolean isInfoEnabled() {
    return isLevelEnabled(Level.INFO, null);
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return isLevelEnabled(Level.INFO, marker);
  }

  @Override
  public boolean isWarnEnabled() {
    return isLevelEnabled(Level.WARN, null);
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return isLevelEnabled(Level.WARN, marker);
  }

  @Override
  public boolean isErrorEnabled() {
    return isLevelEnabled(Level.ERROR, null);
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return isLevelEnabled(Level.ERROR, marker);
  }

}

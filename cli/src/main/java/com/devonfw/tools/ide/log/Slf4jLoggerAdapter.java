package com.devonfw.tools.ide.log;

import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * Implementation of {@link Logger}.
 */
public class Slf4jLoggerAdapter implements Logger {

  private final String name;

  private final IdeLogger ideLogger;

  private final java.util.logging.Logger julLogger;

  /**
   * The constructor.
   *
   * @param name of the logger.
   */
  public Slf4jLoggerAdapter(String name) {

    this.name = name;
    if (name.startsWith("com.devonfw.tools.ide.")) {
      this.ideLogger = IdeLogger.get();
      this.julLogger = null;
    } else {
      this.ideLogger = null;
      this.julLogger = java.util.logging.Logger.getLogger(name);
    }
  }

  @Override
  public String getName() {

    return this.name;
  }

  private String compose(String message, Object... args) {
    return AbstractIdeSubLogger.compose(IdeLogArgFormatter.DEFAULT, InvalidLogMessageHandler.NONE, message, args);
  }

  @Override
  public boolean isTraceEnabled() {
    if (this.julLogger != null) {
      return this.julLogger.isLoggable(Level.FINER);
    } else {
      return this.ideLogger.trace().isEnabled();
    }
  }

  @Override
  public void trace(String message) {
    if (this.julLogger != null) {
      this.julLogger.fine(message);
    } else {
      this.ideLogger.trace(message);
    }
  }

  @Override
  public void trace(String message, Object arg1) {
    trace(message, new Object[] { arg1 });
  }

  @Override
  public void trace(String message, Object arg1, Object arg2) {
    trace(message, new Object[] { arg1, arg2 });
  }

  @Override
  public void trace(String message, Object... args) {
    if (this.julLogger != null) {
      this.julLogger.finer(compose(message, args));
    } else {
      this.ideLogger.trace(message, args);
    }
  }

  @Override
  public void trace(String message, Throwable error) {
    if (this.julLogger != null) {
      this.julLogger.log(Level.FINER, message, error);
    } else {
      this.ideLogger.level(IdeLogLevel.TRACE).log(error, message);
    }
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return isTraceEnabled();
  }

  @Override
  public void trace(Marker marker, String message) {
    trace(message);
  }

  @Override
  public void trace(Marker marker, String message, Object arg1) {
    trace(message, arg1);
  }

  @Override
  public void trace(Marker marker, String message, Object arg1, Object arg2) {
    trace(message, arg1, arg2);
  }

  @Override
  public void trace(Marker marker, String message, Object... args) {
    trace(message, args);
  }

  @Override
  public void trace(Marker marker, String message, Throwable error) {
    trace(message, error);
  }

  @Override
  public boolean isDebugEnabled() {
    if (this.julLogger != null) {
      return this.julLogger.isLoggable(Level.FINE);
    } else {
      return this.ideLogger.debug().isEnabled();
    }
  }

  @Override
  public void debug(String message) {
    if (this.julLogger != null) {
      this.julLogger.fine(message);
    } else {
      this.ideLogger.debug(message);
    }
  }

  @Override
  public void debug(String message, Object arg1) {
    debug(message, new Object[] { arg1 });
  }

  @Override
  public void debug(String message, Object arg1, Object arg2) {
    debug(message, new Object[] { arg1, arg2 });
  }

  @Override
  public void debug(String message, Object... args) {
    if (this.julLogger != null) {
      this.julLogger.fine(compose(message, args));
    } else {
      this.ideLogger.debug(message, args);
    }
  }

  @Override
  public void debug(String message, Throwable error) {
    if (this.julLogger != null) {
      this.julLogger.log(Level.FINE, message, error);
    } else {
      this.ideLogger.level(IdeLogLevel.DEBUG).log(error, message);
    }
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return isDebugEnabled();
  }

  @Override
  public void debug(Marker marker, String message) {
    debug(message);
  }

  @Override
  public void debug(Marker marker, String message, Object arg1) {
    debug(message, arg1);
  }

  @Override
  public void debug(Marker marker, String message, Object arg1, Object arg2) {
    debug(message, arg1, arg2);
  }

  @Override
  public void debug(Marker marker, String message, Object... args) {
    debug(message, args);
  }

  @Override
  public void debug(Marker marker, String message, Throwable error) {
    debug(message, error);
  }

  @Override
  public boolean isInfoEnabled() {
    if (this.julLogger != null) {
      return this.julLogger.isLoggable(Level.INFO);
    } else {
      return this.ideLogger.info().isEnabled();
    }
  }

  @Override
  public void info(String message) {
    if (this.julLogger != null) {
      this.julLogger.info(message);
    } else {
      this.ideLogger.info(message);
    }
  }

  @Override
  public void info(String message, Object arg1) {
    info(message, new Object[] { arg1 });
  }

  @Override
  public void info(String message, Object arg1, Object arg2) {
    info(message, new Object[] { arg1, arg2 });
  }

  @Override
  public void info(String message, Object... args) {
    if (this.julLogger != null) {
      this.julLogger.info(compose(message, args));
    } else {
      this.ideLogger.info(message, args);
    }
  }

  @Override
  public void info(String message, Throwable error) {
    if (this.julLogger != null) {
      this.julLogger.log(Level.INFO, message, error);
    } else {
      this.ideLogger.level(IdeLogLevel.INFO).log(error, message);
    }
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    if (this.julLogger != null) {
      return isInfoEnabled();
    } else {
      return this.ideLogger.level(Markers.getLevel(marker)).isEnabled();
    }
  }

  @Override
  public void info(Marker marker, String message) {
    if (this.julLogger != null) {
      this.julLogger.info(message);
    } else {
      this.ideLogger.level(Markers.getLevel(marker)).log(message);
    }
  }

  @Override
  public void info(Marker marker, String message, Object arg1) {
    info(marker, message, new Object[] { arg1 });
  }

  @Override
  public void info(Marker marker, String message, Object arg1, Object arg2) {
    info(marker, message, new Object[] { arg1, arg2 });
  }

  @Override
  public void info(Marker marker, String message, Object... args) {
    if (this.julLogger != null) {
      this.julLogger.info(compose(message, args));
    } else {
      this.ideLogger.level(Markers.getLevel(marker)).log(message, args);
    }
  }

  @Override
  public void info(Marker marker, String message, Throwable error) {
    if (this.julLogger != null) {
      this.julLogger.log(Level.INFO, message, error);
    } else {
      this.ideLogger.level(Markers.getLevel(marker)).log(error, message);
    }
  }

  @Override
  public boolean isWarnEnabled() {
    if (this.julLogger != null) {
      return this.julLogger.isLoggable(Level.WARNING);
    } else {
      return this.ideLogger.warning().isEnabled();
    }
  }

  @Override
  public void warn(String message) {
    if (this.julLogger != null) {
      this.julLogger.warning(message);
    } else {
      this.ideLogger.warning(message);
    }
  }

  @Override
  public void warn(String message, Object arg1) {
    warn(message, new Object[] { arg1 });
  }

  @Override
  public void warn(String message, Object arg1, Object arg2) {
    warn(message, new Object[] { arg1, arg2 });
  }

  @Override
  public void warn(String message, Object... args) {
    if (this.julLogger != null) {
      this.julLogger.warning(compose(message, args));
    } else {
      this.ideLogger.warning(message, args);
    }
  }

  @Override
  public void warn(String message, Throwable error) {
    if (this.julLogger != null) {
      this.julLogger.log(Level.WARNING, message, error);
    } else {
      this.ideLogger.level(IdeLogLevel.WARNING).log(error, message);
    }
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return isWarnEnabled();
  }

  @Override
  public void warn(Marker marker, String message) {
    warn(message);
  }

  @Override
  public void warn(Marker marker, String message, Object arg1) {
    warn(message, arg1);
  }

  @Override
  public void warn(Marker marker, String message, Object arg1, Object arg2) {
    warn(message, arg1, arg2);
  }

  @Override
  public void warn(Marker marker, String message, Object... args) {
    warn(message, args);
  }

  @Override
  public void warn(Marker marker, String message, Throwable error) {
    warn(message, error);
  }

  @Override
  public boolean isErrorEnabled() {
    if (this.julLogger != null) {
      return this.julLogger.isLoggable(Level.SEVERE);
    } else {
      return this.ideLogger.error().isEnabled();
    }
  }

  @Override
  public void error(String message) {
    if (this.julLogger != null) {
      this.julLogger.severe(message);
    } else {
      this.ideLogger.error(message);
    }
  }

  @Override
  public void error(String message, Object arg1) {
    error(message, new Object[] { arg1 });
  }

  @Override
  public void error(String message, Object arg1, Object arg2) {
    error(message, new Object[] { arg1, arg2 });
  }

  @Override
  public void error(String message, Object... args) {
    if (this.julLogger != null) {
      this.julLogger.severe(compose(message, args));
    } else {
      this.ideLogger.error(message, args);
    }
  }

  @Override
  public void error(String message, Throwable error) {
    if (this.julLogger != null) {
      this.julLogger.log(Level.SEVERE, message, error);
    } else {
      this.ideLogger.error(error, message);
    }
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return isErrorEnabled();
  }

  @Override
  public void error(Marker marker, String message) {
    error(message);
  }

  @Override
  public void error(Marker marker, String message, Object arg1) {
    error(message, arg1);
  }

  @Override
  public void error(Marker marker, String message, Object arg1, Object arg2) {
    error(message, arg1, arg2);
  }

  @Override
  public void error(Marker marker, String message, Object... args) {
    error(message, args);
  }

  @Override
  public void error(Marker marker, String message, Throwable error) {
    error(message, error);
  }
}

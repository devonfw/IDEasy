package com.devonfw.tools.ide.log;

import java.util.List;
import java.util.logging.Logger;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.event.LoggingEvent;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.spi.LoggingEventAware;

import com.devonfw.tools.ide.context.IdeStartContextImpl;

/**
 * Implementation of SLF4J {@link AbstractLogger} for IDEasy.
 */
public class Slf4jLoggerAdapter extends AbstractLogger implements LoggingEventAware {

  /** Package prefix of IDEasy: {@value} */
  public static final String IDEASY_PACKAGE_PREFIX = "com.devonfw.tools.ide.";
  private final String name;

  private final boolean internal;

  private Logger julLogger;

  /**
   * The constructor.
   *
   * @param name of the logger.
   */
  public Slf4jLoggerAdapter(String name) {

    this.name = name;
    this.internal = name.startsWith(IDEASY_PACKAGE_PREFIX);
    this.julLogger = Logger.getLogger(name);
  }

  @Override
  public String getName() {

    return this.name;
  }

  static boolean isEmpty(Object[] args) {
    return (args == null) || (args.length == 0);
  }

  /**
   * Should only be used internally by logger implementation.
   *
   * @param message the message template.
   * @param args the dynamic arguments to fill in.
   * @return the resolved message with the parameters filled in.
   */
  private String compose(IdeLogArgFormatter formatter, String message, Object... args) {

    if (isEmpty(args)) {
      return message;
    }
    int pos = message.indexOf("{}");
    if (pos < 0) {
      if (args.length > 0) {
        invalidMessage(message, false, args);
      }
      return message;
    }
    int argIndex = 0;
    int start = 0;
    int length = message.length();
    StringBuilder sb = new StringBuilder(length + 48);
    while (pos >= 0) {
      sb.append(message, start, pos);
      sb.append(formatter.formatArgument(args[argIndex++]));
      start = pos + 2;
      pos = message.indexOf("{}", start);
      if ((argIndex >= args.length) && (pos > 0)) {
        invalidMessage(message, true, args);
        pos = -1;
      }
    }
    if (start < length) {
      String rest = message.substring(start);
      sb.append(rest);
    }
    if (argIndex < args.length) {
      invalidMessage(message, false, args);
    }
    return sb.toString();
  }

  private void invalidMessage(String message, boolean more, Object[] args) {

    warning("Invalid log message with " + args.length + " argument(s) but " + (more ? "more" : "less")
        + " placeholders: " + message);
  }

  private void warning(String message) {

    boolean colored = isColored();
    if (colored) {
      System.err.print(IdeLogLevel.ERROR.getEndColor());
      System.err.print(IdeLogLevel.ERROR.getStartColor());
    }
    System.err.println(message);
    if (colored) {
      System.err.print(IdeLogLevel.ERROR.getEndColor());
    }
  }

  static boolean isColored() {
    IdeStartContextImpl startContext = IdeStartContextImpl.get();
    if (startContext != null) {
      return !startContext.isNoColorsMode();
    }
    return false;
  }

  @Override
  protected String getFullyQualifiedCallerName() {

    return null;
  }

  @Override
  protected void handleNormalizedLoggingCall(Level level, Marker marker, String message, Object[] args, Throwable error) {
    IdeLogLevel ideLevel = IdeLogLevel.of(level, marker);
    IdeLogListener listener = IdeLogListenerNone.INSTANCE;
    IdeStartContextImpl startContext = IdeStartContextImpl.get();
    IdeLogArgFormatter argFormatter = IdeLogArgFormatter.DEFAULT;
    if (startContext != null) {
      listener = startContext.getLogListener();
      argFormatter = startContext.getArgFormatter();
    }
    String composedMessage = compose(argFormatter, message, args);
    boolean accept = listener.onLog(ideLevel, composedMessage, message, args, error);
    if (accept) {
      java.util.logging.Level julLevel = ideLevel.getJulLevel();
      this.julLogger.log(julLevel, composedMessage, error);
    }
  }

  @Override
  public void log(LoggingEvent event) {

    List<Marker> markers = event.getMarkers();
    Marker marker = null;
    if ((markers != null) && !markers.isEmpty()) {
      assert markers.size() == 1;
      marker = markers.getFirst();
    }
    handleNormalizedLoggingCall(event.getLevel(), marker, event.getMessage(), event.getArgumentArray(), event.getThrowable());
  }

  private boolean isLevelEnabled(Level level, Marker marker) {
    IdeLogLevel ideLevel = IdeLogLevel.of(level, marker);
    return ideLevel.isEnabled();
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

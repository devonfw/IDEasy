package com.devonfw.tools.ide.log;

import java.io.PrintStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.devonfw.tools.ide.context.IdeStartContextImpl;

/**
 * Custom {@link Handler} for java.util.logging to log to console.
 */
public class JulConsoleHandler extends Handler {

  @Override
  public void publish(LogRecord record) {
    Level julLevel = record.getLevel();
    IdeLogLevel ideLevel = IdeLogLevel.of(julLevel);
    IdeStartContextImpl startContext = IdeStartContextImpl.get();
    boolean colored = false;
    if (startContext != null) {
      colored = !startContext.isNoColorsMode();
      if (ideLevel.ordinal() < startContext.getLogLevelConsole().ordinal()) {
        return; // console logging disabled for ideLevel
      }
    }
    PrintStream out = System.out;
    if (ideLevel == IdeLogLevel.ERROR) {
      out = System.err;
    }
    String message = record.getMessage();
    Throwable error = record.getThrown();
    String startColor = null;
    if (colored) {
      startColor = ideLevel.getStartColor();
      if (startColor != null) {
        out.append(startColor);
      }
    }
    if (error != null) {
      message = IdeLogExceptionDetails.of(ideLevel, IdeLogLevel.getLogLevel()).format(message, error);
    }
    out.append(message);
    if (startColor != null) {
      out.append(ideLevel.getEndColor());
    }
    out.append("\n");
  }

  @Override
  public void flush() {

  }

  @Override
  public void close() {

  }
}

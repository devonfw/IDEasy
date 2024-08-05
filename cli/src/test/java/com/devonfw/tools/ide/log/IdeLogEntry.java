package com.devonfw.tools.ide.log;

/**
 * Single entry that was logged by {@link IdeTestLogger}.
 *
 * @param level the {@link IdeLogLevel}.
 * @param message the message that has been logged.
 */
public record IdeLogEntry(IdeLogLevel level, String message) {

  public static IdeLogEntry ofError(String message) {

    return new IdeLogEntry(IdeLogLevel.ERROR, message);
  }

  public static IdeLogEntry ofWarning(String message) {

    return new IdeLogEntry(IdeLogLevel.WARNING, message);
  }

  public static IdeLogEntry ofInfo(String message) {

    return new IdeLogEntry(IdeLogLevel.INFO, message);
  }

  public static IdeLogEntry ofStep(String message) {

    return new IdeLogEntry(IdeLogLevel.STEP, message);
  }

  public static IdeLogEntry ofSuccess(String message) {

    return new IdeLogEntry(IdeLogLevel.SUCCESS, message);
  }

  public static IdeLogEntry ofDebug(String message) {

    return new IdeLogEntry(IdeLogLevel.DEBUG, message);
  }

  public static IdeLogEntry ofTrace(String message) {

    return new IdeLogEntry(IdeLogLevel.TRACE, message);
  }
}

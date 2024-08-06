package com.devonfw.tools.ide.log;

/**
 * Single entry that was logged by {@link IdeTestLogger}.
 *
 * @param level the {@link IdeLogLevel}.
 * @param message the message that has been logged.
 */
public record IdeLogEntry(IdeLogLevel level, String message, boolean contains) {

  public IdeLogEntry(IdeLogLevel level, String message) {
    this(level, message, false);
  }

  public boolean matches(IdeLogEntry entry) {

    if (this.level != entry.level) {
      return false;
    } else if (this.contains) {
      if (!entry.message.contains(this.message)) {
        return false;
      }
    } else if (!entry.message.equals(this.message)) {
      return false;
    }
    return true;
  }

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

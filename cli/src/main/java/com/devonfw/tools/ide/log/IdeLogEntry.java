package com.devonfw.tools.ide.log;

/**
 * Single entry that was logged.
 *
 * @param level the {@link IdeLogLevel}.
 * @param message the {@link IdeSubLogger#log(String) logged message}.
 * @param rawMessage the {@link IdeSubLogger#log(String, Object...) raw message template}.
 * @param args the {@link IdeSubLogger#log(String, Object...) optional message arguments}.
 * @param error the {@link IdeSubLogger#log(Throwable, String) optional error that was logged}.
 * @param contains - {@code true} if the {@link IdeLogEntry} to create is used as sub-string pattern for {@link #matches(IdeLogEntry) matching},
 *     {@code false} otherwise.
 */
public record IdeLogEntry(IdeLogLevel level, String message, String rawMessage, Object[] args, Throwable error, boolean contains) {

  /**
   * The constructor.
   *
   * @param level - {@link #level()}.
   * @param message - {@link #message()}.
   * @param rawMessage - {@link #rawMessage()}.
   * @param args - {@link #args()}.
   * @param error - {@link #error()}.
   * @param contains - {@link #contains()}.
   */
  public IdeLogEntry {

    // validation to be used by all constructors...
    if ((message == null) && (rawMessage == null)) {
      throw new IllegalStateException();
    }
  }

  /**
   * @param level the {@link IdeLogLevel}.
   * @param message the {@link IdeSubLogger#log(String) logged message}.
   */
  public IdeLogEntry(IdeLogLevel level, String message) {

    this(level, message, null, null, null, false);
  }

  /**
   * @param level the {@link IdeLogLevel}.
   * @param message the {@link IdeSubLogger#log(String) logged message}.
   * @param contains the {@link #contains()} flag.
   */
  public IdeLogEntry(IdeLogLevel level, String message, boolean contains) {

    this(level, message, null, null, null, contains);
  }

  /**
   * @param level the {@link IdeLogLevel}.
   * @param message the {@link IdeSubLogger#log(String) logged message}.
   * @param rawMessage the {@link IdeSubLogger#log(String, Object...) raw message template}.
   * @param args the {@link IdeSubLogger#log(String, Object...) optional message arguments}.
   */
  public IdeLogEntry(IdeLogLevel level, String message, String rawMessage, Object[] args) {

    this(level, message, rawMessage, args, null, false);
  }

  /**
   * @param level the {@link IdeLogLevel}.
   * @param message the {@link IdeSubLogger#log(String) logged message}.
   * @param rawMessage the {@link IdeSubLogger#log(String, Object...) raw message template}.
   * @param args the {@link IdeSubLogger#log(String, Object...) optional message arguments}.
   * @param error the {@link IdeSubLogger#log(Throwable, String) optional error that was logged}.
   */
  public IdeLogEntry(IdeLogLevel level, String message, String rawMessage, Object[] args, Throwable error) {

    this(level, message, rawMessage, args, error, false);
  }

  /**
   * ATTENTION: This method is not symmetric so it may be that x.matches(y) != y.matches(x). This method should always be called on the expected entry with the
   * actually collected entry as parameter.
   *
   * @param entry the {@link IdeLogEntry} to match.
   * @return {@code true} if the given {@link IdeLogEntry} matches to this one, {@code false} otherwise.
   */
  public boolean matches(IdeLogEntry entry) {

    if (this.level != entry.level) {
      return false;
    } else if (this.contains) {
      if (!entry.message.contains(this.message)) {
        return false;
      }
    } else {
      if (this.message != null && !entry.message.equals(this.message)) {
        return false;
      }
    }
    return true;
  }

  /**
   * @param message the {@link #message() message}.
   * @return the new {@link IdeLogEntry} with {@link IdeLogLevel#ERROR}.
   */
  public static IdeLogEntry ofError(String message) {

    return new IdeLogEntry(IdeLogLevel.ERROR, message);
  }

  /**
   * @param message the {@link #message() message}.
   * @return the new {@link IdeLogEntry} with {@link IdeLogLevel#WARNING}.
   */
  public static IdeLogEntry ofWarning(String message) {

    return new IdeLogEntry(IdeLogLevel.WARNING, message);
  }

  /**
   * @param message the {@link #message() message}.
   * @return the new {@link IdeLogEntry} with {@link IdeLogLevel#INFO}.
   */
  public static IdeLogEntry ofInfo(String message) {

    return new IdeLogEntry(IdeLogLevel.INFO, message);
  }

  /**
   * @param message the {@link #message() message}.
   * @return the new {@link IdeLogEntry} with {@link IdeLogLevel#STEP}.
   */
  public static IdeLogEntry ofStep(String message) {

    return new IdeLogEntry(IdeLogLevel.STEP, message);
  }

  /**
   * @param message the {@link #message() message}.
   * @return the new {@link IdeLogEntry} with {@link IdeLogLevel#INTERACTION}.
   */
  public static IdeLogEntry ofInteraction(String message) {

    return new IdeLogEntry(IdeLogLevel.INTERACTION, message);
  }

  /**
   * @param message the {@link #message() message}.
   * @return the new {@link IdeLogEntry} with {@link IdeLogLevel#SUCCESS}.
   */
  public static IdeLogEntry ofSuccess(String message) {

    return new IdeLogEntry(IdeLogLevel.SUCCESS, message);
  }

  /**
   * @param message the {@link #message() message}.
   * @return the new {@link IdeLogEntry} with {@link IdeLogLevel#DEBUG}.
   */
  public static IdeLogEntry ofDebug(String message) {

    return new IdeLogEntry(IdeLogLevel.DEBUG, message);
  }

  /**
   * @param message the {@link #message() message}.
   * @return the new {@link IdeLogEntry} with {@link IdeLogLevel#TRACE}.
   */
  public static IdeLogEntry ofTrace(String message) {

    return new IdeLogEntry(IdeLogLevel.TRACE, message);
  }

  /**
   * @param message the {@link #message() message}.
   * @return the new {@link IdeLogEntry} with {@link IdeLogLevel#PROCESSABLE}.
   */
  public static IdeLogEntry ofProcessable(String message) {

    return new IdeLogEntry(IdeLogLevel.PROCESSABLE, message);
  }

}

package com.devonfw.ide.gui.console;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Represents a single log entry in the console with level, timestamp, message, and optional error.
 */
public class LogEntry {

  private final String message;
  private final IdeLogLevel level;
  private final Long timeStamp;

  /**
   * Creates a plain log entry without level (for plain text output).
   *
   * @param message the message
   */
  public LogEntry(String message) {
    this(message, null);
  }

  /**
   * Creates a log entry with level.
   *
   * @param level the log level
   * @param message the message
   */
  public LogEntry(IdeLogLevel level, String message) {
    this(level, message, null);
  }

  /**
   * Creates a log entry with level and error.
   *
   * @param level the log level
   * @param message the message
   * @param error the error
   */
  public LogEntry(IdeLogLevel level, String message, Throwable error) {
    this.level = level;
    this.message = message + (error != null ? "\n  Error: " + error.getMessage() : "");
    this.timeStamp = System.currentTimeMillis();
  }

  /**
   * Creates a log entry with message and level (convenience).
   *
   * @param message the message
   * @param level the log level
   */
  public LogEntry(String message, IdeLogLevel level) {
    this.level = level;
    this.message = message;
    this.timeStamp = System.currentTimeMillis();
  }

  /**
   * Gets the message.
   *
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Gets the log level.
   *
   * @return the log level
   */
  public IdeLogLevel getLevel() {
    return level;
  }


  /**
   * Gets the timestamp.
   *
   * @return the timestamp
   */
  public Long getTimeStamp() {
    return timeStamp;
  }

  private String formatTimeStamp() {
    return Instant.ofEpochMilli(timeStamp).atZone(ZoneId.systemDefault()).toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
  }

  @Override
  public String toString() {
    if (level == null) {
      return message;
    }
    String prefix = getPrefix(level);
    return String.format("%s | %s %s", formatTimeStamp(), prefix, message);
  }

  private String getPrefix(IdeLogLevel level) {
    return switch (level) {
      case ERROR -> "[ERROR] ";
      case WARNING -> "[WARN]  ";
      case INFO -> "[INFO]  ";
      case DEBUG -> "[DEBUG] ";
      case TRACE -> "[TRACE] ";
      default -> "";
    };
  }
}

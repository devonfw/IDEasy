package com.devonfw.ide.gui.console;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.devonfw.tools.ide.log.IdeLogEntry;

/**
 * Wrapper class for an {@link IdeLogEntry} that attaches a timestamp and provides a readable string representation for use in the GUI console.
 * <p>
 * The wrapper stores the original log entry and a timestamp (in milliseconds since the epoch). It offers getters for both values and a {@code toString()}
 * implementation that formats the timestamp and the log entry level and message for display.
 */
public class GuiLogEntryWrapper {

  /** The wrapped log entry from the ide logging framework. */
  private final IdeLogEntry entry;

  /** The timestamp associated with this wrapper (milliseconds since Unix epoch). */
  private Long timeStamp;

  /**
   * Create a new wrapper for the given {@link IdeLogEntry} using the current system time as the timestamp.
   *
   * @param entry the log entry to wrap; must not be {@code null}.
   */
  public GuiLogEntryWrapper(IdeLogEntry entry) {

    this.entry = entry;
    this.timeStamp = System.currentTimeMillis();
  }

  /**
   * Create a new wrapper for the given {@link IdeLogEntry} with the specified timestamp.
   *
   * @param entry the log entry to wrap; must not be {@code null}.
   * @param timeStamp the timestamp in milliseconds since the epoch to associate with this wrapper.
   */
  public GuiLogEntryWrapper(IdeLogEntry entry, Long timeStamp) {

    this.entry = entry;
    this.timeStamp = timeStamp;
  }

  /**
   * Get the wrapped {@link IdeLogEntry}.
   *
   * @return the underlying log entry (never {@code null} for properly constructed instances).
   */
  public IdeLogEntry getEntry() {

    return this.entry;
  }

  /**
   * Get the timestamp associated with this wrapper.
   *
   * @return the timestamp in milliseconds since the epoch.
   */
  public Long getTimeStamp() {

    return timeStamp;
  }

  /**
   * Set or update the timestamp associated with this wrapper.
   *
   * @param timeStamp the new timestamp in milliseconds since the epoch.
   */
  public void setTimeStamp(Long timeStamp) {

    this.timeStamp = timeStamp;
  }

  /**
   * Return a human-readable representation combining the formatted timestamp, the log level and the log message. Example output:
   * <pre>12:34:56.789 | [INFO] Application started</pre>
   *
   * @return a formatted string suitable for display in the GUI console.
   */
  @Override
  public String toString() {

    String timeStampReadable = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(this.timeStamp));
    return String.format("%s | [%s] %s", timeStampReadable, this.entry.level(), this.entry.message());
  }
}

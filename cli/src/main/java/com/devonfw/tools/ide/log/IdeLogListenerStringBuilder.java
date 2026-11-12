package com.devonfw.tools.ide.log;

/**
 * Implementation of {@link IdeLogListener} that appends the message of every accepted {@link IdeLogEntry log event} to a {@link StringBuilder}.
 * <p>
 * It is used to capture the output of a code section (e.g. a {@link com.devonfw.tools.ide.commandlet.StatusCommandlet status commandlet}) without it being
 * printed to the console or written to the log file. {@link #onLog(IdeLogEntry) The listener} rejects every event so that the regular logging pipeline is
 * bypassed and {@link #toString() only the captured messages} remain.
 */
public class IdeLogListenerStringBuilder implements IdeLogListener {

  private final StringBuilder builder;

  /**
   * The constructor.
   */
  public IdeLogListenerStringBuilder() {

    super();
    this.builder = new StringBuilder();
  }

  /**
   * The constructor.
   *
   * @param builder the {@link StringBuilder} to append the captured log messages to.
   */
  public IdeLogListenerStringBuilder(StringBuilder builder) {

    super();
    this.builder = builder;
  }

  @Override
  public boolean onLog(IdeLogEntry entry) {

    this.builder.append(entry.message()).append('\n');
    return false;
  }

  @Override
  public String toString() {

    return this.builder.toString();
  }

}

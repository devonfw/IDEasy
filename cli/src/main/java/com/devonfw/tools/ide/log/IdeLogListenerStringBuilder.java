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

  private final IdeLogLevel threshold;

  /**
   * The constructor.
   */
  public IdeLogListenerStringBuilder() {

    super();
    this.builder = new StringBuilder();
    this.threshold = IdeLogLevel.TRACE;
  }

  /**
   * The constructor.
   *
   * @param builder the {@link StringBuilder} to append the captured log messages to.
   */
  public IdeLogListenerStringBuilder(StringBuilder builder) {

    super();
    this.builder = builder;
    this.threshold = IdeLogLevel.TRACE;
  }

  /**
   * The constructor.
   *
   * @param threshold the minimum {@link IdeLogLevel} to capture (log events of a lower level are ignored). Use {@link IdeLogLevel#TRACE} to capture all
   *     log events.
   */
  public IdeLogListenerStringBuilder(IdeLogLevel threshold) {

    super();
    this.builder = new StringBuilder();
    this.threshold = (threshold == null) ? IdeLogLevel.TRACE : threshold;
  }

  /**
   * The constructor.
   *
   * @param builder the {@link StringBuilder} to append the captured log messages to.
   * @param threshold the minimum {@link IdeLogLevel} to capture (log events of a lower level are ignored). Use {@link IdeLogLevel#TRACE} to capture all
   *     log events.
   */
  public IdeLogListenerStringBuilder(StringBuilder builder, IdeLogLevel threshold) {

    super();
    this.builder = builder;
    this.threshold = (threshold == null) ? IdeLogLevel.TRACE : threshold;
  }

  @Override
  public boolean onLog(IdeLogEntry entry) {

    if (entry.level().ordinal() < this.threshold.ordinal()) {
      return false;
    }
    this.builder.append(entry.message()).append('\n');
    return false;
  }

  @Override
  public String toString() {

    return this.builder.toString();
  }

}

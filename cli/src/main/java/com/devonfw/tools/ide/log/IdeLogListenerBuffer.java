package com.devonfw.tools.ide.log;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link IdeLogListener} to buffer log events during bootstrapping and then flush them once the logger is properly configured.
 *
 * @see com.devonfw.tools.ide.context.IdeContext#runWithoutLogging(Runnable)
 */
public class IdeLogListenerBuffer implements IdeLogListener {

  private static final Logger LOG = LoggerFactory.getLogger(IdeLogListenerBuffer.class);

  protected final List<IdeLogEntry> buffer;

  protected IdeLogLevel threshold;

  private boolean buffering;

  /**
   * The constructor.
   */
  public IdeLogListenerBuffer() {
    this(true);
  }

  IdeLogListenerBuffer(boolean buffering) {
    super();
    this.buffer = new ArrayList<>();
    this.threshold = IdeLogLevel.TRACE;
    this.buffering = buffering;
  }

  @Override
  public boolean onLog(IdeLogLevel level, String message, String rawMessage, Object[] args, Throwable error) {
    if (this.buffering) {
      if (level.ordinal() >= threshold.ordinal()) {
        this.buffer.add(new IdeLogEntry(level, message, rawMessage, args, error));
      }
      return false;
    }
    return true;
  }

  /**
   * @return {@code true} if this collector is currently buffering all logs, {@code false} otherwise (regular logging).
   */
  protected boolean isBuffering() {
    return this.buffering;
  }

  /**
   * This method is supposed to be called once after invocation of {@link #startBuffering(IdeLogLevel)}.
   */
  public void flushAndEndBuffering() {

    // disable buffering further log events
    this.buffering = false;
    // write all cached log events to the logger again for processing
    for (IdeLogEntry entry : this.buffer) {
      IdeLogLevel level = entry.level();
      level.log(LOG, entry.error(), entry.rawMessage(), entry.args());
    }
    this.buffer.clear();
    this.threshold = IdeLogLevel.TRACE;
  }

  /**
   * Re-enables the buffering of the logger so nothing gets logged and log messages are only collected until {@link #flushAndEndBuffering()} is called.
   *
   * @param threshold the {@link IdeLogLevel} acting as threshold.
   * @see com.devonfw.tools.ide.context.IdeContext#runWithoutLogging(Runnable, IdeLogLevel)
   */
  public void startBuffering(IdeLogLevel threshold) {

    assert (!this.buffering);
    this.threshold = threshold;
    this.buffering = true;
  }

}

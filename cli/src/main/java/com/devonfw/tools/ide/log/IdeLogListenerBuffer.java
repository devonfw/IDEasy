package com.devonfw.tools.ide.log;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements {@link IdeLogListener} to buffer log events during bootstrapping and then flush them once the logger is properly configured.
 *
 * @see com.devonfw.tools.ide.context.IdeContext#runWithoutLogging(Runnable)
 */
public class IdeLogListenerBuffer implements IdeLogListener {

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
   * This method is supposed to be called once after the {@link IdeLogger} has been properly initialized or after invocation of
   * {@link #startBuffering(IdeLogLevel)}.
   *
   * @param logger the initialized {@link IdeLogger}.
   */
  public void flushAndEndBuffering(IdeLogger logger) {

    // disable buffering further log events
    this.buffering = false;
    // write all cached log events to the logger again for processing
    for (IdeLogEntry entry : this.buffer) {
      logger.level(entry.level()).log(entry.error(), entry.message());
    }
    this.buffer.clear();
    this.threshold = IdeLogLevel.TRACE;
  }

  /**
   * Re-enables the buffering of the logger so nothing gets logged and log messages are only collected until {@link #flushAndEndBuffering(IdeLogger)} is
   * called.
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

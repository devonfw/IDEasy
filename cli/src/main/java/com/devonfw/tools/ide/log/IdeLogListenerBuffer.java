package com.devonfw.tools.ide.log;

/**
 * Extends {@link IdeLogListenerCollector} to buffer log events during bootstrapping and then flush them once the logger is properly configured.
 */
public class IdeLogListenerBuffer extends IdeLogListenerCollector {

  private boolean disabled;

  @Override
  protected boolean isActive() {

    return !this.disabled;
  }

  @Override
  public boolean onLog(IdeLogLevel level, String message, String rawMessage, Object[] args, Throwable error) {

    if (isActive()) {
      // buffer the log event
      super.onLog(level, message, rawMessage, args, error);
      // reject further processing of the log event suppressing it (so it is only cached)
      return false;
    } else {
      return true;
    }
  }

  /**
   * This method is supposed to be called once after the {@link IdeLogger} has been properly initialized.
   *
   * @param logger the initialized {@link IdeLogger}.
   */
  public void flushAndDisable(IdeLogger logger) {

    if (this.disabled) {
      assert (this.entries.isEmpty());
      return;
    }
    // disable ourselves from collecting further events
    this.disabled = true;
    // write all cached log events to the logger again for processing
    for (IdeLogEntry entry : this.entries) {
      logger.level(entry.level()).log(entry.error(), entry.message());
    }
    this.entries.clear();
  }

  /**
   * Re-enables the buffering of the logger so nothing gets logged and log messages are only collected until {@link #flushAndDisable(IdeLogger)} is called.
   *
   * @param threshold the {@link IdeLogLevel} acting as threshold.
   * @see com.devonfw.tools.ide.context.IdeContext#runWithoutLogging(Runnable, IdeLogLevel)
   */
  public void enable(IdeLogLevel threshold) {

    assert (this.disabled);
    this.threshold = threshold;
    this.disabled = false;
  }

}

package com.devonfw.tools.ide.log;

/**
 * Implementation of {@link IdeLogListener} that does nothing.
 */
public class IdeLogListenerNone implements IdeLogListener {

  /** The singleton instance. */
  public static final IdeLogListenerNone INSTANCE = new IdeLogListenerNone();

  private IdeLogListenerNone() {
    super();
  }

  @Override
  public boolean onLog(IdeLogEntry entry) {

    return true;
  }
}

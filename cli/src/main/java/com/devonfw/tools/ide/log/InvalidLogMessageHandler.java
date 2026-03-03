package com.devonfw.tools.ide.log;

/**
 * Handler for {@link #invalidMessage(String, boolean, Object[]) invalid log messages}.
 */
@FunctionalInterface
public interface InvalidLogMessageHandler {

  /** An instance doing nothing. */
  InvalidLogMessageHandler NONE = (message, more, args) -> {
  };

  /**
   * @param message the message template.
   * @param more - {@code true} if more placeholders were present than {@code args} given, {@code false} otherwise (fewer placeholders present).
   * @param args the dynamic arguments to fill into the message template.
   */
  void invalidMessage(String message, boolean more, Object[] args);
}

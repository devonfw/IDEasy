package com.devonfw.tools.ide.process;

/**
 * Interface to listen for output.
 */
@FunctionalInterface
public interface OutputListener {

  /**
   * An empty {@link OutputListener} instance doing nothing.
   */
  OutputListener NONE = (m, e) -> {
  };

  /**
   * @param message the output message
   * @param error {@code true} in case of an error, {@code false} otherwise.
   */
  void onOutput(String message, boolean error);
}

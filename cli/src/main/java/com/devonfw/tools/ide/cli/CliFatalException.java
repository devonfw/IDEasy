package com.devonfw.tools.ide.cli;

/**
 * {@link CliException} that aborts the entire CLI process when a critical guardrail fails (e.g. the settings repository could not be cloned or is not a valid
 * settings repository). Unlike a regular error that only makes the current {@link com.devonfw.tools.ide.step.Step Step} fail while the overall process
 * continues, this exception {@link #isForceRethrowInStep() is always re-thrown} so no further step is executed in an invalid state.
 */
public final class CliFatalException extends CliException {

  /**
   * The constructor.
   *
   * @param message the {@link #getMessage() message}.
   */
  public CliFatalException(String message) {

    super(message);
  }

  /**
   * The constructor.
   *
   * @param message the {@link #getMessage() message}.
   * @param cause the {@link #getCause() cause}.
   */
  public CliFatalException(String message, Throwable cause) {

    super(message, cause);
  }

  @Override
  public boolean isForceRethrowInStep() {

    return true;
  }
}

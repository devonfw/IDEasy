package com.devonfw.tools.ide.cli;

import com.devonfw.tools.ide.process.ProcessResult;

/**
 * {@link CliException} that is thrown if the user aborted further processing (e.g. by answering a question with "no") or if a question cannot be asked because
 * we are in batch mode without force mode. Since the end-user wants to stop, this exception {@link #isForceRethrowInStep() is always re-thrown} from a
 * {@link com.devonfw.tools.ide.step.Step Step} so no further step is executed.
 */
public final class CliAbortException extends CliException {

  /**
   * The constructor.
   */
  public CliAbortException() {

    super("Aborted by end-user.", ProcessResult.ABORT);
  }

  @Override
  public boolean isForceRethrowInStep() {

    return true;
  }

}

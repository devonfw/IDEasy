package com.devonfw.tools.ide.context;

import com.devonfw.tools.ide.cli.CliProcessException;
import com.devonfw.tools.ide.process.ProcessContextImpl;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;

/**
 * Extends {@link ProcessContextImpl} for testing.
 */
public class ProcessContextTestImpl extends ProcessContextImpl {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public ProcessContextTestImpl(IdeContext context) {

    super(context);
  }

  @Override
  public ProcessResult run(ProcessMode processMode) {

    ProcessResult result;
    try {
      result = super.run(ProcessMode.DEFAULT_CAPTURE);
      logOutput(result);
      return result;
    } catch (CliProcessException e) {
      logOutput(e.getProcessResult());
      throw e;
    }
  }

  private void logOutput(ProcessResult result) {
    for (String out : result.getOut()) {
      this.context.info(out);
    }
    for (String err : result.getErr()) {
      this.context.error(err);
    }
  }
}

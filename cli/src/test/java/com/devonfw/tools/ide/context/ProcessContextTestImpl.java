package com.devonfw.tools.ide.context;

import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessContext;
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
  public ProcessContext createChild() {

    return this;
  }

  @Override
  public ProcessResult run(ProcessMode processMode) {
    ProcessResult result = super.run(ProcessMode.DEFAULT_CAPTURE);
    // this hack is still required to capture test script output
    if (result.isSuccessful() && (processMode == ProcessMode.DEFAULT || processMode == ProcessMode.BACKGROUND)) {
      result.log(IdeLogLevel.INFO);
    }
    return result;
  }
}

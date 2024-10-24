package com.devonfw.tools.ide.context;

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

    return super.run(ProcessMode.DEFAULT_CAPTURE);
  }
}

package com.devonfw.tools.ide.context;

import com.devonfw.tools.ide.process.ProcessContextImpl;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;

public class ProcessContextTestImpl extends ProcessContextImpl {

  public ProcessContextTestImpl(IdeContext context) {

    super(context);
  }

  @Override
  public ProcessResult run(ProcessMode processMode) {

    ProcessResult result = super.run(ProcessMode.DEFAULT_CAPTURE);
    for (String out : result.getOut()) {
      this.context.info(out);
    }
    for (String err : result.getErr()) {
      this.context.error(err);
    }
    return result;
  }
}

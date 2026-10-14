package com.devonfw.tools.ide.context;

import java.util.ArrayList;
import java.util.List;

import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessContextImpl;

/**
 * Mock {@link ProcessContext} that captures executed commands for testing without actually running them.
 */
public class CapturingProcessContextTest extends ProcessContextImpl {

  private final List<String> capturedArgs = new ArrayList<>();

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CapturingProcessContextTest(IdeContext context) {
    super(context);
  }

  @Override
  public ProcessContext addArg(String arg) {
    this.capturedArgs.add(arg);
    return this;
  }

  /**
   * @return the {@link capturedArgs captured arguments}.
   */
  public List<String> getArgs() {

    return this.capturedArgs;
  }
}

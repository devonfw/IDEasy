package com.devonfw.tools.ide.context;

import java.util.ArrayList;
import java.util.List;

import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessContextImpl;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.process.ProcessResultImpl;

/**
 * Mock {@link ProcessContext} that captures executed commands for testing without actually running them.
 */
public class CapturingProcessContextTest extends ProcessContextImpl {

  private final List<String> executedCommands = new ArrayList<>();

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CapturingProcessContextTest(IdeContext context) {
    super(context);
  }

  @Override
  public ProcessContext createChild() {
    return new CapturingProcessContextTest(this.context) {
      @Override
      public ProcessResult run(ProcessMode processMode) {
        return CapturingProcessContextTest.this.run(processMode);
      }
    };
  }

  @Override
  public ProcessResult run(ProcessMode processMode) {
    String executable = this.executable.toString();
    List<String> args = this.arguments;
    String command = executable + " " + String.join(" ", args);
    executedCommands.add(command);

    this.arguments.clear();
    this.executable = null;

    return new ProcessResultImpl("bash", command, ProcessResult.SUCCESS, true, List.of());
  }

  /**
   * @return the list of captured commands that would have been executed.
   */
  public List<String> getExecutedCommands() {
    return executedCommands;
  }
}

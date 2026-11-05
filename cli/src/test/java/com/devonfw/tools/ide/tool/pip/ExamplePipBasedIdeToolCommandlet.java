package com.devonfw.tools.ide.tool.pip;

import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.process.ProcessResultImpl;
import com.devonfw.tools.ide.tool.ToolInstallRequest;

/**
 * Test double of {@link PipBasedIdeToolCommandlet} that records the workspace configuration and short-circuits the actual installation and process launch for
 * testing.
 */
public class ExamplePipBasedIdeToolCommandlet extends PipBasedIdeToolCommandlet {

  private boolean workspaceConfigured;

  /**
   * The constructor.
   *
   * @param context the {@link IdeTestContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool.
   */
  public ExamplePipBasedIdeToolCommandlet(IdeTestContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
  }

  @Override
  public void configureWorkspace() {
    // only record the call, do not perform the real workspace configuration
    this.workspaceConfigured = true;
  }

  @Override
  public ProcessResult runTool(ToolInstallRequest request, ProcessMode processMode, List<String> args) {
    // skip the actual installation and process launch
    return new ProcessResultImpl(getName(), getName(), ProcessResult.SUCCESS, List.of());
  }

  /**
   * @return {@code true} if {@link #configureWorkspace()} has been triggered, {@code false} otherwise.
   */
  public boolean wasWorkspaceConfigured() {

    return this.workspaceConfigured;
  }
}

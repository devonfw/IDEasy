package com.devonfw.tools.ide.tool.pip;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.ide.IdeFeatures;
import com.devonfw.tools.ide.tool.ide.IdeWorkspaceConfigurer;

/**
 * Base class for pip-based IDE tools that should launch in the background instead of blocking the terminal.
 * Implements {@link IdeFeatures} to provide IDE workspace configuration capabilities.
 */
public abstract class PipBasedIdeToolCommandlet extends PipBasedCommandlet implements IdeFeatures {

  private final IdeWorkspaceConfigurer workspaceConfigurer;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool.
   */
  public PipBasedIdeToolCommandlet(IdeContext context, String tool, Set<Tag> tags) {
    super(context, tool, tags);
    this.workspaceConfigurer = new IdeWorkspaceConfigurer(context, tool);
  }

  @Override
  public ProcessResult runTool(List<String> args) {
    configureWorkspace();
    return runTool(ProcessMode.BACKGROUND, null, args);
  }

  @Override
  public void configureWorkspace() {
    this.workspaceConfigurer.configureWorkspace();
  }

  @Override
  public Path getIdeMetadataPath() {
    return this.workspaceConfigurer.getIdeMetadataPath();
  }

  @Override
  public void importRepository(Path repositoryPath) {
    this.workspaceConfigurer.importRepository(repositoryPath);
  }

  @Override
  public Path getToolPath() {
    return this.context.getSoftwarePath().resolve(this.tool);
  }
}

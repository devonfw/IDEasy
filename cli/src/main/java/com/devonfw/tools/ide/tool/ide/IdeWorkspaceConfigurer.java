package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.step.Step;

/**
 * Configures IDE workspaces by merging templates from settings repositories.
 * <p>
 * This class encapsulates the workspace configuration logic that is shared between all IDEs, regardless of their installation mechanism (binary, pip, npm,
 * etc.). It can be used via composition by any {@link com.devonfw.tools.ide.tool.ToolCommandlet} that needs IDE workspace configuration capabilities.
 */
public class IdeWorkspaceConfigurer {

  private static final Logger LOG = LoggerFactory.getLogger(IdeWorkspaceConfigurer.class);

  private final IdeContext context;
  private final String toolName;

  /**
   * Creates a new workspace configurer for the given IDE tool.
   *
   * @param context the {@link IdeContext}.
   * @param toolName the name of the IDE tool (e.g. "intellij", "spyder", "vscode").
   */
  public IdeWorkspaceConfigurer(IdeContext context, String toolName) {
    this.context = context;
    this.toolName = toolName;
  }

  /**
   * Configure (initialize or update) the workspace for this IDE using the templates from the settings.
   */
  public void configureWorkspace() {
    FileAccess fileAccess = this.context.getFileAccess();
    Path workspaceFolder = this.context.getWorkspacePath();
    if (!fileAccess.isExpectedFolder(workspaceFolder)) {
      LOG.warn("Current workspace does not exist: {}", workspaceFolder);
      return; // should actually never happen...
    }
    Step step = this.context.newStep("Configuring workspace " + workspaceFolder.getFileName() + " for IDE " + this.toolName);
    step.run(() -> doMergeWorkspaceStep(step, workspaceFolder));
  }

  private void doMergeWorkspaceStep(Step step, Path workspaceFolder) {

    int errors = 0;
    errors = mergeWorkspace(this.context.getUserHomeIde(), workspaceFolder, errors);
    errors = mergeWorkspace(this.context.getSettingsPath(), workspaceFolder, errors);
    errors = mergeWorkspace(this.context.getConfPath(), workspaceFolder, errors);
    if (errors == 0) {
      step.success();
    } else {
      step.error("Your workspace configuration failed with {} error(s) - see log above.\n"
          + "This is either a configuration error in your settings git repository or a bug in IDEasy.\n"
          + "Please analyze the above errors with your team or IDE-admin and try to fix the problem.", errors);
      this.context.askToContinue(
          "In order to prevent you from being blocked, you can start your IDE anyhow but some configuration may not be in sync.");
    }
  }

  private int mergeWorkspace(Path configFolder, Path workspaceFolder, int errors) {

    int result = errors;
    result = mergeWorkspaceSingle(configFolder.resolve(IdeContext.FOLDER_WORKSPACE), workspaceFolder, result);
    result = mergeWorkspaceSingle(configFolder.resolve(this.toolName).resolve(IdeContext.FOLDER_WORKSPACE), workspaceFolder, result);
    return result;
  }

  private int mergeWorkspaceSingle(Path templatesFolder, Path workspaceFolder, int errors) {

    Path setupFolder = templatesFolder.resolve(IdeContext.FOLDER_SETUP);
    Path updateFolder = templatesFolder.resolve(IdeContext.FOLDER_UPDATE);
    if (!Files.isDirectory(setupFolder) && !Files.isDirectory(updateFolder)) {
      LOG.trace("Skipping empty or non-existing workspace template folder {}.", templatesFolder);
      return errors;
    }
    LOG.debug("Merging workspace templates from {}...", templatesFolder);
    return errors + this.context.getWorkspaceMerger().merge(setupFolder, updateFolder, this.context.getVariables(), workspaceFolder);
  }
}

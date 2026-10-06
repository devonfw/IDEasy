package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.CommandletManager;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.AbstractEnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.ExtensibleEnvironmentVariables;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;

/**
 * {@link Commandlet} interface for IDE-specific features that are independent of the installation mechanism (binary vs. package manager).
 * <p>
 * This allows tools installed via package managers (like pip for Spyder) to still benefit from IDEasy's IDE features such as workspace configuration, metadata
 * management, and repository import.
 */
public interface IdeToolCommandlet extends LocalToolCommandlet {

  Logger LOG = LoggerFactory.getLogger(IdeToolCommandlet.class);

  /**
   * @return the {@link IdeWorkspaceConfigurer} that configures the workspace of this IDE. Needed so that the default {@link #configureWorkspace()} of this
   *     interface can delegate the shared workspace configuration logic to a single implementation.
   */
  IdeWorkspaceConfigurer getWorkspaceConfigurer();

  /**
   * Configures (initializes or updates) the workspace for this IDE using the templates from the settings. The default implementation is shared by all IDEs,
   * whether installed as binary (see {@link AbstractIdeToolCommandlet}) or via a package manager (see
   * {@link com.devonfw.tools.ide.tool.pip.PipBasedIdeToolCommandlet}).
   */
  default void configureWorkspace() {

    getWorkspaceConfigurer().configureWorkspace(this::getWorkspaceRedirects);
  }

  /**
   * @param workspaceFolder the {@link IdeContext#getWorkspacePath() workspace folder}.
   * @return the {@link Map} with the {@link Path}s inside the given {@code workspaceFolder} as keys and the {@link Path}s where the according workspace
   *     templates shall be merged to instead as values. Allows to keep IDE-specific data out of the workspace (e.g. in {@link #getIdeMetadataPath()}) without
   *     changing the structure of the workspace templates in the settings. By default, nothing is redirected.
   */
  default Map<Path, Path> getWorkspaceRedirects(Path workspaceFolder) {

    return Map.of();
  }

  /**
   * @return the {@link Path} to the IDE-specific metadata folder for the current workspace, located at {@code $IDE_HOME/.ide/«toolName»/«workspace»}. Unlike
   *     {@link IdeContext#getWorkspacePath() the workspace path} (which holds the projects to open), this folder keeps IDE-specific metadata (e.g.
   *     {@code .vmoptions} or {@code *.properties} files) out of the workspace so it stays clean and independent of the IDE being used.
   *
   *     <p>
   *     The default implementation is shared by all IDEs, whether installed as binary (see {@link AbstractIdeToolCommandlet}) or via a package manager (see
   *     {@link com.devonfw.tools.ide.tool.pip.PipBasedIdeToolCommandlet}).
   */
  default Path getIdeMetadataPath() {

    IdeContext context = getContext();
    return context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE).resolve(getName()).resolve(context.getWorkspaceName());
  }

  /**
   * Imports the repository specified by the given {@link Path} into the IDE managed by this {@link IdeToolCommandlet}.
   *
   * <p>
   * The repository is searched for a build descriptor of any build tool that this IDE supports via {@link #getBuildTool2TemplateMap()}. The first match
   * triggers a merge of the corresponding template into the workspace via {@link #mergeTemplate(Path, String)}. If no build tool of this IDE applies the
   * repository is skipped.
   * </p>
   *
   * @param repositoryPath the {@link Path} to the repository directory to import.
   */
  default void importRepository(Path repositoryPath) {

    CommandletManager commandletManager = getContext().getCommandletManager();
    for (Entry<Class<? extends AbstractLocalToolCommandlet>, String> entry : getBuildTool2TemplateMap().entrySet()) {
      AbstractLocalToolCommandlet buildTool = commandletManager.getCommandlet(entry.getKey());
      Path buildDescriptor = buildTool.findBuildDescriptor(repositoryPath);
      if (buildDescriptor != null) {
        String templateFilename = entry.getValue();
        LOG.debug("Found build descriptor {} so merging template {}", buildDescriptor, templateFilename);
        mergeTemplate(repositoryPath, templateFilename);
        return;
      }
    }
    LOG.warn("No supported build descriptor was found for project import in {}", repositoryPath);
  }

  /**
   * @return the mapping of supported build tool commandlets to the template file name to be merged into the workspace (see
   *     {@link #mergeTemplate(Path, String)}) when the corresponding build descriptor is present in the imported repository.
   *     The default is an empty map meaning that no build tool is supported for repository import by this IDE.
   */
  default Map<Class<? extends AbstractLocalToolCommandlet>, String> getBuildTool2TemplateMap() {

    return Map.of();
  }

  /**
   * Merges the template with the given file name into the workspace for the imported repository. This is the IDE-specific part of
   * {@link #importRepository(Path)} and is called after a supported build descriptor was found.
   *
   * <p>
   * The template location is built dynamically from the tool name (see {@link #getTemplateFolder()}) and the template file name, so no per-IDE template path
   * constant is needed. The environment variables are created via {@link #getTemplateEnvironmentVariables(Path)} with the relative project path as
   * {@code PROJECT_PATH}.
   * </p>
   *
   * @param repositoryPath the {@link Path} to the imported repository directory.
   * @param templateFilename the file name of the workspace-relative template to merge (as configured in {@link #getBuildTool2TemplateMap()}).
   */
  default void mergeTemplate(Path repositoryPath, String templateFilename) {

    String templateFolder = getTemplateFolder();
    if (templateFolder == null) {
      throw new UnsupportedOperationException("Repository import is not yet implemented for IDE " + getName());
    }
    Path templateFile = getContext().getSettingsPath()
        .resolve(getName())
        .resolve(IdeContext.FOLDER_WORKSPACE)
        .resolve(IdeContext.FOLDER_REPOSITORY)
        .resolve(templateFolder)
        .resolve(templateFilename);
    if (!Files.exists(templateFile)) {
      throw new CliException("Cannot import project into workspace: template file not found at " + templateFile + "\n"
          + "Please do an upstream merge of your settings git repository.");
    }
    Path workspacesPath = getContext().getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES);
    Path workspacePath = getContext().getFileAccess().findAncestor(repositoryPath, workspacesPath, 1);
    if (workspacePath == null) {
      throw new CliException("Cannot import project into workspace: could not find workspace from " + repositoryPath);
    }
    EnvironmentVariables environmentVariables = getTemplateEnvironmentVariables(workspacePath.relativize(repositoryPath));
    Path workspaceFile = workspacePath.resolve(templateFolder).resolve(templateFilename);
    doMergeTemplate(templateFile, workspaceFile, environmentVariables);
  }

  /**
   * Performs the actual merge of the resolved template file into the workspace file. This is the only IDE-specific part of
   * {@link #mergeTemplate(Path, String)} as the merge algorithm differs per IDE (e.g. {@code JSON} vs {@code XML}).
   *
   * @param templateFile the resolved {@link Path} to the template file in the settings repository.
   * @param workspaceFile the {@link Path} to the target file in the workspace to merge the template into.
   * @param environmentVariables the {@link EnvironmentVariables} to resolve variables (e.g. {@code PROJECT_PATH}) in the template.
   */
  default void doMergeTemplate(Path templateFile, Path workspaceFile, EnvironmentVariables environmentVariables) {

    throw new UnsupportedOperationException("Repository import is not yet implemented for IDE " + getName());
  }

  /**
   * @return the name of the IDE configuration folder (e.g. {@code .vscode} or {@code .idea}) inside which the repository workspace templates
   *     are stored and merged, or {@code null} if this IDE does not support repository import. This folder is used both in the settings
   *     repository to locate the template and in the workspace to store the merged result.
   */
  default String getTemplateFolder() {

    return null;
  }

  /**
   * Creates {@link EnvironmentVariables} for resolving the imported repository workspace template with the relative project path as {@code PROJECT_PATH}.
   *
   * @param projectPath the relative {@link Path} from the workspace root to the repository.
   * @return the resolved {@link EnvironmentVariables}.
   */
  default EnvironmentVariables getTemplateEnvironmentVariables(Path projectPath) {

    ExtensibleEnvironmentVariables environmentVariables = new ExtensibleEnvironmentVariables(
        (AbstractEnvironmentVariables) getContext().getVariables().getParent(), getContext());
    environmentVariables.setValue("PROJECT_PATH", projectPath.toString().replace('\\', '/'));
    return environmentVariables.resolved();
  }
}

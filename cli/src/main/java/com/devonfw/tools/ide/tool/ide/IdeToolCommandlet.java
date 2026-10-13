package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;
import java.util.Map;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;

/**
 * {@link Commandlet} interface for IDE-specific features that are independent of the installation mechanism (binary vs. package manager).
 * <p>
 * This allows tools installed via package managers (like pip for Spyder) to still benefit from IDEasy's IDE features such as workspace configuration, metadata
 * management, and repository import.
 */
public interface IdeToolCommandlet extends LocalToolCommandlet {

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
   * @param repositoryPath the {@link Path} to the repository directory to import.
   */
  default void importRepository(Path repositoryPath) {

    throw new CliException("Repository import is not yet supported for IDE " + getName());
  }
}

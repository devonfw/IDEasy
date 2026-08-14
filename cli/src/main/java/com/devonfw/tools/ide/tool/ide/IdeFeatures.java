package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;

/**
 * Interface for IDE-specific features that are independent of the installation mechanism (binary vs. package manager).
 * <p>
 * This allows tools installed via package managers (like pip for Spyder) to still benefit from IDEasy's IDE features such as workspace configuration, metadata
 * management, and repository import.
 */
public interface IdeFeatures {

  /**
   * Configures (initializes or updates) the workspace for this IDE using the templates from the settings.
   */
  void configureWorkspace();

  /**
   * @return the {@link Path} to the IDE-specific metadata folder for the current workspace, located at {@code $IDE_HOME/.ide/«toolName»/«workspace»}. Unlike
   *     {@link com.devonfw.tools.ide.context.IdeContext#getWorkspacePath() the workspace path} (which holds the projects to open), this folder keeps
   *     IDE-specific metadata (e.g. {@code .vmoptions} or {@code *.properties} files) out of the workspace so it stays clean and independent of the IDE being
   *     used.
   */
  Path getIdeMetadataPath();

  /**
   * Imports the repository specified by the given {@link Path} into the IDE managed by this {@link IdeFeatures}.
   *
   * @param repositoryPath the {@link Path} to the repository directory to import.
   */
  void importRepository(Path repositoryPath);
}

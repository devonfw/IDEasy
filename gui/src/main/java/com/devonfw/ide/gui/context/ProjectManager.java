package com.devonfw.ide.gui.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Service class that allows to access the list of projects
 */
public class ProjectManager {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectManager.class);

  private final Path ideRootDirectory;

  /**
   * Service class that reads the list of projects/workspaces
   *
   * @param ideRootDirectory IDE_ROOT ENV variable value
   */
  /*
   * Protected: Class should only be accessed by the code via the {@link IdeGuiStateManager}
   * Why not in the IdeGuiContext? Reasoning is, that you might want to read the list of projects without being already in the project context
   */
  protected ProjectManager(Path ideRootDirectory) {

    this.ideRootDirectory = ideRootDirectory;

    if (ideRootDirectory == null) {
      throw new IllegalArgumentException("Root directory is null");
    } else if (!Files.exists(ideRootDirectory)) {
      throw new IllegalArgumentException("Root directory does not exist");
    } else if (!Files.isDirectory(ideRootDirectory)) {
      throw new IllegalArgumentException("Root directory is not a directory");
    }
  }

  /**
   * @return the list of project (names) in the project directory
   */
  public List<String> getProjectNames() {

    try (Stream<Path> subPaths = Files.list(ideRootDirectory)) {
      return subPaths
          .filter(Files::isDirectory)
          .map(Path::getFileName)
          .map(Path::toString)
          .filter(name -> !name.equals(IdeContext.FOLDER_UNDERSCORE_IDE) && Files.exists(ideRootDirectory.resolve(name).resolve("workspaces")))
          .toList();

    } catch (IOException e) {
      throw new RuntimeException("Failed to read project list!", e);
    }
  }

  /**
   * @param projectName name of the project for which the workspace names should be returned
   * @return the list of workspace (names) for the given project name
   */
  public List<String> getWorkspaceNames(String projectName) throws NotDirectoryException {

    Path workspacesDir = ideRootDirectory.resolve(projectName).resolve("workspaces");
    if (!Files.isDirectory(workspacesDir)) {
      throw new NotDirectoryException("invalid workspaces directory for project: " + projectName);
    }
    try (Stream<Path> subPaths = Files.list(workspacesDir)) {
      return subPaths.filter(Files::isDirectory)
          .map(Path::getFileName).map(Path::toString).toList();
    } catch (IOException e) {
      throw new RuntimeException("Failed to read workspaces for " + projectName, e);
    }
  }
}

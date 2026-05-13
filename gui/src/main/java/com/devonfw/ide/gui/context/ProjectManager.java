package com.devonfw.ide.gui.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import org.jline.utils.Log;

/**
 * Service class that allows to access the list of projects
 */
public class ProjectManager {

  private final Path ideRootDirectory;

  private final ArrayList<String> projectNames = new ArrayList<>();
  private final HashMap<String, ArrayList<String>> workspaces = new HashMap<>();

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

    refreshProjects();
  }

  /**
   * re-reads the list of projects/workspaces
   */
  public void refreshProjects() {
    projectNames.clear();
    workspaces.clear();

    readProjects();
    readWorkspaces();
  }

  /**
   * read all projects in the users IDE_ROOT directory
   */
  private void readProjects() {

    try (Stream<Path> subPaths = Files.list(ideRootDirectory)) {
      subPaths
          .filter(Files::isDirectory)
          .map(Path::getFileName)
          .map(Path::toString)
          .filter(name -> !name.startsWith("_") && Files.exists(ideRootDirectory.resolve(name).resolve("workspaces")))
          .forEach(projectNames::add);
    } catch (IOException e) {
      Log.error("Failed to read project list!", e);
      throw new IllegalStateException("Failed to read project list!", e);
    }
  }

  /**
   * reads all workspaces of all loaded projects.
   */
  protected void readWorkspaces() {

    if (projectNames.isEmpty()) {
      Log.info("Project list is empty. Therefore no workspaces can be read.");
    }

    for (String projectName : projectNames) {
      Path projectDirectory = ideRootDirectory.resolve(projectName);
      Path workspacesDirectory = projectDirectory.resolve("workspaces");

      ArrayList<String> workspaceNames = new ArrayList<>();

      try (Stream<Path> subPaths = Files.list(workspacesDirectory)) {
        subPaths
            .filter(Files::isDirectory)
            .map(Path::getFileName)
            .map(Path::toString)
            .forEach(workspaceNames::add);

        workspaces.put(projectName, workspaceNames);
      } catch (IOException e) {
        Log.error("Error occurred while fetching workspace names.", e);
        throw new RuntimeException("Error occurred while fetching workspace names.", e);
      }
    }
  }

  /**
   * @return the list of project (names) in the project directory
   */
  public List<String> getProjectNames() {
    return projectNames;
  }

  /**
   * @param projectName name of the project for which the workspace names should be returned
   * @return the list of workspace (names) for the given project name
   */
  public List<String> getWorkspaceNames(String projectName) {
    return workspaces.get(projectName);
  }
}

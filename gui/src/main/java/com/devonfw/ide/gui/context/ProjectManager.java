package com.devonfw.ide.gui.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class that allows to access the list of projects
 */
public class ProjectManager {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectManager.class);

  private final Path ideRootDirectory;

  private final List<String> projectNames = new ArrayList<>();
  private final Map<String, List<String>> workspaces = new HashMap<>(); // key: project name, value: list of workspace names

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
   * read all projects in the users IDE_ROOT directory
   */
  protected void readProjects() {

    projectNames.clear();

    try (Stream<Path> subPaths = Files.list(ideRootDirectory)) {
      subPaths
          .filter(Files::isDirectory)
          .map(Path::getFileName)
          .map(Path::toString)
          .filter(name -> !name.startsWith("_") && Files.exists(ideRootDirectory.resolve(name).resolve("workspaces")))
          .forEach(projectNames::add);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read project list!", e);
    }
  }

  /**
   * reads all workspaces of all loaded projects.
   */
  protected void readWorkspaces() {

    workspaces.clear();

    if (projectNames.isEmpty()) {
      LOG.warn("Project list is empty. Therefore no workspaces can be read.");
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
        throw new RuntimeException("Failed to read workspaces!", e);
      }
    }
  }

  /**
   * @return the list of project (names) in the project directory
   */
  public List<String> getProjectNames() {

    readProjects();
    return projectNames;
  }

  /**
   * @param projectName name of the project for which the workspace names should be returned
   * @return the list of workspace (names) for the given project name
   */
  public List<String> getWorkspaceNames(String projectName) {

    readWorkspaces();
    return workspaces.get(projectName);
  }
}

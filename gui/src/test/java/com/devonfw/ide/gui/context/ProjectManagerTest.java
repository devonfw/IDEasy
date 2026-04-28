package com.devonfw.ide.gui.context;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Tests for the ProjectManager class.
 */
public class ProjectManagerTest extends AbstractIdeContextTest {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectManagerTest.class);

  private static ProjectManager projectManager;

  private static IdeTestContext context;
  private static Path ideRoot;

  @BeforeEach
  void resetContext() {

    context = newContext("testProject", "project-0");
    ideRoot = context.getIdeRoot();
  }

  @Test
  void testConstructorWithValidDirectory() {

    //No exception should be thrown
    projectManager = new ProjectManager(ideRoot);
  }

  @Test
  void testConstructorWithNullDirectory() {

    try {
      projectManager = new ProjectManager(null);
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("Root directory is null");
    }
  }

  @Test
  void testConstructorWithNonExistentDirectory() {

    try {

      projectManager = new ProjectManager(ideRoot.resolve("nonExistent"));
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("Root directory does not exist");
    }
  }

  @Test
  void testConstructorWithFile() throws IOException {

    try {
      File testFile = ideRoot.resolve("testFile").toFile();
      boolean success = testFile.createNewFile();
      if (!success) {
        throw new RuntimeException("Unable to create test file");
      }

      projectManager = new ProjectManager(ideRoot.resolve("testFile"));
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("Root directory is not a directory");
    } finally {
      Files.deleteIfExists(ideRoot.resolve("testFile"));
    }
  }

  // ============ readProjects() Tests ============

  @Test
  void testReadProjectsWithEmptyProjectDirectory() {

    context = newContext("emptyProjects", "");
    projectManager = new ProjectManager(ideRoot);
    projectManager.refreshProjects();

    assertThat(projectManager.getProjectNames()).isEmpty();
  }

  @Test
  void testReadProjectsWithValidProjects() {

    projectManager = new ProjectManager(ideRoot);

    //test folder contains baseProject to project-5
    assertThat(projectManager.getProjectNames()).hasSize(6).as("Should have 6 projects")
        .containsExactlyInAnyOrder("project-0", "project-1", "project-2", "project-3", "project-4", "project-5");
  }

  @Test
  void testReadProjectsIgnoresUnderscorePrefixed() throws Exception {

    // Create a project with underscore prefix
    Path underscoreProject = ideRoot.resolve("_project");
    Files.createDirectories(underscoreProject);
    Files.copy(ideRoot.resolve("project-0"), underscoreProject, StandardCopyOption.REPLACE_EXISTING);

    projectManager = new ProjectManager(ideRoot);

    // Should not include _project, same as _ide directory should be ignored
    assertThat(projectManager.getProjectNames()).doesNotContain("_project").hasSize(6);

    //clean up
    Files.delete(underscoreProject);
  }

  @Test
  void testReadProjectsIgnoresDirectoriesWithoutWorkspacesFolder() {

    context = newContext("noWorkspaces", "projectWithoutWorkspaces");
    projectManager = new ProjectManager(ideRoot);

    // Should not include projectNoWorkspaces
    assertThat(projectManager.getProjectNames()).doesNotContain("projectWithoutWorkspaces");
  }

  // ============ readWorkspaces() Tests ============

  @Test
  void testReadWorkspacesWithMultipleWorkspaces() throws Exception {

    Path project = ideRoot.resolve("project-0");
    Path workspacesDir = project.resolve("workspaces");

    // Create additional workspaces besides existing
    Files.createDirectory(workspacesDir.resolve("dev"));
    Files.createDirectory(workspacesDir.resolve("prod"));

    projectManager = new ProjectManager(ideRoot);

    // Should have main, dev, prod workspaces
    assertThat(projectManager.getWorkspaceNames("project-0")).hasSize(4)
        .containsExactlyInAnyOrder("main", "foo-test", "dev", "prod");
  }

  @Test
  void testReadWorkspacesEmptyWorkspaceDirectory() {

    context = newContext("emptyWorkspaceFolders", "project-0");

    projectManager = new ProjectManager(ideRoot);

    // Should return empty list for empty workspaces directory
    assertThat(projectManager.getWorkspaceNames("project-0")).isEmpty();
  }

  // ============ refreshProjects() Tests ============

  @Test
  void testRefreshProjectsClearsExistingData() throws Exception {

    projectManager = new ProjectManager(ideRoot);

    // Get initial projects
    assertThat(projectManager.getProjectNames()).hasSize(6);

    // Delete a project
    try (var stream = Files.walk(ideRoot.resolve("project-0"))) {
      stream.forEach(p -> {
        try {
          FileUtils.deleteDirectory(p.toFile());
        } catch (Exception e) {
          fail("Error deleting project for test case: {}", p, e);
        }
      });
    }

    // Refresh should clear old data and reload
    projectManager.refreshProjects();

    assertThat(projectManager.getProjectNames()).hasSize(5).doesNotContain("project-0");
  }

  @Test
  void testRefreshProjectsReloadsAll() throws Exception {

    projectManager = new ProjectManager(ideRoot);

    assertThat(projectManager.getProjectNames()).isEmpty();

    Path newProject = ideRoot.resolve("newProject");
    Files.createDirectories(newProject.resolve("workspaces").resolve("main"));

    projectManager.refreshProjects();

    assertThat(projectManager.getProjectNames()).hasSize(1).contains("newProject");

    //clean up
    Files.delete(newProject);
  }

  @Test
  void testGetWorkspaceNamesReturnsValidList() {

    projectManager = new ProjectManager(ideRoot);

    assertThat(projectManager.getWorkspaceNames("project-0")).containsExactly("foo-test", "main");
  }

  @Test
  void testGetWorkspaceNamesWithInvalidProject() {

    projectManager = new ProjectManager(ideRoot);

    // Should return null for unknown project
    assertThat(projectManager.getWorkspaceNames("unknownProject")).isNull();
  }


  @Test
  void testProjectWithMultipleWorkspaces() throws Exception {

    Path project = ideRoot.resolve("project-0");
    Path workspacesDir = project.resolve("workspaces");

    // Create multiple workspaces
    for (String workspace : new String[] { "dev", "test", "staging", "production" }) {
      Files.createDirectory(workspacesDir.resolve(workspace));
    }

    projectManager = new ProjectManager(ideRoot);

    // Should have all workspaces including main
    assertThat(projectManager.getWorkspaceNames("project-0")).hasSize(5)
        .contains("main", "dev", "test", "staging", "production");
  }

  @Test
  void testMultipleProjectsWithWorkspaces() throws Exception {

    // Create different workspace structures for different projects
    for (int i = 0; i < 6; i++) {
      Path project = ideRoot.resolve("project-" + i);
      Path workspacesDir = project.resolve("workspaces");

      // Add additional workspaces to each project
      for (int j = 1; j <= i; j++) {
        Files.createDirectory(workspacesDir.resolve("workspace-" + j));
      }
    }

    projectManager = new ProjectManager(ideRoot);

    // Verify each project has correct number of workspaces
    for (int i = 0; i < 6; i++) {
      String projectName = "project-" + i;
      // Each project has "main" + i additional workspaces
      assertThat(projectManager.getWorkspaceNames(projectName)).hasSize(i + 1);
    }
  }

  @Test
  void testProjectNamesWithSpecialCharacters() throws Exception {

    Path specialProject = ideRoot.resolve("project-dash_underscore&");
    Files.createDirectories(specialProject.resolve("workspaces").resolve("main"));

    projectManager = new ProjectManager(ideRoot);

    assertThat(projectManager.getProjectNames()).contains("project-dash_underscore&");
  }

  @Test
  void testWorkspaceNamesWithSpecialCharacters() throws Exception {

    Path project = ideRoot.resolve("project-0");
    Path workspacesDir = project.resolve("workspaces");

    // Create workspaces with special characters
    Files.createDirectory(workspacesDir.resolve("dev-environment"));
    Files.createDirectory(workspacesDir.resolve("test_workspace"));

    projectManager = new ProjectManager(ideRoot);

    assertThat(projectManager.getWorkspaceNames("project-0"))
        .contains("dev-environment", "test_workspace", "main");
  }
}

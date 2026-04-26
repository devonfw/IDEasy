package com.devonfw.ide.gui.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.helper.FakeProjectFolderStructureHelper;

/**
 * Tests for the ProjectManager class.
 */
public class ProjectManagerTest {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectManagerTest.class);

  private ProjectManager projectManager;

  @TempDir
  private static Path mockIdeRoot;

  // Used in tests where we don't want to have any projects in the root directory
  @TempDir
  private Path emptyRootDir;

  @BeforeAll
  static void setup() throws IOException {

    FakeProjectFolderStructureHelper.createFakeProjectFolderStructure(mockIdeRoot);
  }

  //Before each test, we want to restore the default clean test folder structure
  @BeforeEach
  void resetToDefault() {

    try (var stream = Files.list(mockIdeRoot)) {
      stream.forEach(p -> {
        try {
          if (Files.isDirectory(p)) {
            FileUtils.deleteDirectory(p.toFile());
          } else if (Files.isRegularFile(p)) {
            Files.delete(p);
          }
        } catch (Exception e) {
          LOG.error("Error deleting file while resetting project structure: {}", p, e);
        }
      });
      FakeProjectFolderStructureHelper.createFakeProjectFolderStructure(mockIdeRoot);
    } catch (IOException e) {
      LOG.error("Error walking through files while resetting project structure", e);
    }
  }

  @Test
  void testConstructorWithValidDirectory() {

    //No exception should be thrown
    projectManager = new ProjectManager(mockIdeRoot);
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

      projectManager = new ProjectManager(mockIdeRoot.resolve("nonExistent"));
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("Root directory does not exist");
    }
  }

  @Test
  void testConstructorWithFile() throws IOException {

    try {

      File testFile = mockIdeRoot.resolve("testFile").toFile();
      boolean success = testFile.createNewFile();
      if (!success) {
        throw new RuntimeException("Unable to create test file");
      }

      projectManager = new ProjectManager(mockIdeRoot.resolve("testFile"));
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("Root directory is not a directory");
    }
  }

  // ============ readProjects() Tests ============

  @Test
  void testReadProjectsWithEmptyProjectDirectory() {

    projectManager = new ProjectManager(emptyRootDir);
    projectManager.refreshProjects();

    assertThat(projectManager.getProjectNames()).isEmpty();
  }

  @Test
  void testReadProjectsWithValidProjects() {

    projectManager = new ProjectManager(mockIdeRoot);

    // FakeProjectFolderStructureHelper creates projects 0-5
    assertThat(projectManager.getProjectNames()).hasSize(6).as("Should have 6 projects")
        .containsExactlyInAnyOrder("project-0", "project-1", "project-2", "project-3", "project-4", "project-5");
  }

  @Test
  void testReadProjectsIgnoresUnderscorePrefixed() throws Exception {

    // Create a project with underscore prefix
    Path underscoreProject = mockIdeRoot.resolve("_project");
    Files.createDirectories(underscoreProject.resolve("workspaces"));

    projectManager = new ProjectManager(mockIdeRoot);

    // Should not include _project, same as _ide directory should be ignored
    assertThat(projectManager.getProjectNames()).doesNotContain("_project").hasSize(6);
  }

  @Test
  void testReadProjectsIgnoresDirectoriesWithoutWorkspacesFolder() throws Exception {

    // Create a project directory without workspaces folder
    Path projectWithoutWorkspaces = mockIdeRoot.resolve("projectNoWorkspaces");
    Files.createDirectory(projectWithoutWorkspaces);

    projectManager = new ProjectManager(mockIdeRoot);

    // Should not include projectNoWorkspaces
    assertThat(projectManager.getProjectNames()).doesNotContain("projectNoWorkspaces").hasSize(6);
  }

  // ============ readWorkspaces() Tests ============

  @Test
  void testReadWorkspacesEmpty() {

    projectManager = new ProjectManager(emptyRootDir);

    // No projects, so no workspaces
    assertThat(projectManager.getWorkspaceNames("any-project")).isNull();
  }

  @Test
  void testReadWorkspacesWithMultipleWorkspaces() throws Exception {

    Path project = mockIdeRoot.resolve("project-0");
    Path workspacesDir = project.resolve("workspaces");

    // Create additional workspaces
    Files.createDirectory(workspacesDir.resolve("dev"));
    Files.createDirectory(workspacesDir.resolve("prod"));

    projectManager = new ProjectManager(mockIdeRoot);

    // Should have main, dev, prod workspaces
    assertThat(projectManager.getWorkspaceNames("project-0")).hasSize(3)
        .containsExactlyInAnyOrder("main", "dev", "prod");
  }

  @Test
  void testReadWorkspacesEmptyWorkspaceDirectory() throws Exception {

    // Create a project with empty workspaces directory
    Path emptyWorkspacesProject = mockIdeRoot.resolve("emptyWorkspacesProject");
    Files.createDirectories(emptyWorkspacesProject.resolve("workspaces"));

    projectManager = new ProjectManager(mockIdeRoot);

    // Should return empty list for empty workspaces directory
    assertThat(projectManager.getWorkspaceNames("emptyWorkspacesProject")).isEmpty();
  }

  // ============ refreshProjects() Tests ============

  @Test
  void testRefreshProjectsClearsExistingData() throws Exception {

    projectManager = new ProjectManager(mockIdeRoot);

    // Get initial projects
    assertThat(projectManager.getProjectNames()).hasSize(6);

    // Delete a project
    try (var stream = Files.walk(mockIdeRoot.resolve("project-0"))) {
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

    projectManager = new ProjectManager(emptyRootDir);

    assertThat(projectManager.getProjectNames()).isEmpty();

    Path newProject = emptyRootDir.resolve("newProject");
    Files.createDirectories(newProject.resolve("workspaces").resolve("main"));

    projectManager.refreshProjects();

    assertThat(projectManager.getProjectNames()).hasSize(1).contains("newProject");
  }

  @Test
  void testGetProjectNamesReturnsNotNull() {

    projectManager = new ProjectManager(emptyRootDir);

    assertThat(projectManager.getProjectNames()).isNotNull();
  }

  @Test
  void testGetProjectNamesReturnsCorrectList() {

    projectManager = new ProjectManager(mockIdeRoot);

    assertThat(projectManager.getProjectNames()).isNotEmpty().hasSize(6);
  }

  @Test
  void testGetWorkspaceNamesReturnsCorrectList() {

    projectManager = new ProjectManager(mockIdeRoot);

    assertThat(projectManager.getWorkspaceNames("project-0")).isNotNull().containsExactly("main");
  }

  @Test
  void testGetWorkspaceNamesWithInvalidProject() {

    projectManager = new ProjectManager(mockIdeRoot);

    // Should return null for unknown project
    assertThat(projectManager.getWorkspaceNames("unknownProject")).isNull();
  }

  //Just checks whether getting projects and workspaces works correctly in one go
  @Test
  void testCompleteWorkflow() {

    projectManager = new ProjectManager(mockIdeRoot);

    // Verify projects are loaded
    assertThat(projectManager.getProjectNames()).hasSize(6);

    // Verify workspaces are loaded for each project
    for (String projectName : projectManager.getProjectNames()) {
      assertThat(projectManager.getWorkspaceNames(projectName)).isNotNull().contains("main");
    }
  }

  @Test
  void testProjectWithMultipleWorkspaces() throws Exception {

    Path project = mockIdeRoot.resolve("project-0");
    Path workspacesDir = project.resolve("workspaces");

    // Create multiple workspaces
    for (String workspace : new String[] { "dev", "test", "staging", "production" }) {
      Files.createDirectory(workspacesDir.resolve(workspace));
    }

    projectManager = new ProjectManager(mockIdeRoot);

    // Should have all workspaces including main
    assertThat(projectManager.getWorkspaceNames("project-0")).hasSize(5)
        .contains("main", "dev", "test", "staging", "production");
  }

  @Test
  void testMultipleProjectsWithWorkspaces() throws Exception {

    // Create different workspace structures for different projects
    for (int i = 0; i < 6; i++) {
      Path project = mockIdeRoot.resolve("project-" + i);
      Path workspacesDir = project.resolve("workspaces");

      // Add additional workspaces to each project
      for (int j = 1; j <= i; j++) {
        Files.createDirectory(workspacesDir.resolve("workspace-" + j));
      }
    }

    projectManager = new ProjectManager(mockIdeRoot);

    // Verify each project has correct number of workspaces
    for (int i = 0; i < 6; i++) {
      String projectName = "project-" + i;
      // Each project has "main" + i additional workspaces
      assertThat(projectManager.getWorkspaceNames(projectName)).hasSize(i + 1);
    }
  }

  @Test
  void testConstructorCallsRefreshProjects() {

    projectManager = new ProjectManager(mockIdeRoot);

    // If refreshProjects is called in constructor, projects should be loaded immediately
    assertThat(projectManager.getProjectNames()).isNotEmpty().hasSize(6);
  }

  @Test
  void testProjectNamesWithSpecialCharacters() throws Exception {

    Path specialProject = mockIdeRoot.resolve("project-dash_underscore&");
    Files.createDirectories(specialProject.resolve("workspaces").resolve("main"));

    projectManager = new ProjectManager(mockIdeRoot);

    assertThat(projectManager.getProjectNames()).contains("project-dash_underscore&");
  }

  @Test
  void testWorkspaceNamesWithSpecialCharacters() throws Exception {

    Path project = mockIdeRoot.resolve("project-0");
    Path workspacesDir = project.resolve("workspaces");

    // Create workspaces with special characters
    Files.createDirectory(workspacesDir.resolve("dev-environment"));
    Files.createDirectory(workspacesDir.resolve("test_workspace"));

    projectManager = new ProjectManager(mockIdeRoot);

    assertThat(projectManager.getWorkspaceNames("project-0"))
        .contains("dev-environment", "test_workspace", "main");
  }
}

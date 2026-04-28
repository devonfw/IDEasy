package com.devonfw.ide.gui.context;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Tests for the ProjectManager class.
 */
public class ProjectManagerTest extends AbstractIdeContextTest {

  private static ProjectManager projectManager;

  private static IdeTestContext context;
  private static Path ideRoot;

  @BeforeEach
  void resetContext() {

    context = newContext("testProject", "project-0");
    ideRoot = context.getIdeRoot();
  }

  @Test
  void testProjectManagerFull() {

    projectManager = new ProjectManager(ideRoot);

    assertThat(projectManager).isNotNull();
    assertThat(projectManager.getProjectNames()).containsExactlyInAnyOrder("project-0", "project-1", "project-2", "project-3", "project-4", "project-5");
    for (String projectName : projectManager.getProjectNames()) {
      assertThat(projectManager.getWorkspaceNames(projectName)).containsExactlyInAnyOrder("foo-test", "main");
    }
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

  @Test
  void testRefreshProjects() throws IOException {

    projectManager = new ProjectManager(ideRoot);
    assertThat(projectManager.getProjectNames()).containsExactlyInAnyOrder("project-0", "project-1", "project-2", "project-3", "project-4", "project-5");

    Path project0 = ideRoot.resolve("project-0");
    Path project6 = ideRoot.resolve("project-6");
    FileUtils.copyDirectory(project0.toFile(), project6.toFile());

    projectManager.refreshProjects();

    // Verify that project-6 is now recognized
    assertThat(projectManager.getProjectNames()).containsExactlyInAnyOrder("project-0", "project-1", "project-2", "project-3", "project-4", "project-5",
        "project-6");
    assertThat(projectManager.getWorkspaceNames("project-6")).containsExactlyInAnyOrder("foo-test", "main");

    // Cleanup
    FileUtils.deleteDirectory(project0.toFile());
  }

  @Test
  void testReadProjectsExcludesFoldersWithoutWorkspaces() throws IOException {

    // Create a project folder without a workspaces subdirectory
    Path testProject = ideRoot.resolve("test-project-no-workspaces");
    Files.createDirectory(testProject);

    projectManager = new ProjectManager(ideRoot);

    // Verify that test-project-no-workspaces is not recognized
    assertThat(projectManager.getProjectNames()).doesNotContain("test-project-no-workspaces");
    assertThat(projectManager.getProjectNames()).containsExactlyInAnyOrder("project-0", "project-1", "project-2", "project-3", "project-4", "project-5");

    // Cleanup
    FileUtils.deleteDirectory(testProject.toFile());
  }

  @Test
  void testReadProjectsExcludesUnderscorePrefixedFolders() {

    projectManager = new ProjectManager(ideRoot);

    // Verify that _ide folder is not in the project names
    assertThat(projectManager.getProjectNames()).doesNotContain("_ide");
    assertThat(projectManager.getProjectNames()).containsExactlyInAnyOrder("project-0", "project-1", "project-2", "project-3", "project-4", "project-5");
  }
}

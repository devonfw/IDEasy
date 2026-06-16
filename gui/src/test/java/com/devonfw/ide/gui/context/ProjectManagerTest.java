package com.devonfw.ide.gui.context;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

  private final List<String> VALID_PROJECT_LIST = List.of("project-0", "project-1", "project-2", "project-3", "project-4", "project-5");

  @BeforeEach
  void resetContext() {

    context = newContext("testProject", "project-0");
    ideRoot = context.getIdeRoot();
  }

  @Test
  void testProjectManagerFull() throws NotDirectoryException {

    projectManager = new ProjectManager(ideRoot);

    assertThat(projectManager).isNotNull();
    assertThat(projectManager.getProjectNames()).containsAll(VALID_PROJECT_LIST);

    for (int i = 0; i < projectManager.getProjectNames().size(); i++) {
      assertThat(projectManager.getWorkspaceNames("project-" + i)).containsExactlyInAnyOrder("foo-test-" + i, "main");
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
    assertThat(projectManager.getProjectNames()).containsAll(VALID_PROJECT_LIST);

    Path project0 = ideRoot.resolve("project-0");
    Path project6 = ideRoot.resolve("project-6");
    FileUtils.copyDirectory(project0.toFile(), project6.toFile());
    File adjustedWorkspaceName = project6.resolve("workspaces").resolve("foo-test-0").toFile();

    //as we copied from project-0, the test workspace folder name is still old
    assertThat(adjustedWorkspaceName.renameTo(project6.resolve("workspaces").resolve("foo-test-6").toFile())).isTrue();

    // Verify that project-6 is now recognized
    List<String> extendedList = new ArrayList<>(VALID_PROJECT_LIST);
    extendedList.add("project-6");
    assertThat(projectManager.getProjectNames()).containsAll(extendedList);
    assertThat(projectManager.getWorkspaceNames("project-6")).containsExactlyInAnyOrder("foo-test-6", "main");

    // Cleanup
    FileUtils.deleteDirectory(project6.toFile());
  }

  @Test
  void testReadProjectsExcludesFoldersWithoutWorkspaces() throws IOException {

    // Create a project folder without a workspaces subdirectory
    Path testProject = ideRoot.resolve("test-project-no-workspaces");
    Files.createDirectory(testProject);

    projectManager = new ProjectManager(ideRoot);

    // Verify that test-project-no-workspaces is not recognized
    assertThat(projectManager.getProjectNames()).doesNotContain("test-project-no-workspaces");
    assertThat(projectManager.getProjectNames()).containsAll(VALID_PROJECT_LIST);

    // Cleanup
    FileUtils.deleteDirectory(testProject.toFile());
  }

  @Test
  void testReadProjectsExcludesUnderscorePrefixedFolders() {

    projectManager = new ProjectManager(ideRoot);

    // Verify that _ide folder is not in the project names
    assertThat(projectManager.getProjectNames()).doesNotContain("_ide");
    assertThat(projectManager.getProjectNames()).containsAll(VALID_PROJECT_LIST);
  }
}

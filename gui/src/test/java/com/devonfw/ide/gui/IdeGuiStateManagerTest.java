package com.devonfw.ide.gui;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.ide.gui.context.IdeGuiStateManager;

public class IdeGuiStateManagerTest {

  @TempDir
  static Path mockIdeRoot;

  private IdeGuiStateManager guiStateManager = IdeGuiStateManager.getInstanceOverrideRootDir(mockIdeRoot.toString());

  @BeforeAll
  static void setup() {
    FakeProjectFolderStructureHelper.createFakeProjectFolderStructure(mockIdeRoot);
  }

  @Test
  void testThrowsIfIdeRootNull() {

    try {
      IdeGuiStateManager.getInstanceOverrideRootDir(null);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("ideRoot must not be null!");
    }
  }

  @Test
  void testThrowsIfIdeRootDoesNotExist() {

    try {
      IdeGuiStateManager.getInstanceOverrideRootDir("nonExistingIdeRoot");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("Root directory does not exist");
    }
  }

  @Test
  void testGetContext() throws FileNotFoundException {

    IdeGuiContext context = guiStateManager.switchContext("project-0", "main");
    assertThat(context).isNotNull().as("context was null after switchContext was called"); // When switching to a project, the context should be set.
  }

  @Test
  void testSwitchContext() throws IOException {

    Files.list(mockIdeRoot).forEach((projectPath) -> {
      try {
        guiStateManager.switchContext(projectPath.getFileName().toString(), "main");
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  void testThrowsIfNonExistantProjectSelected() throws IOException {

    Path fakeProject = mockIdeRoot.resolve("nonExistingProject");

    try {
      guiStateManager.switchContext(fakeProject.getFileName().toString(), "main");
    } catch (FileNotFoundException e) {
      assertThat(e.getMessage()).contains("Project " + fakeProject.resolve("workspaces").resolve("main") + " does not exist!")
          .as("GuiStateManager.switchContext should throw an exception, if a non-existent project is selected");
    }
  }

  @Test
  void testThrowsIfNonExistantWorkspaceSelected() throws IOException {

    Files.list(mockIdeRoot).forEach((projectPath) -> {
      try {
        guiStateManager.switchContext(projectPath.getFileName().toString(), "test");
      } catch (FileNotFoundException e) {
        assertThat(e.getMessage()).contains("Workspace " + projectPath.resolve("workspaces").resolve("test") + " does not exist!")
            .as("GuiStateManager.switchContext should throw an exception, if a non-existent workspace is selected");
      }
    });
  }

}

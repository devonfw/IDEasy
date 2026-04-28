package com.devonfw.ide.gui.context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Tests for {@link IdeGuiStateManager}.
 */
public class IdeGuiStateManagerTest extends AbstractIdeContextTest {

  private static final Logger LOG = LoggerFactory.getLogger(IdeGuiStateManagerTest.class);

  private static IdeTestContext context;

  private static IdeGuiStateManager guiStateManager;
  private static ProjectManager projectManager;

  @BeforeAll
  static void setup() throws FileNotFoundException {

    context = newContext("testProject", "project");
    LOG.warn("root: {}", context.getIdeRoot());

    guiStateManager = IdeGuiStateManager.getInstanceOverrideRootDir(context.getIdeRoot().toString());
    projectManager = guiStateManager.getProjectManager();
  }

  @BeforeEach
  void reset() {
    IdeGuiStateManager.getInstanceOverrideRootDir(context.getIdeRoot().toString());
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

    IdeGuiContext context = guiStateManager.switchContext(projectManager.getProjectNames().getFirst(), "main");
    assertThat(context).isNotNull().as("context was null after switchContext was called"); // When switching to a project, the context should be set.
  }

  @Test
  void testSwitchContext() {

    projectManager.getProjectNames().forEach((projectName) -> {
      try {
        guiStateManager.switchContext(projectName, "main");
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  void testThrowsIfNonExistentProjectSelected() {

    Path fakeProject = context.getIdeRoot().resolve("nonExistingProject");

    try {
      guiStateManager.switchContext(fakeProject.getFileName().toString(), "main");
    } catch (FileNotFoundException e) {
      assertThat(e.getMessage()).contains("Project " + fakeProject + " does not exist!")
          .as("GuiStateManager.switchContext should throw an exception, if a non-existent project is selected");
    }
  }

  @Test
  void testThrowsIfNonExistentWorkspaceSelected() throws IOException {

    Files.list(context.getIdeRoot()).forEach((projectPath) -> {
      try {
        guiStateManager.switchContext(projectPath.getFileName().toString(), "test");
      } catch (FileNotFoundException e) {
        assertThat(e.getMessage()).contains("Workspace " + projectPath.resolve("workspaces").resolve("test") + " does not exist!")
            .as("GuiStateManager.switchContext should throw an exception, if a non-existent workspace is selected");
      }
    });
  }

}

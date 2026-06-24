package com.devonfw.ide.gui.context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Tests for {@link GuiStateManager}.
 */
public class GuiStateManagerTest extends AbstractIdeContextTest {

  private static final Logger LOG = LoggerFactory.getLogger(GuiStateManagerTest.class);

  private static IdeTestContext context;

  private static GuiStateManager guiStateManager;
  private static ProjectManager projectManager;
  private TaskManager taskManager;

  @BeforeAll
  static void setup() {

    context = newContext("testProject", "project-0");
    LOG.debug("root: {}", context.getIdeRoot());
  }

  @BeforeEach
  void reset() {

    TaskManager taskManager = new TaskManager();
    guiStateManager = new GuiStateManager(taskManager, context.getIdeRoot().toString());
    projectManager = guiStateManager.getProjectManager();
  }

  @Test
  void testThrowsIfIdeRootDoesNotExist() {

    try {
      new GuiStateManager(taskManager, "nonExistingIdeRoot");
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("Root directory does not exist");
    }
  }

  @Test
  void testGetContext() throws FileNotFoundException {

    IdeGuiContext context = guiStateManager.switchContext(projectManager.getProjectNames().getFirst(), "main");
    assertThat(context).as("context was null after switchContext was called").isNotNull(); // When switching to a project, the context should be set.
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
      fail("FileNotFoundException expected");
    } catch (FileNotFoundException e) {
      assertThat(e.getMessage()).contains("Project " + fakeProject + " does not exist!")
          .as("GuiStateManager.switchContext should throw an exception, if a non-existent project is selected");
    }
  }

  @Test
  void testThrowsIfNonExistentWorkspaceSelected() throws IOException {

    try (Stream<Path> stream = Files.list(context.getIdeRoot())) {
      stream.forEach((projectPath) -> {
        Path fakeWorkspacePath = projectPath.resolve("workspaces").resolve("nonExistingWorkspace");

        try {
          guiStateManager.switchContext(projectPath.getFileName().toString(), fakeWorkspacePath.getFileName().toString());
        } catch (FileNotFoundException e) {
          assertThat(e.getMessage()).contains("Workspace " + fakeWorkspacePath + " does not exist!")
              .as("GuiStateManager.switchContext should throw an exception, if a non-existent workspace is selected");
        }
      });
    }
  }

}

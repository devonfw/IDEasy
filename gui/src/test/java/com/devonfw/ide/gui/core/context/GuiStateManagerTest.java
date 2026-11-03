package com.devonfw.ide.gui.core.context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Tests for {@link GuiStateManager}.
 */
public class GuiStateManagerTest extends AbstractIdeContextTest {

  private static final Logger LOG = LoggerFactory.getLogger(GuiStateManagerTest.class);

  private static final String FIRST_PROJECT = "project-0";

  private static IdeTestContext context;

  private static GuiStateManager guiStateManager;

  @BeforeAll
  static void setup() {

    context = newContext("testProject", FIRST_PROJECT);
    LOG.debug("root: {}", context.getIdeRoot());
  }

  @BeforeEach
  void reset() {

    guiStateManager = new GuiStateManager(context.getIdeRoot().toString());
  }

  @Test
  void testThrowsIfIdeRootDoesNotExist() {

    assertThatIllegalArgumentException()
        .isThrownBy(() -> new GuiStateManager("nonExistingIdeRoot"))
        .withMessageContaining("Root directory does not exist");
  }

  @Test
  void testGetProjectNames() {

    assertThat(guiStateManager.getProjectNames()).contains(FIRST_PROJECT, "project-1", "project-2");
  }

  @Test
  void testGetWorkspaceNames() throws NotDirectoryException {

    assertThat(guiStateManager.getWorkspaceNames(FIRST_PROJECT)).containsExactlyInAnyOrder("main", "foo-test-0");
  }

  @Test
  void testGetWorkspaceNamesThrowsIfNoWorkspacesFolder() {

    assertThatExceptionOfType(NotDirectoryException.class)
        .isThrownBy(() -> guiStateManager.getWorkspaceNames("nonExistentProject"))
        .withMessageContaining("invalid workspaces directory for project: nonExistentProject");
  }

  @Test
  void testGetContext() throws FileNotFoundException {

    IdeGuiContext context = guiStateManager.switchContext(guiStateManager.getProjectNames().getFirst(), "main");
    assertThat(context).as("context was null after switchContext was called").isNotNull(); // When switching to a project, the context should be set.
  }

  @Test
  void testSwitchContext() {

    guiStateManager.getProjectNames().forEach((projectName) -> {
      try {
        guiStateManager.switchContext(projectName, "main");
        assertThat(guiStateManager.getCurrentContext().getCwd()).isNotNull();
        assertThat(guiStateManager.getCurrentContext().getCwd())
            .isEqualTo(context.getIdeRoot().resolve(projectName).resolve(IdeContext.FOLDER_WORKSPACES).resolve("main"));
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  void testThrowsIfNonExistentProjectSelected() {

    Path fakeProject = context.getIdeRoot().resolve("nonExistingProject");

    assertThatExceptionOfType(FileNotFoundException.class)
        .isThrownBy(() -> guiStateManager.switchContext(fakeProject.getFileName().toString(), "main"))
        .withMessageContaining("Project " + fakeProject + " does not exist!");
  }

  @Test
  void testThrowsIfNonExistentWorkspaceSelected() throws IOException {

    try (Stream<Path> stream = Files.list(context.getIdeRoot())) {
      stream.forEach((projectPath) -> {
        Path fakeWorkspacePath = projectPath.resolve("workspaces").resolve("nonExistingWorkspace");

        assertThatExceptionOfType(FileNotFoundException.class)
            .as("switchContext should throw if a non-existent workspace is selected")
            .isThrownBy(() -> guiStateManager.switchContext(projectPath.getFileName().toString(), fakeWorkspacePath.getFileName().toString()))
            .withMessageContaining("Workspace " + fakeWorkspacePath + " does not exist!");
      });
    }
  }

}

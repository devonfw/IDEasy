package com.devonfw.ide.gui.core.context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogListenerBuffer;

/**
 * Tests for {@link GuiStateManager}.
 */
public class GuiStateManagerTest extends AbstractIdeContextTest {

  private static final Logger LOG = LoggerFactory.getLogger(GuiStateManagerTest.class);

  private static IdeTestContext context;

  private static GuiStateManager guiStateManager;
  private static ProjectManager projectManager;

  @BeforeAll
  static void setup() {

    context = newContext("testProject", "project-0");
    LOG.debug("root: {}", context.getIdeRoot());
  }

  @BeforeEach
  void reset() {

    guiStateManager = new GuiStateManager(context.getIdeRoot().toString());
    projectManager = guiStateManager.getProjectManager();
  }

  @Test
  void testThrowsIfIdeRootDoesNotExist() {

    assertThatIllegalArgumentException()
        .isThrownBy(() -> new GuiStateManager("nonExistingIdeRoot"))
        .withMessageContaining("Root directory does not exist");
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

  /**
   * Verifies that GUI startup logging is activated to prevent startup errors from being swallowed.
   */
  @Test
  void testStartContextLoggingIsActivatedSoStartupErrorsAreNotSwallowed(@TempDir Path emptyRoot) {

    new GuiStateManager(emptyRoot.toString());

    // the state manager activates logging, so the start-context listener must no longer buffer
    IdeLogListenerBuffer buffer = (IdeLogListenerBuffer) IdeStartContextImpl.get().getLogListener();
    assertThat(buffer.isBuffering()).as("the GUI start context must not keep buffering after the state manager is created").isFalse();

    // an error logged now (as the default uncaught-exception handler does on a fatal startup crash) must be accepted, not swallowed
    Throwable startupError = new IllegalStateException("fatal startup error");
    boolean accept = buffer.onLog(new IdeLogEntry(IdeLogLevel.ERROR, "Uncaught exception in thread main", null, null, startupError));
    assertThat(accept).as("an error logged on the default uncaught-exception handler must reach the console").isTrue();
  }

}

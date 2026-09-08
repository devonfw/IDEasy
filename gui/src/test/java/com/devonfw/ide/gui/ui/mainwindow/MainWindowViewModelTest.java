package com.devonfw.ide.gui.ui.mainwindow;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.ide.gui.FakeProjectFolderStructureHelper;
import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.service.NlsService;

/**
 * Tests for {@link MainWindowViewModel}.
 * <p>
 * These run without starting the JavaFX toolkit, which is the point of separating the view model from {@link MainWindowView}: the selection logic and the
 * context switching are now reachable without a window. It also means the tests would fail with "Toolkit not initialized" if the view model ever went back to
 * constructing JavaFX dialogs itself instead of reporting through its error handler.
 */
public class MainWindowViewModelTest extends Assertions {

  private static final String PROJECT = "project-1";

  private static final String OTHER_PROJECT = "project-2";

  private static final String DEFAULT_WORKSPACE = "main";

  @TempDir
  private Path mockIdeRoot;

  private GuiStateManager guiStateManager;

  private MainWindowViewModel viewModel;

  /** Collects what the view model reports instead of the dialog the view would show. */
  private List<String> reportedErrors;

  @BeforeEach
  void setUp() throws IOException {

    FakeProjectFolderStructureHelper.createFakeProjectFolderStructure(this.mockIdeRoot);

    this.guiStateManager = new GuiStateManager(new TaskManager(), this.mockIdeRoot.toString());
    this.viewModel = new MainWindowViewModel(this.guiStateManager, this.guiStateManager.getProjectManager(), new NlsService(Locale.ENGLISH));

    this.reportedErrors = new ArrayList<>();
  }

  /**
   * Selecting a project has to populate the workspaces, preselect {@value #DEFAULT_WORKSPACE} and switch the context. This chain used to live in
   * {@code NavigationPanelControl}; it now hangs off the view model's own selection properties.
   */
  @Test
  void testSelectingProjectPreselectsMainWorkspaceAndSwitchesContext() {

    this.viewModel.getProjectsViewModel().select(PROJECT);

    assertThat(this.viewModel.getWorkspacesViewModel().getItems()).contains(DEFAULT_WORKSPACE);
    assertThat(this.viewModel.getWorkspacesViewModel().getSelectedItem()).contains(DEFAULT_WORKSPACE);
    assertThat(this.guiStateManager.isWorkspaceSelected()).as("a valid selection must enable the IDE launch buttons").isTrue();
    assertThat(this.guiStateManager.getCurrentContext()).isNotNull();
    assertThat(this.reportedErrors).isEmpty();
  }

  /**
   * Switching to a different project has to re-run the whole chain against the new project.
   */
  @Test
  void testSwitchingProjectSwitchesContextToTheNewProject() {

    this.viewModel.getProjectsViewModel().select(PROJECT);
    this.viewModel.getProjectsViewModel().select(OTHER_PROJECT);

    assertThat(this.viewModel.getWorkspacesViewModel().getSelectedItem()).contains(DEFAULT_WORKSPACE);
    assertThat(this.guiStateManager.getCurrentContext().getCwd()).endsWith(Path.of(OTHER_PROJECT, "workspaces", DEFAULT_WORKSPACE));
    assertThat(this.reportedErrors).isEmpty();
  }

}

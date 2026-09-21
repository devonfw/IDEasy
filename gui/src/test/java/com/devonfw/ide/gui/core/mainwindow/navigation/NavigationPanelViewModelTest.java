package com.devonfw.ide.gui.core.mainwindow.navigation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.ide.gui.FakeProjectFolderStructureHelper;
import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.context.TaskManager;
import com.devonfw.ide.gui.core.service.NlsService;

/**
 * Tests for {@link NavigationPanelViewModel}.
 * <p>
 * These run without starting the JavaFX toolkit. The project→workspace→context orchestration is driven by the view (see {@link NavigationPanelView}), so these
 * tests exercise the view model's own public methods the way the view calls them.
 */
public class NavigationPanelViewModelTest extends Assertions {

  private static final String PROJECT = "project-1";

  private static final String OTHER_PROJECT = "project-2";

  private static final String DEFAULT_WORKSPACE = "main";

  @TempDir
  private Path mockIdeRoot;

  private GuiStateManager guiStateManager;

  private NavigationPanelViewModel viewModel;

  @BeforeEach
  void setUp() throws IOException {

    FakeProjectFolderStructureHelper.createFakeProjectFolderStructure(this.mockIdeRoot);

    this.guiStateManager = new GuiStateManager(new TaskManager(), this.mockIdeRoot.toString());
    this.viewModel = new NavigationPanelViewModel(this.guiStateManager, new NlsService(Locale.ENGLISH));
  }

  /**
   * Populating the workspaces for a selected project has to include {@value #DEFAULT_WORKSPACE} and preselect it.
   */
  @Test
  void testPopulatingWorkspacesPreselectsMainWorkspace() {

    this.viewModel.getProjectsViewModel().select(PROJECT);
    this.viewModel.populateWorkspaceComboBox();

    assertThat(this.viewModel.getWorkspacesViewModel().getItems()).contains(DEFAULT_WORKSPACE);
    assertThat(this.viewModel.getWorkspacesViewModel().getSelectedItem()).contains(DEFAULT_WORKSPACE);
  }

  /**
   * Updating the context for a selected project/workspace has to switch the {@link GuiStateManager} to the corresponding working directory.
   */
  @Test
  void testUpdatingContextSwitchesToTheSelectedProjectAndWorkspace() {

    this.viewModel.getProjectsViewModel().select(OTHER_PROJECT);
    this.viewModel.populateWorkspaceComboBox();
    this.viewModel.updateContext();

    assertThat(this.guiStateManager.isWorkspaceSelected()).isTrue();
    assertThat(this.guiStateManager.getCurrentContext().getCwd()).endsWith(Path.of(OTHER_PROJECT, "workspaces", DEFAULT_WORKSPACE));
  }

}

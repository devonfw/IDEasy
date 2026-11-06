package com.devonfw.ide.gui.core.mainwindow.navigation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devonfw.ide.gui.UIBasedApplicationTest;
import com.devonfw.ide.gui.core.context.GuiStateManager;

/**
 * Tests for {@link NavigationPanelViewModel}.
 * <p>
 * These run without starting the JavaFX toolkit. The project→workspace→context orchestration is driven by the view (see {@link NavigationPanelView}), so these
 * tests exercise the view model's own public methods the way the view calls them.
 */
public class NavigationPanelViewModelTest extends UIBasedApplicationTest {

  private static final String PROJECT = "project-1";

  private static final String OTHER_PROJECT = "project-2";

  private static final String DEFAULT_WORKSPACE = "main";

  private GuiStateManager guiStateManager;

  private NavigationPanelViewModel viewModel;

  @BeforeEach
  void setUp() {

    setTestContext("testProject", PROJECT);

    this.guiStateManager = new GuiStateManager(getTestContext().getIdeRoot().toString());
    this.viewModel = new NavigationPanelViewModel(this.guiStateManager);
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

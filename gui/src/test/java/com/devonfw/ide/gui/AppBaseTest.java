package com.devonfw.ide.gui;

import static org.testfx.assertions.api.Assertions.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.IdeGuiStateManager;

/**
 * Basic UI Test
 */
public class AppBaseTest extends IdeGuiApplicationTest {

  private static Logger LOGGER = LoggerFactory.getLogger(AppBaseTest.class);

  private Button androidStudioOpen, eclipseOpen, intellijOpen, vsCodeOpen;
  private ComboBox<String> selectedProject, selectedWorkspace;


  @Override
  public void start(Stage stage) throws IOException {

    URL mainViewUrl = getClass().getResource("main-view.fxml");
    assertThat(mainViewUrl).as("Cannot resolve main UI FXML resource!").isNotNull();

    FXMLLoader fxmlLoader = new FXMLLoader(mainViewUrl);
    fxmlLoader.setController(new MainController(getMockIdeRoot().toString()));
    Parent root = fxmlLoader.load();
    stage.setScene(new Scene(root));
    stage.requestFocus(); //sometimes needed for headless setup to work
    stage.show();

    androidStudioOpen = (Button) root.lookup("#androidStudioOpen");
    eclipseOpen = (Button) root.lookup("#eclipseOpen");
    intellijOpen = (Button) root.lookup("#intellijOpen");
    vsCodeOpen = (Button) root.lookup("#vsCodeOpen");
    selectedProject = (ComboBox<String>) root.lookup("#selectedProject");
    selectedWorkspace = (ComboBox<String>) root.lookup("#selectedWorkspace");
  }

  /**
   * Generate a temporary project directories in order to be able to test on any device (including GitHub CI). This is required for the {@link MainController}
   * to work in the test context. Generates a structure like this: /project-[0..6]/workspaces/main
   */
  @BeforeAll
  protected static void generateProjectFolderStructure() throws FileNotFoundException {

    LOGGER.debug("tempDir: {}", getMockIdeRoot());
    for (int i = 0; i <= 5; i++) {
      String projectFolderName = "project-" + i;
      assertThat(getMockIdeRoot().resolve(projectFolderName).toFile().mkdir())
          .as("Unable to create mock project directory for mock project " + i)
          .isTrue();
      assertThat(getMockIdeRoot().resolve(projectFolderName).resolve("workspaces").toFile().mkdir())
          .as("Unable to create mock workspaces directory for mock project " + i)
          .isTrue();
      assertThat(getMockIdeRoot().resolve(projectFolderName).resolve("workspaces").resolve("main").toFile().mkdir())
          .as(
              "Unable to create mock main workspace directory for mock project " + i)
          .isTrue();
    }
    LOGGER.info("project folders: {}", Arrays.toString(getMockIdeRoot().toFile().list()));

    //We set the project root directory to the temporary directory before all tests, so that the IDE can find the projects in the test.
    IdeGuiStateManager.getInstance(getMockIdeRoot().toString()).switchContext("project-1", "main");
  }

  /**
   * This test ensures that all IDE open buttons are disabled when no project is selected.
   */
  @Test
  public void testIdeOpenButtonsDisabledWhenNoProjectSelected() {

    // assert that no project is selected
    assertThat(selectedProject.getValue()).isNull();

    // assert all IDE open buttons are disabled
    for (Button button : new Button[] { androidStudioOpen, eclipseOpen, intellijOpen, vsCodeOpen }) {
      assertThat(button.isDisabled()).as(button.getId() + " button should be disabled when no project has been selected").isTrue();
    }
  }

  /**
   * This test ensures that all IDE open buttons are enabled when a project is selected.
   */
  @Test
  public void testIdeOpenButtonsEnabledWhenProjectSelected() {

    // assert that a project is selected
    interact(() -> selectedProject.getSelectionModel().select("project-1"));

    // assert all IDE open buttons are disabled
    for (Button button : new Button[] { androidStudioOpen, eclipseOpen, intellijOpen, vsCodeOpen }) {
      assertThat(!button.isDisabled()).as(button.getId() + " button should be enabled when a project has been selected").isTrue();
    }
  }

  /**
   * Tests that the workspace {@link ComboBox} is disbaled when no project is selected.
   */
  @Test
  public void testWorkspaceComboBoxDisabledWhenNoProjectSelected() {

    assertThat(selectedProject.getValue()).isNull();

    assertThat(selectedWorkspace.isDisabled())
        .as("selectedWorkspace ComboBox should be disabled when no project is selected")
        .isTrue();
  }

  /**
   * Tests that the workspace {@link ComboBox} is enabled when a project is selected.
   */
  @Test
  public void testWorkspaceComboboxEnabledEnabledWhenProjectSelected() {

    // assert that a project is selected
    interact(() -> selectedProject.getSelectionModel().select("project-1"));

    // assert all IDE open buttons are disabled
    assertThat(selectedWorkspace.isDisabled())
        .as("selectedWorkspace ComboBox should be enabled when a project is selected")
        .isFalse();
  }
}

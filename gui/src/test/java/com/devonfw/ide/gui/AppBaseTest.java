package com.devonfw.ide.gui;

import static org.testfx.assertions.api.Assertions.assertThat;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

/**
 * Base Test
 */
public class AppBaseTest extends ApplicationTest {

  private Button androidStudioOpen, eclipseOpen, intellijOpen, vsCodeOpen;
  private ComboBox<String> selectedProject, selectedWorkspace;

  @Override
  public void start(Stage stage) throws IOException {

    Parent root = FXMLLoader.load(App.class.getResource("main-view.fxml"));
    stage.setScene(new Scene(root));
    stage.show();

    androidStudioOpen = (Button) root.lookup("#androidStudioOpen");
    eclipseOpen = (Button) root.lookup("#eclipseOpen");
    intellijOpen = (Button) root.lookup("#intellijOpen");
    vsCodeOpen = (Button) root.lookup("#vsCodeOpen");
    selectedProject = (ComboBox<String>) root.lookup("#selectedProject");
    selectedWorkspace = (ComboBox<String>) root.lookup("#selectedWorkspace");
  }

  /**
   * Set up headless testing
   *
   */
  @BeforeAll
  public static void setupHeadlessMode() {

    //Enable headless testing. Should be moved as a system property "-Dheadless=true" into workflows to only affect CI
    System.setProperty("headless", "true");

    if (Boolean.getBoolean("headless")) {
      System.setProperty("testfx.robot", "glass");
      System.setProperty("glass.platform", "Monocle");
      System.setProperty("testfx.headless", "true");
      System.setProperty("prism.order", "sw");
      System.setProperty("java.awt.headless", "true");
    }
  }

  /**
   * This test ensures, that all IDE open buttons are disabled when no project is selected.
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
   * Tests that the workspace ComboBox is disabled when no project is selected.
   */
  @Test
  public void testWorkspaceComboBoxDisabledWhenNoProjectSelected() {

    assertThat(selectedProject.getValue()).isNull();

    assertThat(selectedWorkspace.isDisabled())
        .as("selectedWorkspace ComboBox should be disabled when no project is selected")
        .isTrue();
  }
}

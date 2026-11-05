package com.devonfw.ide.gui.feature.tool.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.util.WaitForAsyncUtils;

import com.devonfw.ide.gui.UIBasedApplicationTest;
import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.event.TabChangeEvent;
import com.devonfw.ide.gui.core.mainwindow.navigation.NavigationPanelViewModel;
import com.devonfw.ide.gui.core.service.ToolConfiguration;

/**
 * Tests for {@link ToolConfigTab} together with its {@link ToolConfigViewModel} and {@link ToolConfigView}.
 * <p>
 * No project is selected, so no background lookup against the tool repository is triggered.
 */
class ToolConfigTabTest extends UIBasedApplicationTest {

  @TempDir
  Path ideRoot;

  private Stage stage;

  private final List<Tab> emittedTabs = new ArrayList<>();

  @Override
  public void start(Stage stage) {

    this.stage = stage;
    stage.setScene(new Scene(new Group()));
    stage.show();
  }

  private ToolConfigViewModel showToolConfigTab() {

    List<ToolConfigViewModel> viewModels = new ArrayList<>();
    interact(() -> {
      ToolConfigTab toolConfigTab = new ToolConfigTab(new GuiStateManager(this.ideRoot.toString()));
      Tab tab = toolConfigTab.createTabChangeEvent().tab();
      this.stage.setScene(new Scene((Parent) tab.getContent(), 800, 600));
      viewModels.add(toolConfigTab.getViewModel());
    });
    return viewModels.getFirst();
  }

  private static ToolConfiguration tool(String name, ToolConfiguration.ToolGroup group, boolean enabled) {

    ToolConfiguration tool = new ToolConfiguration(name);
    tool.setGroup(group);
    tool.setEnabled(enabled);
    tool.setConfiguredVersion("1.0.0");
    tool.setAvailableVersions(List.of("1.0.0", "2.0.0"));
    return tool;
  }

  /**
   * Opening the tool config tab twice must publish the same, closable tab.
   */
  @Test
  void testOpeningTwiceReusesTheSameTab() {

    interact(() -> {
      GuiStateManager guiStateManager = new GuiStateManager(this.ideRoot.toString());
      // like the main window: show the published tab in a tab pane unless it is already there
      TabPane tabPane = new TabPane();
      guiStateManager.getEventBus().addListener(TabChangeEvent.class, event -> {
        this.emittedTabs.add(event.tab());
        if (!tabPane.getTabs().contains(event.tab())) {
          tabPane.getTabs().add(event.tab());
        }
      });
      NavigationPanelViewModel navigationViewModel = new NavigationPanelViewModel(guiStateManager);
      navigationViewModel.openToolConfigTab();
      navigationViewModel.openToolConfigTab();
    });

    assertThat(this.emittedTabs).hasSize(2);
    assertThat(this.emittedTabs.get(1)).isSameAs(this.emittedTabs.getFirst());
    assertThat(this.emittedTabs.getFirst().getUserData()).isEqualTo("tab.tool_config");
    assertThat(this.emittedTabs.getFirst().isClosable()).isTrue();
  }

  /**
   * Rendering tools and entering an invalid version must disable saving until the version is fixed.
   */
  @Test
  void testInvalidVersionDisablesSaveUntilFixed() {

    ToolConfigViewModel viewModel = showToolConfigTab();
    ToolConfiguration intellij = tool("intellij", ToolConfiguration.ToolGroup.IDE, true);
    intellij.setSupportsEdition(true);
    intellij.setAvailableEditions(List.of("intellij", "ultimate"));
    intellij.setConfiguredEdition("intellij");
    ToolConfiguration mvn = tool("mvn", ToolConfiguration.ToolGroup.LOCAL, false);

    interact(() -> viewModel.getTools().setAll(intellij, mvn));
    WaitForAsyncUtils.waitForFxEvents();
    assertThat(viewModel.saveDisabledProperty().get()).isFalse();

    interact(() -> viewModel.changeVersion(intellij, "9.9.9", () -> {
    }));
    assertThat(viewModel.isInvalid(intellij)).isTrue();
    assertThat(viewModel.saveDisabledProperty().get()).isTrue();

    interact(() -> viewModel.changeVersion(intellij, "", () -> {
    }));
    assertThat(intellij.getConfiguredVersion()).isEqualTo("*");
    assertThat(viewModel.saveDisabledProperty().get()).isFalse();
    assertThat(viewModel.getPreview()).contains("IDE_TOOLS=intellij").contains("INTELLIJ_VERSION=*").doesNotContain("MVN_VERSION");
  }

  /**
   * The controls of a rendered row must write back to the {@link ToolConfiguration} and the preview dialog must open and close.
   */
  @Test
  void testRowControlsUpdateTheToolConfiguration() {

    ToolConfigViewModel viewModel = showToolConfigTab();
    ToolConfiguration intellij = tool("intellij", ToolConfiguration.ToolGroup.IDE, true);
    intellij.setSupportsEdition(true);
    intellij.setAvailableEditions(List.of("intellij", "ultimate"));
    intellij.setConfiguredEdition("intellij");
    interact(() -> viewModel.getTools().setAll(intellij));
    WaitForAsyncUtils.waitForFxEvents();

    List<ComboBox<String>> comboBoxes = new ArrayList<>();
    for (ComboBox<?> comboBox : lookup(".combo-box").queryAllAs(ComboBox.class)) {
      comboBoxes.add(asStringComboBox(comboBox));
    }
    assertThat(comboBoxes).hasSize(2);
    ComboBox<String> edition = comboBoxes.stream().filter(c -> c.getItems().contains("ultimate")).findFirst().orElseThrow();
    ComboBox<String> version = comboBoxes.stream().filter(c -> c != edition).findFirst().orElseThrow();

    // edition change
    interact(() -> edition.setValue("ultimate"));
    assertThat(intellij.getConfiguredEdition()).isEqualTo("ultimate");

    // lazy loading of the versions when the dropdown opens
    interact(() -> {
      version.show();
      version.hide();
    });
    assertThat(version.getItems()).containsExactly("1.0.0", "2.0.0");

    // validation when the version field loses focus
    interact(() -> {
      version.requestFocus();
      version.setValue("2.0.0");
      edition.requestFocus();
    });
    assertThat(intellij.getConfiguredVersion()).isEqualTo("2.0.0");

    // disabling the tool
    CheckBox enabled = lookup(".check-box").queryAs(CheckBox.class);
    interact(enabled::fire);
    assertThat(intellij.isEnabled()).isFalse();

    // preview dialog
    Button preview = lookup("#previewButton").queryButton();
    // the preview dialog blocks via showAndWait, so it must not be opened inside interact
    Platform.runLater(preview::fire);
    WaitForAsyncUtils.waitForFxEvents();
    TextArea previewText = lookup(".text-area").queryAs(TextArea.class);
    assertThat(previewText.getText()).startsWith("IDE_TOOLS=");
    interact(() -> previewText.getScene().getWindow().hide());
  }

  @SuppressWarnings("unchecked")
  private static ComboBox<String> asStringComboBox(ComboBox<?> comboBox) {

    return (ComboBox<String>) comboBox;
  }

}

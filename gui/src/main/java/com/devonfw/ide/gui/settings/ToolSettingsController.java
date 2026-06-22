package com.devonfw.ide.gui.settings;

import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.ide.gui.context.IdeGuiStateManager;
import com.devonfw.ide.gui.localization.LocalizationService;

/**
 * Controller for the Tools Configuration dialog.
 */
public class ToolSettingsController {

  @FXML
  private TreeView<ToolConfiguration> toolsTree;

  @FXML
  private Button saveButton;

  @FXML
  private Button cancelButton;

  @FXML
  private Button previewButton;

  private static final ToolSettingsService service = new ToolSettingsService();
  private static IdeGuiContext currentContext;

  @FXML
  private void initialize() {
    var ctx = IdeGuiStateManager.getInstance().getCurrentContext();
    currentContext = ctx;

    List<ToolConfiguration> toolConfigurations = service.listToolConfigurations(ctx);
    // Build a root (not shown) and create one TreeItem per tool for future extensibility
    TreeItem<ToolConfiguration> root = new TreeItem<>();
    root.setExpanded(false);
    for (ToolConfiguration toolConfiguration : toolConfigurations) {
      TreeItem<ToolConfiguration> item = new TreeItem<>(toolConfiguration);
      root.getChildren().add(item);
    }
    toolsTree.setRoot(root);
    toolsTree.setShowRoot(false);
    toolsTree.setCellFactory(tv -> new ToolTreeCell());
  }

  @FXML
  private void onSave() {
    // collect configurations from tree children
    List<ToolConfiguration> configs = toolsTree.getRoot().getChildren().stream().map(TreeItem::getValue).collect(Collectors.toList());
    service.applyAndSave(configs, currentContext);
    closeWindow();
  }

  @FXML
  private void onPreview() {

    List<ToolConfiguration> configs = toolsTree.getRoot().getChildren().stream().map(TreeItem::getValue).collect(Collectors.toList());
    String content = service.buildSettingsContent(configs);

    // Build a simple preview dialog
    TextArea area = new TextArea(content);
    area.setEditable(false);
    area.setPrefWidth(800);
    area.setPrefHeight(400);

    VBox box = new VBox(8, area);
    box.setPadding(new Insets(10));

    Scene scene = new Scene(box);
    Stage dialog = new Stage();
    dialog.initModality(Modality.APPLICATION_MODAL);
    dialog.initOwner(toolsTree.getScene().getWindow());
    dialog.setTitle(LocalizationService.getInstance().get("label.toolsConfig") + " - Preview");

    // Add Save and Close buttons
    Button save = new Button(LocalizationService.getInstance().get("button.save"));
    Button close = new Button(LocalizationService.getInstance().get("button.cancel"));
    save.setOnAction(e -> {
      // Persist changes
      service.applyAndSave(configs, currentContext);
      dialog.close();
      closeWindow();
    });
    close.setOnAction(e -> dialog.close());

    HBox actions = new HBox(8, close, save);
    actions.setAlignment(Pos.CENTER_RIGHT);
    box.getChildren().add(actions);

    dialog.setScene(scene);
    dialog.showAndWait();
  }

  @FXML
  private void onCancel() {
    closeWindow();
  }

  private void closeWindow() {
    if (toolsTree == null || toolsTree.getScene() == null) {
      return;
    }
    Window w = toolsTree.getScene().getWindow();
    if (w != null) {
      w.hide();
    }
  }

  // Custom TreeCell to display each tool configuration with multiple columns and appropriate controls
  private static final class ToolTreeCell extends TreeCell<ToolConfiguration> {

    private final HBox root;
    private CheckBox enabled;
    private Label name;
    private ComboBox<String> edition;
    private ComboBox<String> version;

    ToolTreeCell() {
      super();
      root = new HBox(10);
      root.setAlignment(Pos.CENTER_LEFT);
      root.setStyle("-fx-padding: 5 0 5 0;");
    }

    @Override
    protected void updateItem(ToolConfiguration toolItem, boolean empty) {
      super.updateItem(toolItem, empty);
      if (empty || toolItem == null) {
        setGraphic(null);
        setText(null);
      } else {
        // Rebuild the content for this tool Item
        root.getChildren().clear();

        // Column 1: Enabled checkbox
        enabled = new CheckBox();
        enabled.setPrefWidth(40);
        enabled.setSelected(toolItem.isEnabled());
        enabled.setOnAction(_ -> {
          toolItem.setEnabled(enabled.isSelected());
          updateVisualStateForDisabledTool(toolItem);
        });

        // Column 2: Tool name label
        name = new Label(toolItem.getToolName());
        name.setPrefWidth(120);
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 11;");

        // Column 3: Edition editable ComboBox
        edition = new ComboBox<>();
        edition.setPrefWidth(130);
        edition.setEditable(true);
        edition.setStyle("-fx-font-size: 10;");

        boolean supportsEdition = toolItem.isSupportsEdition() && toolItem.getAvailableEditions() != null;
        if (supportsEdition) {
          edition.setItems(FXCollections.observableArrayList(toolItem.getAvailableEditions()));
          edition.setValue(toolItem.getConfiguredEdition() == null ? "" : toolItem.getConfiguredEdition());
          edition.setVisible(true);
          edition.setManaged(true);
          // When edition changes, reload versions for that edition
          edition.setOnAction(e -> {
            String selectedEdition = edition.getValue();
            if (selectedEdition != null && !selectedEdition.isBlank() && currentContext != null) {
              List<String> newVersions = service.reloadVersionsForEdition(toolItem.getToolName(), selectedEdition, currentContext);
              version.setItems(FXCollections.observableArrayList(newVersions));
              toolItem.setConfiguredEdition(selectedEdition);
            }
          });
        } else {
          // Keep a gap where the edition combobox would be so columns stay aligned
          edition.setVisible(false);
          edition.setManaged(true); // still managed so it occupies layout space
          edition.getItems().clear();
          edition.setDisable(true);
        }

        // Column 4: Version editable ComboBox
        version = new ComboBox<>();
        version.setPrefWidth(130);
        version.setEditable(true);
        if (toolItem.getAvailableVersions() != null && !toolItem.getAvailableVersions().isEmpty()) {
          version.setItems(FXCollections.observableArrayList(toolItem.getAvailableVersions()));
        }
        version.setValue(toolItem.getConfiguredVersion() == null ? "" : toolItem.getConfiguredVersion());
        version.setOnAction(e -> toolItem.setConfiguredVersion(version.getValue()));
        version.setStyle("-fx-font-size: 10;");

        // Spacer at the end (grows to fill remaining space)
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Add all columns to root
        root.getChildren().addAll(enabled, name, edition, version, spacer);

        // Apply disabled state visually
        updateVisualStateForDisabledTool(toolItem);

        setGraphic(root);
      }
    }

    /**
     * Update visual styling based on enabled/disabled state.
     */
    private void updateVisualStateForDisabledTool(ToolConfiguration item) {
      double opacity = item.isEnabled() ? 1.0 : 0.6;
      enabled.setOpacity(opacity);
      name.setOpacity(opacity);
      version.setOpacity(opacity);
      edition.setOpacity(opacity);
      version.setDisable(!item.isEnabled());
      edition.setDisable(!item.isEnabled());
    }
  }

}


package com.devonfw.ide.gui.settings;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.application.Platform;
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
import javafx.scene.control.Tooltip;
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
  private Button previewButton;

  private static final ToolSettingsService service = new ToolSettingsService();
  private static IdeGuiContext currentContext;
  // Static so validation errors survive cell recycling — JavaFX reuses TreeCell instances during scroll.
  private static final Set<String> validationErrors = new HashSet<>();


  @FXML
  private void initialize() {
    currentContext = IdeGuiStateManager.getInstance().getCurrentContext();

    updateButtonStates();

    List<ToolConfiguration> configurations = service.listToolConfigurations(currentContext);

    toolsTree.setRoot(buildToolTree(configurations));
    toolsTree.setShowRoot(false);
    toolsTree.setCellFactory(tv -> new ToolTreeCell(this));

    // Load editions in background to avoid blocking the UI thread on startup.
    // Each tool's edition list is a directory scan, so we batch all tools in a single thread
    // and do a single refresh() at the end instead of one per tool.
    if (currentContext != null) {
      Thread t = new Thread(() -> {
        for (ToolConfiguration tc : configurations) {
          List<String> editions = service.loadEditionsForTool(tc.getToolName(), currentContext);
          tc.setAvailableEditions(editions);
          // Only show edition selector when there is a real choice (>1 option).
          tc.setSupportsEdition(editions.size() > 1);
          // Auto-select the sole edition so the user doesn't have to.
          if (editions.size() == 1 && (tc.getConfiguredEdition() == null || tc.getConfiguredEdition().isBlank())) {
            tc.setConfiguredEdition(editions.get(0));
          }
        }
        Platform.runLater(() -> toolsTree.refresh());
      });
      t.setDaemon(true);
      t.start();
    }
  }

  private TreeItem<ToolConfiguration> buildToolTree(List<ToolConfiguration> toolConfigurations) {
    TreeItem<ToolConfiguration> root = new TreeItem<>();
    root.setExpanded(false);
    for (ToolConfiguration.ToolGroup group : ToolConfiguration.ToolGroup.values()) {
      List<ToolConfiguration> groupTools = toolConfigurations.stream().filter(tc -> tc.getGroup() == group).toList();
      if (!groupTools.isEmpty()) {
        root.getChildren().add(createGroupItem(group, groupTools));
      }
    }
    return root;
  }

  private TreeItem<ToolConfiguration> createGroupItem(ToolConfiguration.ToolGroup group, List<ToolConfiguration> groupTools) {
    Label toolGroupLabel = new Label(group.getLabel());
    toolGroupLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12; -fx-padding: 6 0 6 0;");

    TreeItem<ToolConfiguration> groupItem = new TreeItem<>(null);
    groupItem.setGraphic(toolGroupLabel);
    groupItem.setExpanded(true);
    for (ToolConfiguration toolConfiguration : groupTools) {
      groupItem.getChildren().add(new TreeItem<>(toolConfiguration));
    }
    return groupItem;
  }

  @FXML
  private void onSave() {
    List<ToolConfiguration> toolConfigurations = collectLeafConfigurations();
    service.applyAndSave(toolConfigurations, currentContext);
    closeWindow();
  }

  @FXML
  private void onPreview() {
    List<ToolConfiguration> toolConfigurations = collectLeafConfigurations();
    String content = service.buildPreviewSettingsContent(toolConfigurations);

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

    Button save = new Button(LocalizationService.getInstance().get("button.save"));
    Button close = new Button(LocalizationService.getInstance().get("button.cancel"));
    save.setOnAction(event -> {
      event.consume();
      service.applyAndSave(toolConfigurations, currentContext);
      dialog.close();
      closeWindow();
    });
    close.setOnAction(event -> {
      event.consume();
      dialog.close();
    });

    HBox actions = new HBox(8, close, save);
    actions.setAlignment(Pos.CENTER_RIGHT);
    box.getChildren().add(actions);

    dialog.setScene(scene);
    dialog.showAndWait();
  }

  private List<ToolConfiguration> collectLeafConfigurations() {
    return toolsTree.getRoot().getChildren().stream().flatMap(groupNode -> groupNode.getChildren().stream()).map(TreeItem::getValue)
        .filter(Objects::nonNull).filter(ToolConfiguration::isEnabled).collect(Collectors.toList());
  }

  @FXML
  private void onCancel() {
    closeWindow();
  }

  private void updateButtonStates() {
    boolean hasErrors = !validationErrors.isEmpty();
    saveButton.setDisable(hasErrors);
    previewButton.setDisable(hasErrors);
  }

  private void closeWindow() {
    if (toolsTree == null || toolsTree.getScene() == null) {
      return;
    }
    Window window = toolsTree.getScene().getWindow();
    if (window != null) {
      window.hide();
    }
  }

  /**
   * Custom cell rendering each tool row as: [checkbox | tool name | edition combo | version combo | error icon]. Group header rows carry a null value and are
   * rendered via the graphic set on their TreeItem instead.
   */
  private static final class ToolTreeCell extends TreeCell<ToolConfiguration> {

    private final HBox root;
    private final ToolSettingsController controller;
    private CheckBox enabled;
    private Label name;
    private ComboBox<String> edition;
    private ComboBox<String> version;

    ToolTreeCell(ToolSettingsController controller) {
      this.controller = controller;
      this.root = new HBox(10);
      this.root.setAlignment(Pos.CENTER_LEFT);
      this.root.setStyle("-fx-padding: 5 0 5 0;");
    }

    @Override
    protected void updateItem(ToolConfiguration toolItem, boolean empty) {
      super.updateItem(toolItem, empty);
      if (empty) {
        setGraphic(null);
        setText(null);
        return;
      }
      // null value means this is a group header row — delegate to the Label graphic set in createGroupItem().
      if (toolItem == null) {
        TreeItem<ToolConfiguration> treeItem = getTreeItem();
        if (isTopLevelGroupHeader(treeItem)) {
          setGraphic(treeItem.getGraphic());
        } else {
          setGraphic(null);
        }
        setText(null);
        return;
      }

      root.getChildren().clear();

      enabled = createEnabledToggle(toolItem);
      name = createToolNameLabel(toolItem);
      edition = createEditionSelector(toolItem);
      version = createVersionSelector(toolItem);

      Label errorIcon = createErrorIcon();
      attachVersionValidation(toolItem, errorIcon);

      // Reapply error state if this tool has a validation error
      if (validationErrors.contains(toolItem.getToolName())) {
        version.setStyle("-fx-font-size: 10; -fx-border-color: red; -fx-border-width: 2;");
        errorIcon.setVisible(true);
        errorIcon.setManaged(true);
      }

      HBox versionWithIcon = new HBox(5);
      versionWithIcon.setAlignment(Pos.CENTER_LEFT);
      versionWithIcon.getChildren().addAll(version, errorIcon);

      Region spacer = new Region();
      HBox.setHgrow(spacer, Priority.ALWAYS);

      root.getChildren().addAll(enabled, name, edition, versionWithIcon, spacer);
      applyEnabledState(toolItem);
      setGraphic(root);
    }

    // Tree depth: invisible root → group items (depth 1) → tool items (depth 2).
    // Group headers are at depth 1: they have a parent (root) but that parent has no parent.
    private boolean isTopLevelGroupHeader(TreeItem<ToolConfiguration> treeItem) {
      return treeItem != null && treeItem.getParent() != null && treeItem.getParent().getParent() == null;
    }

    private CheckBox createEnabledToggle(ToolConfiguration toolItem) {
      CheckBox enabledToggle = new CheckBox();
      enabledToggle.setPrefWidth(40);
      enabledToggle.setSelected(toolItem.isEnabled());
      enabledToggle.setOnAction(_ -> {
        toolItem.setEnabled(enabledToggle.isSelected());
        applyEnabledState(toolItem);
      });
      return enabledToggle;
    }

    private Label createToolNameLabel(ToolConfiguration toolItem) {
      Label toolNameLabel = new Label(toolItem.getToolName());
      toolNameLabel.setPrefWidth(120);
      toolNameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11;");
      return toolNameLabel;
    }

    private ComboBox<String> createEditionSelector(ToolConfiguration toolItem) {
      ComboBox<String> editionSelector = new ComboBox<>();
      editionSelector.setPrefWidth(130);
      editionSelector.setEditable(true);
      editionSelector.setStyle("-fx-font-size: 10;");

      List<String> editions = toolItem.getAvailableEditions();
      boolean supportsEdition = toolItem.isSupportsEdition() && editions != null && !editions.isEmpty();
      if (supportsEdition) {
        editionSelector.setItems(FXCollections.observableArrayList(editions));
        editionSelector.setValue(toolItem.getConfiguredEdition() == null ? "" : toolItem.getConfiguredEdition());
        editionSelector.setVisible(true);
        editionSelector.setManaged(true);
        // When the edition changes, refresh the version list to match the new edition.
        // Version is captured before the lambda to avoid referencing the mutable field from a background thread.
        editionSelector.setOnAction(e -> {
          String selectedEdition = editionSelector.getValue();
          if (selectedEdition != null && !selectedEdition.isBlank() && currentContext != null) {
            toolItem.setConfiguredEdition(selectedEdition);
            ComboBox<String> capturedVersion = version;
            Thread t = new Thread(() -> {
              List<String> versions = service.loadVersionsForSelectedEdition(toolItem.getToolName(), selectedEdition, currentContext);
              Platform.runLater(() -> capturedVersion.setItems(FXCollections.observableArrayList(versions)));
            });
            t.setDaemon(true);
            t.start();
          }
        });
      } else {
        editionSelector.setVisible(false);
        editionSelector.setManaged(true);
        editionSelector.setDisable(true);
      }
      return editionSelector;
    }

    private ComboBox<String> createVersionSelector(ToolConfiguration toolItem) {
      ComboBox<String> versionSelector = new ComboBox<>();
      versionSelector.setPrefWidth(130);
      versionSelector.setEditable(true);
      versionSelector.setValue(toolItem.getConfiguredVersion() == null ? "" : toolItem.getConfiguredVersion());
      versionSelector.setStyle("-fx-font-size: 10;");

      // Lazy-load versions the first time the dropdown is opened to avoid fetching all tools' versions upfront.
      versionSelector.setOnShowing(e -> {
        if (versionSelector.getItems().isEmpty() && currentContext != null) {
          String edition = toolItem.getConfiguredEdition();
          Thread t = new Thread(() -> {
            List<String> versions = service.loadVersionsForSelectedEdition(toolItem.getToolName(), edition, currentContext);
            Platform.runLater(() -> {
              versionSelector.setItems(FXCollections.observableArrayList(versions));
              toolItem.setAvailableVersions(versions);
              // JavaFX doesn't repaint an already-open popup after its items change;
              // hide/show forces a fresh layout with the newly loaded list.
              if (versionSelector.isShowing()) {
                versionSelector.hide();
                versionSelector.show();
              }
            });
          });
          t.setDaemon(true);
          t.start();
        }
      });

      return versionSelector;
    }

    private Label createErrorIcon() {
      Label errorIcon = new Label("✗");
      errorIcon.setStyle("-fx-text-fill: red; -fx-font-size: 14; -fx-font-weight: bold; -fx-cursor: hand;");
      errorIcon.setPrefWidth(20);
      errorIcon.setVisible(false);
      errorIcon.setManaged(false);

      Tooltip errorTooltip = new Tooltip(LocalizationService.getInstance().get("invalidVersionError"));
      errorIcon.setOnMouseEntered(e -> {
        if (errorIcon.isVisible()) {
          errorTooltip.show(errorIcon, e.getScreenX() + 10, e.getScreenY() + 10);
        }
      });
      errorIcon.setOnMouseExited(e -> errorTooltip.hide());
      return errorIcon;
    }

    // Validate free-text version input on focus-lost.
    // Also handles setting the right selected version in the ToolConfiguration object when the comboBox focus is left
    // Blank input is normalized to "*" (meaning "latest") so the field is never left empty.
    // Validation is skipped when versions haven't been loaded yet (availableVersions == null),
    // and "*" is always allowed as a wildcard regardless of the loaded list.
    private void attachVersionValidation(ToolConfiguration toolItem, Label errorIcon) {
      String errorKey = toolItem.getToolName();
      version.focusedProperty().addListener((obs, oldVal, newVal) -> {
        if (!newVal) {
          String enteredVersion = version.getValue();
          if (enteredVersion == null || enteredVersion.isBlank()) {
            version.setValue("*");
            version.setStyle("-fx-font-size: 10;");
            errorIcon.setVisible(false);
            errorIcon.setManaged(false);
            validationErrors.remove(errorKey);
          } else {
            List<String> availableVersions = toolItem.getAvailableVersions();
            if (availableVersions != null && !availableVersions.contains(enteredVersion) && !enteredVersion.equals("*")) {
              version.setStyle("-fx-font-size: 10; -fx-border-color: red; -fx-border-width: 2;");
              errorIcon.setVisible(true);
              errorIcon.setManaged(true);
              validationErrors.add(errorKey);
            } else {
              version.setStyle("-fx-font-size: 10;");
              errorIcon.setVisible(false);
              errorIcon.setManaged(false);
              validationErrors.remove(errorKey);
            }
          }

          toolItem.setConfiguredVersion(version.getValue());
          controller.updateButtonStates();
        }
      });
    }

    private void applyEnabledState(ToolConfiguration toolItem) {
      double opacity = toolItem.isEnabled() ? 1.0 : 0.6;
      enabled.setOpacity(opacity);
      name.setOpacity(opacity);
      version.setOpacity(opacity);
      edition.setOpacity(opacity);
      version.setDisable(!toolItem.isEnabled());
      edition.setDisable(!toolItem.isEnabled());
    }
  }
}


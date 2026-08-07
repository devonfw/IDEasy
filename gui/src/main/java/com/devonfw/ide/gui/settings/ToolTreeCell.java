package com.devonfw.ide.gui.settings;

import java.util.List;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import com.devonfw.ide.gui.context.IdeGuiContext;

/**
 * Custom {@link TreeCell} for the tool configuration tree. Dispatches on item type:
 * <ul>
 *   <li>{@code null} — group header (graphic delegated to the TreeItem's own graphic)</li>
 *   <li>{@link ToolConfiguration} — tool row: checkbox | name | edition combo | version combo | error icon</li>
 *   <li>{@link PluginConfiguration} — plugin row: checkbox | name | id</li>
 * </ul>
 */
final class ToolTreeCell extends TreeCell<ToolTreeNode> {

  private final HBox root;
  private final ToolSettingsController controller;
  private CheckBox enabled;
  private Label name;
  private ComboBox<String> edition;
  private ComboBox<String> version;
  private Label errorIcon;

  ToolTreeCell(ToolSettingsController controller) {
    this.controller = controller;
    this.root = new HBox(10);
    this.root.setAlignment(Pos.CENTER_LEFT);
    this.root.setStyle("-fx-padding: 5 0 5 0;");
  }

  @Override
  protected void updateItem(ToolTreeNode item, boolean empty) {
    super.updateItem(item, empty);
    if (empty) {
      setGraphic(null);
      setText(null);
      return;
    }
    // null value means this is a group header row — delegate to the Label graphic set in createGroupItem().
    switch (item) {
      case null -> {
        TreeItem<ToolTreeNode> treeItem = getTreeItem();
        if (isTopLevelGroupHeader(treeItem)) {
          setGraphic(treeItem.getGraphic());
        } else {
          setGraphic(null);
        }
        setText(null);
      }
      case ToolConfiguration toolItem -> renderToolRow(toolItem);
      case PluginConfiguration plugin -> renderPluginRow(plugin);
      default -> {
      }
    }
  }

  // Tree depth: invisible root → group items (depth 1) → tool items (depth 2) → plugin items (depth 3).
  // Group headers are at depth 1: they have a parent (root) but that parent has no parent.
  private boolean isTopLevelGroupHeader(TreeItem<ToolTreeNode> treeItem) {
    return treeItem != null && treeItem.getParent() != null && treeItem.getParent().getParent() == null;
  }

  private void renderToolRow(ToolConfiguration toolItem) {
    root.getChildren().clear();

    enabled = createEnabledToggle(toolItem);
    name = createToolNameLabel(toolItem);
    errorIcon = createErrorIcon();
    edition = createEditionSelector(toolItem);
    version = createVersionSelector(toolItem);

    attachVersionValidation(toolItem, errorIcon);

    // Reapply error state if this tool has a validation error
    if (controller.validationErrors.contains(toolItem.getToolName())) {
      version.setStyle("-fx-font-size: 12; -fx-border-color: red; -fx-border-width: 2;");
      errorIcon.setVisible(true);
      errorIcon.setManaged(true);
    }

    HBox versionWithIcon = new HBox(5);
    versionWithIcon.setAlignment(Pos.CENTER_LEFT);
    versionWithIcon.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(version, Priority.ALWAYS);
    versionWithIcon.getChildren().addAll(version, errorIcon);
    HBox.setHgrow(versionWithIcon, Priority.ALWAYS);

    root.getChildren().addAll(enabled, name, edition, versionWithIcon);
    applyEnabledState(toolItem);
    setGraphic(root);
    setText(null);
  }

  private CheckBox createEnabledToggle(ToolConfiguration toolItem) {
    CheckBox enabledToggle = new CheckBox();
    enabledToggle.setPrefWidth(40);
    enabledToggle.setSelected(toolItem.isEnabled());
    enabledToggle.setOnAction(_ -> {
      toolItem.setEnabled(enabledToggle.isSelected());
      applyEnabledState(toolItem);
      // Refresh child plugin cells so they reflect the new parent-enabled state.
      controller.refreshTree();
    });
    return enabledToggle;
  }

  private Label createToolNameLabel(ToolConfiguration toolItem) {
    Label toolNameLabel = new Label(toolItem.getToolName());
    toolNameLabel.setPrefWidth(120);
    toolNameLabel.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(toolNameLabel, Priority.ALWAYS);
    toolNameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
    return toolNameLabel;
  }

  private ComboBox<String> createEditionSelector(ToolConfiguration toolItem) {
    ComboBox<String> editionSelector = new ComboBox<>();
    editionSelector.setPrefWidth(130);
    editionSelector.setMaxWidth(Double.MAX_VALUE);
    editionSelector.setPrefHeight(32);
    HBox.setHgrow(editionSelector, Priority.ALWAYS);
    editionSelector.setEditable(true);
    editionSelector.setStyle("-fx-font-size: 12;");

    List<String> editions = toolItem.getAvailableEditions();
    boolean supportsEdition = toolItem.isSupportsEdition() && editions != null && !editions.isEmpty();
    if (supportsEdition) {
      editionSelector.setItems(FXCollections.observableArrayList(editions));
      editionSelector.setValue(toolItem.getConfiguredEdition() == null ? "" : toolItem.getConfiguredEdition());
      editionSelector.setVisible(true);
      editionSelector.setManaged(true);
      // When the edition changes, reload the version list and re-validate the already-entered version against it
      // (a version valid for the old edition may not be for the new one). Version/errorIcon are captured before
      // the lambda to avoid referencing the mutable fields from a background thread.
      editionSelector.setOnAction(e -> {
        String selectedEdition = editionSelector.getValue();
        if (selectedEdition == null || selectedEdition.isBlank() || controller.currentContext == null) {
          return;
        }
        toolItem.setConfiguredEdition(selectedEdition);
        ComboBox<String> capturedVersion = version;
        Label capturedErrorIcon = errorIcon;
        String errorKey = toolItem.getToolName();

        String enteredVersion = capturedVersion.getValue();
        loadVersions(toolItem, versions -> {
          capturedVersion.setItems(FXCollections.observableArrayList(versions));
          capturedVersion.setValue(enteredVersion);
          toolItem.setConfiguredVersion(enteredVersion);
          applyValidation(errorKey, capturedVersion, capturedErrorIcon, versions);
        });
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
    versionSelector.setMaxWidth(Double.MAX_VALUE);
    versionSelector.setPrefHeight(32);
    versionSelector.setEditable(true);
    versionSelector.setValue(toolItem.getConfiguredVersion() == null ? "" : toolItem.getConfiguredVersion());
    versionSelector.setStyle("-fx-font-size: 12;");

    // Lazy-load versions the first time the dropdown is opened to avoid fetching all tools' versions upfront.
    // Reuse the list if it was already loaded (e.g. by focus-lost validation) instead of fetching again.
    versionSelector.setOnShowing(e -> {
      if (!versionSelector.getItems().isEmpty()) {
        return;
      }
      List<String> cachedVersions = toolItem.getAvailableVersions();
      if (cachedVersions != null) {
        versionSelector.setItems(FXCollections.observableArrayList(cachedVersions));
        return;
      }
      loadVersions(toolItem, versions -> {
        versionSelector.setItems(FXCollections.observableArrayList(versions));
        // JavaFX doesn't repaint an already-open popup after its items change;
        // hide/show forces a fresh layout with the newly loaded list.
        if (versionSelector.isShowing()) {
          versionSelector.hide();
          versionSelector.show();
        }
      });
    });

    return versionSelector;
  }

  private void renderPluginRow(PluginConfiguration plugin) {
    root.getChildren().clear();
    boolean parentEnabled = plugin.isParentEnabled();

    CheckBox activeToggle = new CheckBox();
    activeToggle.setSelected(plugin.isActive());
    activeToggle.setDisable(!parentEnabled);
    activeToggle.setOpacity(parentEnabled ? 1.0 : 0.6);
    activeToggle.setOnAction(_ -> plugin.setActive(activeToggle.isSelected()));

    Label nameLabel = new Label(plugin.getPluginName());
    nameLabel.setStyle("-fx-font-size: 12;");
    nameLabel.setOpacity(parentEnabled ? 1.0 : 0.6);

    root.getChildren().addAll(activeToggle, nameLabel);

    if (plugin.getPluginId() != null && !plugin.getPluginId().isBlank()) {
      Label idLabel = new Label("(" + plugin.getPluginId() + ")");
      idLabel.setStyle("-fx-font-size: 11; -fx-text-fill: gray;");
      idLabel.setOpacity(parentEnabled ? 1.0 : 0.6);
      root.getChildren().add(idLabel);
    }

    setGraphic(root);
    setText(null);
  }

  /**
   * Loads {@code toolItem}'s available versions for its currently configured edition on a background thread, caches them onto {@code toolItem}, then runs
   * {@code onLoaded} with the result on the JavaFX application thread. Used by the version dropdown, focus-lost validation, and edition changes.
   */
  private void loadVersions(ToolConfiguration toolItem, Consumer<List<String>> onLoaded) {
    if (controller.currentContext == null) {
      return;
    }
    String edition = toolItem.getConfiguredEdition();
    IdeGuiContext loadContext = controller.currentContext;
    Thread t = new Thread(() -> {
      List<String> versions = controller.service.loadVersionsForSelectedEdition(toolItem.getToolName(), edition, loadContext);
      toolItem.setAvailableVersions(versions);
      Platform.runLater(() -> onLoaded.accept(versions));
    });
    t.setDaemon(true);
    t.start();
  }

  private Label createErrorIcon() {
    Label errorIcon = new Label("✗");
    errorIcon.setStyle("-fx-text-fill: red; -fx-font-size: 14; -fx-font-weight: bold; -fx-cursor: hand;");
    errorIcon.setPrefWidth(20);
    errorIcon.setVisible(false);
    errorIcon.setManaged(false);

    Tooltip errorTooltip = new Tooltip(controller.getNlsService().get("invalidVersionError"));
    errorIcon.setOnMouseEntered(e -> {
      if (errorIcon.isVisible()) {
        errorTooltip.show(errorIcon, e.getScreenX() + 10, e.getScreenY() + 10);
      }
    });
    errorIcon.setOnMouseExited(e -> errorTooltip.hide());
    return errorIcon;
  }

  // Validate free-text version input on focus-lost, and handle setting the version on the ToolConfiguration.
  // Blank input is normalized to "*" (meaning "latest") so the field is never left empty.
  // If versions were never loaded (dropdown never opened), load them first so typing a value and tabbing/clicking
  // away still validates correctly.
  private void attachVersionValidation(ToolConfiguration toolItem, Label errorIcon) {
    String errorKey = toolItem.getToolName();
    version.focusedProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal) {
        return;
      }
      if (version.getValue() == null || version.getValue().isBlank()) {
        version.setValue("*");
      }
      toolItem.setConfiguredVersion(version.getValue());

      List<String> availableVersions = toolItem.getAvailableVersions();
      if (availableVersions != null) {
        applyValidation(errorKey, version, errorIcon, availableVersions);
      } else {
        loadVersions(toolItem, versions -> applyValidation(errorKey, version, errorIcon, versions));
      }
    });
  }

  /**
   * Validates {@code versionSelector}'s current value (using {@link ToolSettingsService#isValidVersion}) against {@code availableVersions} and updates the
   * border, error icon, the shared {@code validationErrors} set, and the Save/Preview button state accordingly.
   */
  private void applyValidation(String errorKey, ComboBox<String> versionSelector, Label errorIconLabel, List<String> availableVersions) {
    boolean valid = controller.service.isValidVersion(versionSelector.getValue(), availableVersions);
    if (valid) {
      versionSelector.setStyle("-fx-font-size: 12;");
      errorIconLabel.setVisible(false);
      errorIconLabel.setManaged(false);
      controller.validationErrors.remove(errorKey);
    } else {
      versionSelector.setStyle("-fx-font-size: 12; -fx-border-color: red; -fx-border-width: 2;");
      errorIconLabel.setVisible(true);
      errorIconLabel.setManaged(true);
      controller.validationErrors.add(errorKey);
    }
    controller.updateButtonStates();
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

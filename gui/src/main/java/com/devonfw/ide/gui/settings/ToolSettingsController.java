package com.devonfw.ide.gui.settings;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import com.devonfw.ide.gui.nls.NlsService;

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

  private final ToolSettingsService service = new ToolSettingsService();

  private IdeGuiContext currentContext;

  private final Set<String> validationErrors = new HashSet<>();

  private final NlsService nlsService;

  private Runnable onClose;

  /**
   * Constructor
   *
   * @param nlsService the injected {@link NlsService} used for translations.
   * @param currentContext the current {@link IdeGuiContext} providing access to the selected project and workspace.
   */
  public ToolSettingsController(NlsService nlsService, IdeGuiContext currentContext) {
    this.nlsService = nlsService;
    this.currentContext = currentContext;
  }

  NlsService getNlsService() {
    return this.nlsService;
  }


  @FXML
  private void initialize() {
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
    Label toolGroupLabel = new Label(this.nlsService.get(group.getLabel()));
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
    dialog.setTitle(nlsService.get("toolsConfigTitle") + " - " + nlsService.get("toolsConfigPreviewTitle"));

    Button save = new Button(nlsService.get("save"));
    Button close = new Button(nlsService.get("cancel"));
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
        .filter(Objects::nonNull).collect(Collectors.toList());
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

  public void setOnClose(Runnable onClose) {
    this.onClose = onClose;
  }

  private void closeWindow() {
    if (onClose != null) {
      onClose.run();
      return;
    }
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
    private Label errorIcon;

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

      Node editionSlot = edition.isManaged() ? edition : createEditionPlaceholder();
      root.getChildren().addAll(enabled, name, editionSlot, versionWithIcon);
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
        editionSelector.setManaged(false);
        editionSelector.setDisable(true);
      }
      return editionSelector;
    }

    // Empty stand-in occupying the same column width as the edition combo, used for tools that have no edition
    // choice so the Version column stays aligned with rows that do render an edition selector.
    private Region createEditionPlaceholder() {
      Region placeholder = new Region();
      placeholder.setPrefWidth(130);
      placeholder.setMaxWidth(Double.MAX_VALUE);
      HBox.setHgrow(placeholder, Priority.ALWAYS);
      return placeholder;
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
}

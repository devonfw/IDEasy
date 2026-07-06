package com.devonfw.ide.gui.settings;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.ide.gui.context.IdeGuiStateManager;
import com.devonfw.ide.gui.localization.LocalizationService;

/**
 * Controller for the Tools Configuration Tab.
 */
public class ToolSettingsController {

  @FXML
  private TreeView<ToolTreeNode> toolsTree;

  @FXML
  private Button saveButton;

  @FXML
  private Button previewButton;

  @FXML
  private Button addPluginButton;

  static final ToolSettingsService service = new ToolSettingsService();
  
  static IdeGuiContext currentContext;
  // Static so validation errors survive cell recycling — JavaFX reuses TreeCell instances during scroll.
  static final Set<String> validationErrors = new HashSet<>();

  private Runnable onClose;


  @FXML
  private void initialize() {
    currentContext = IdeGuiStateManager.getInstance().getCurrentContext();

    updateButtonStates();
    addPluginButton.setDisable(true);

    List<ToolConfiguration> configurations = service.listToolConfigurations(currentContext);

    toolsTree.setRoot(buildToolTree(configurations));
    toolsTree.setShowRoot(false);
    toolsTree.setCellFactory(tv -> new ToolTreeCell(this));
    toolsTree.getSelectionModel().selectedItemProperty().addListener(
        (_, _, newVal) -> addPluginButton.setDisable(!isPluginCapableSelection(newVal)));

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
            tc.setConfiguredEdition(editions.getFirst());
          }
        }
        Platform.runLater(() -> toolsTree.refresh());
      });
      t.setDaemon(true);
      t.start();
    }
  }

  private TreeItem<ToolTreeNode> buildToolTree(List<ToolConfiguration> toolConfigurations) {
    TreeItem<ToolTreeNode> root = new TreeItem<>();
    root.setExpanded(false);
    for (ToolConfiguration.ToolGroup group : ToolConfiguration.ToolGroup.values()) {
      List<ToolConfiguration> groupTools = toolConfigurations.stream().filter(tc -> tc.getGroup() == group).toList();
      if (!groupTools.isEmpty()) {
        root.getChildren().add(createGroupItem(group, groupTools));
      }
    }
    return root;
  }

  private TreeItem<ToolTreeNode> createGroupItem(ToolConfiguration.ToolGroup group, List<ToolConfiguration> groupTools) {
    Label toolGroupLabel = new Label(group.getLabel());
    toolGroupLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12; -fx-padding: 6 0 6 0;");

    TreeItem<ToolTreeNode> groupItem = new TreeItem<>(null);
    groupItem.setGraphic(toolGroupLabel);
    groupItem.setExpanded(true);
    for (ToolConfiguration toolConfiguration : groupTools) {
      groupItem.getChildren().add(createToolItem(toolConfiguration));
    }
    return groupItem;
  }

  private TreeItem<ToolTreeNode> createToolItem(ToolConfiguration toolConfiguration) {
    TreeItem<ToolTreeNode> toolItem = new TreeItem<>(toolConfiguration);
    if (toolConfiguration.getGroup() == ToolConfiguration.ToolGroup.IDE && currentContext != null) {
      List<PluginConfiguration> plugins = service.loadPluginsForTool(toolConfiguration, currentContext);
      if (!plugins.isEmpty()) {
        for (PluginConfiguration plugin : plugins) {
          toolItem.getChildren().add(new TreeItem<>(plugin));
        }
      }
    }
    return toolItem;
  }

  @FXML
  private void onSave() {
    List<ToolConfiguration> toolConfigurations = collectLeafConfigurations();
    service.applyAndSave(toolConfigurations, currentContext);
    if (currentContext != null) {
      collectPluginConfigurations().forEach(
          plugin -> service.savePluginActive(plugin.getParentToolName(), plugin.getPluginName(), plugin.isActive(), currentContext));
    }
    refreshTree();
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
      if (currentContext != null) {
        collectPluginConfigurations().forEach(
            plugin -> service.savePluginActive(plugin.getParentToolName(), plugin.getPluginName(), plugin.isActive(), currentContext));
      }
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

  private List<PluginConfiguration> collectPluginConfigurations() {
    return toolsTree.getRoot().getChildren().stream()
        .flatMap(groupNode -> groupNode.getChildren().stream())
        .flatMap(toolNode -> toolNode.getChildren().stream())
        .map(TreeItem::getValue)
        .filter(v -> v instanceof PluginConfiguration)
        .map(v -> (PluginConfiguration) v)
        .collect(Collectors.toList());
  }

  private List<ToolConfiguration> collectLeafConfigurations() {
    return toolsTree.getRoot().getChildren().stream()
        .flatMap(groupNode -> groupNode.getChildren().stream())
        .map(TreeItem::getValue)
        .filter(v -> v instanceof ToolConfiguration)
        .map(v -> (ToolConfiguration) v)
        .collect(Collectors.toList());
  }

  @FXML
  private void onAddPlugin() {
    TreeItem<ToolTreeNode> toolItem = resolveToolItem(toolsTree.getSelectionModel().getSelectedItem());
    if (toolItem == null || !(toolItem.getValue() instanceof ToolConfiguration tc)) {
      return;
    }

    boolean urlNeeded = service.isPluginUrlNeeded(tc.getToolName(), currentContext);

    TextField idField = new TextField();
    idField.setPromptText("Required");
    idField.setPrefWidth(250);
    TextField nameField = new TextField();
    nameField.setPromptText("Required");
    nameField.setPrefWidth(250);

    String[] lastSuggestion = { "" };
    idField.textProperty().addListener((obs, old, newId) -> {
      String suggested = suggestPluginName(newId);
      if (nameField.getText().isBlank() || nameField.getText().equals(lastSuggestion[0])) {
        nameField.setText(suggested);
        lastSuggestion[0] = suggested;
      }
    });

    TextField tagsField = new TextField();
    tagsField.setPromptText("e.g. java,lint");
    tagsField.setPrefWidth(250);

    VBox form = new VBox(6, new Label("Plugin ID *"), idField, new Label("Plugin Name *"), nameField,
        new Label("Tags"), tagsField);

    TextField urlField = null;
    if (urlNeeded) {
      urlField = new TextField();
      urlField.setPromptText("Required");
      urlField.setPrefWidth(250);
      form.getChildren().addAll(new Label("Plugin URL *"), urlField);
    }
    TextField capturedUrl = urlField;

    Button okBtn = new Button("OK");
    okBtn.setDefaultButton(true);
    Button cancelBtn = new Button(LocalizationService.getInstance().get("button.cancel"));
    HBox actions = new HBox(8, cancelBtn, okBtn);
    actions.setAlignment(Pos.CENTER_RIGHT);

    VBox content = new VBox(10, form, actions);
    content.setPadding(new Insets(15));

    Stage dialog = new Stage();
    dialog.initModality(Modality.APPLICATION_MODAL);
    dialog.initOwner(toolsTree.getScene().getWindow());
    dialog.setTitle("Add Plugin - " + tc.getToolName());
    dialog.setScene(new Scene(content));

    boolean[] confirmed = { false };
    okBtn.setOnAction(e -> {
      boolean valid = !idField.getText().isBlank() && !nameField.getText().isBlank() && (!urlNeeded || !capturedUrl.getText().isBlank());
      if (valid) {
        confirmed[0] = true;
        dialog.close();
      }
    });
    cancelBtn.setOnAction(e -> dialog.close());
    dialog.showAndWait();

    if (!confirmed[0]) {
      return;
    }
    String url = capturedUrl != null ? capturedUrl.getText().trim() : null;
    String tags = tagsField.getText().trim().isEmpty() ? null : tagsField.getText().trim();
    PluginConfiguration newPlugin = service.createPlugin(
        tc.getToolName(), nameField.getText().trim(), idField.getText().trim(), url, tags, tc, currentContext);
    if (newPlugin != null) {
      toolItem.getChildren().add(new TreeItem<>(newPlugin));
      toolItem.setExpanded(true);
      toolsTree.refresh();
    }
  }

  private boolean isPluginCapableSelection(TreeItem<ToolTreeNode> item) {
    if (item == null || currentContext == null) {
      return false;
    }
    ToolTreeNode value = item.getValue();
    if (value instanceof ToolConfiguration tc) {
      return tc.isPluginBased();
    }
    return value instanceof PluginConfiguration;
  }

  private TreeItem<ToolTreeNode> resolveToolItem(TreeItem<ToolTreeNode> selected) {
    if (selected == null) {
      return null;
    }
    ToolTreeNode value = selected.getValue();
    if (value instanceof ToolConfiguration) {
      return selected;
    }
    if (value instanceof PluginConfiguration) {
      return selected.getParent();
    }
    return null;
  }

  private static String suggestPluginName(String id) {
    if (id == null || id.isBlank()) {
      return "";
    }
    int lastDot = id.lastIndexOf('.');
    return lastDot >= 0 ? id.substring(lastDot + 1) : id;
  }

  @FXML
  private void onCancel() {
    closeWindow();
  }

  void updateButtonStates() {
    boolean hasErrors = !validationErrors.isEmpty();
    saveButton.setDisable(hasErrors);
    previewButton.setDisable(hasErrors);
  }

  void refreshTree() {
    toolsTree.refresh();
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

}


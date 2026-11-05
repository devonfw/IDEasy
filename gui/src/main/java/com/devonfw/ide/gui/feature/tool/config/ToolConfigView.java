package com.devonfw.ide.gui.feature.tool.config;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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

import com.devonfw.ide.gui.core.service.NlsService;
import com.devonfw.ide.gui.core.service.ToolConfiguration;
import com.devonfw.ide.gui.core.tab.TabView;

/**
 * View for the tool configuration tab. It shows the tools grouped by type, each row as: [checkbox | tool name | edition combo | version combo | error icon].
 */
public class ToolConfigView extends TabView {

  private static final String VERSION_STYLE = "-fx-font-size: 12;";

  private static final String INVALID_VERSION_STYLE = VERSION_STYLE + " -fx-border-color: red; -fx-border-width: 2;";

  @FXML
  private TreeView<ToolConfiguration> toolsTree;

  @FXML
  private Button saveButton;

  @FXML
  private Button previewButton;

  private final ToolConfigViewModel viewModel;

  private final NlsService nlsService;

  /**
   * Creates the tool configuration view and loads its FXML.
   *
   * @param viewModel the view model of the tool configuration tab.
   * @param nlsService the localization service.
   */
  public ToolConfigView(ToolConfigViewModel viewModel, NlsService nlsService) {
    this.viewModel = viewModel;
    this.nlsService = nlsService;
    loadFxml("ToolConfig.fxml", nlsService);
  }

  @FXML
  private void initialize() {

    this.toolsTree.setShowRoot(false);
    this.toolsTree.setCellFactory(_ -> new ToolTreeCell());
    this.toolsTree.setRoot(buildToolTree(this.viewModel.getTools()));
    this.viewModel.getTools().addListener((ListChangeListener<ToolConfiguration>) _ -> this.toolsTree.setRoot(buildToolTree(this.viewModel.getTools())));
    // editions are loaded in the background, so the rows have to be re-rendered once they are available
    this.viewModel.editionsLoadedProperty().addListener((_, _, loaded) -> {
      if (loaded) {
        this.toolsTree.refresh();
      }
    });

    this.saveButton.disableProperty().bind(this.viewModel.saveDisabledProperty());
    this.saveButton.setOnAction(_ -> this.viewModel.save());
    this.previewButton.disableProperty().bind(this.viewModel.saveDisabledProperty());
    this.previewButton.setOnAction(_ -> showPreview());
  }

  private TreeItem<ToolConfiguration> buildToolTree(List<ToolConfiguration> tools) {

    TreeItem<ToolConfiguration> root = new TreeItem<>();
    for (ToolConfiguration.ToolGroup group : ToolConfiguration.ToolGroup.values()) {
      List<ToolConfiguration> groupTools = tools.stream().filter(tool -> tool.getGroup() == group).toList();
      if (!groupTools.isEmpty()) {
        root.getChildren().add(createGroupItem(group, groupTools));
      }
    }
    return root;
  }

  private TreeItem<ToolConfiguration> createGroupItem(ToolConfiguration.ToolGroup group, List<ToolConfiguration> groupTools) {

    Label groupLabel = new Label(this.nlsService.get(group.getLabel()));
    groupLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12; -fx-padding: 6 0 6 0;");

    TreeItem<ToolConfiguration> groupItem = new TreeItem<>(null);
    groupItem.setGraphic(groupLabel);
    groupItem.setExpanded(true);
    for (ToolConfiguration tool : groupTools) {
      groupItem.getChildren().add(new TreeItem<>(tool));
    }
    return groupItem;
  }

  private void showPreview() {

    TextArea area = new TextArea(this.viewModel.getPreview());
    area.setEditable(false);
    area.setPrefWidth(800);
    area.setPrefHeight(400);

    Stage dialog = new Stage();
    dialog.initModality(Modality.APPLICATION_MODAL);
    dialog.initOwner(getScene().getWindow());
    dialog.setTitle(this.nlsService.get("tool_config.preview_title"));

    Button save = new Button(this.nlsService.get("tool_config.save"));
    save.setOnAction(_ -> {
      this.viewModel.save();
      dialog.close();
    });
    Button close = new Button(this.nlsService.get("tool_config.cancel"));
    close.setOnAction(_ -> dialog.close());

    HBox actions = new HBox(8, close, save);
    actions.setAlignment(Pos.CENTER_RIGHT);
    VBox box = new VBox(8, area, actions);
    box.setPadding(new Insets(10));

    dialog.setScene(new Scene(box));
    dialog.showAndWait();
  }

  /**
   * Cell rendering a tool row. Group header rows carry a {@code null} value and are rendered via the graphic set on their {@link TreeItem} instead.
   */
  private final class ToolTreeCell extends TreeCell<ToolConfiguration> {

    private final HBox root;

    private CheckBox enabled;

    private Label name;

    private ComboBox<String> edition;

    private ComboBox<String> version;

    private Label errorIcon;

    ToolTreeCell() {
      this.root = new HBox(10);
      this.root.setAlignment(Pos.CENTER_LEFT);
      this.root.setStyle("-fx-padding: 5 0 5 0;");
    }

    @Override
    protected void updateItem(ToolConfiguration tool, boolean empty) {

      super.updateItem(tool, empty);
      setText(null);
      if (empty) {
        setGraphic(null);
        return;
      }
      if (tool == null) {
        setGraphic(getTreeItem() == null ? null : getTreeItem().getGraphic());
        return;
      }

      this.enabled = createEnabledToggle(tool);
      this.name = createToolNameLabel(tool);
      this.errorIcon = createErrorIcon();
      this.version = createVersionSelector(tool);
      this.edition = createEditionSelector(tool);
      showValidation(tool, this.version, this.errorIcon);

      HBox versionWithIcon = new HBox(5, this.version, this.errorIcon);
      versionWithIcon.setAlignment(Pos.CENTER_LEFT);
      versionWithIcon.setMaxWidth(Double.MAX_VALUE);
      HBox.setHgrow(this.version, Priority.ALWAYS);
      HBox.setHgrow(versionWithIcon, Priority.ALWAYS);

      // tools without an edition choice get an empty placeholder so the version column stays aligned
      Node editionSlot = this.edition.isManaged() ? this.edition : createEditionPlaceholder();
      this.root.getChildren().setAll(this.enabled, this.name, editionSlot, versionWithIcon);
      applyEnabledState(tool);
      setGraphic(this.root);
    }

    private CheckBox createEnabledToggle(ToolConfiguration tool) {

      CheckBox enabledToggle = new CheckBox();
      enabledToggle.setPrefWidth(40);
      enabledToggle.setSelected(tool.isEnabled());
      enabledToggle.setOnAction(_ -> {
        tool.setEnabled(enabledToggle.isSelected());
        applyEnabledState(tool);
      });
      return enabledToggle;
    }

    private Label createToolNameLabel(ToolConfiguration tool) {

      Label toolNameLabel = new Label(tool.getToolName());
      toolNameLabel.setPrefWidth(120);
      toolNameLabel.setMaxWidth(Double.MAX_VALUE);
      HBox.setHgrow(toolNameLabel, Priority.ALWAYS);
      toolNameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
      return toolNameLabel;
    }

    private ComboBox<String> createEditionSelector(ToolConfiguration tool) {

      ComboBox<String> editionSelector = createComboBox();
      List<String> editions = tool.getAvailableEditions();
      if (tool.isSupportsEdition() && editions != null && !editions.isEmpty()) {
        editionSelector.setItems(FXCollections.observableArrayList(editions));
        editionSelector.setValue(tool.getConfiguredEdition() == null ? "" : tool.getConfiguredEdition());
        ComboBox<String> versionSelector = this.version;
        Label versionErrorIcon = this.errorIcon;
        editionSelector.setOnAction(_ -> ToolConfigView.this.viewModel.changeEdition(tool, editionSelector.getValue(), () -> {
          versionSelector.setItems(FXCollections.observableArrayList(tool.getAvailableVersions()));
          showValidation(tool, versionSelector, versionErrorIcon);
        }));
      } else {
        editionSelector.setVisible(false);
        editionSelector.setManaged(false);
        editionSelector.setDisable(true);
      }
      return editionSelector;
    }

    private Region createEditionPlaceholder() {

      Region placeholder = new Region();
      placeholder.setPrefWidth(130);
      placeholder.setMaxWidth(Double.MAX_VALUE);
      HBox.setHgrow(placeholder, Priority.ALWAYS);
      return placeholder;
    }

    private ComboBox<String> createVersionSelector(ToolConfiguration tool) {

      ComboBox<String> versionSelector = createComboBox();
      versionSelector.setValue(tool.getConfiguredVersion() == null ? "" : tool.getConfiguredVersion());

      // lazy-load the versions the first time the dropdown is opened to avoid fetching the versions of all tools upfront
      versionSelector.setOnShowing(_ -> {
        if (!versionSelector.getItems().isEmpty()) {
          return;
        }
        if (tool.getAvailableVersions() != null) {
          versionSelector.setItems(FXCollections.observableArrayList(tool.getAvailableVersions()));
          return;
        }
        ToolConfigView.this.viewModel.loadVersions(tool, () -> {
          versionSelector.setItems(FXCollections.observableArrayList(tool.getAvailableVersions()));
          // JavaFX does not repaint an already open popup after its items change, so hide/show it to display the loaded list
          if (versionSelector.isShowing()) {
            versionSelector.hide();
            versionSelector.show();
          }
        });
      });

      // validate free-text input once the user leaves the field
      Label versionErrorIcon = this.errorIcon;
      versionSelector.focusedProperty().addListener((_, _, focused) -> {
        if (!focused) {
          ToolConfigView.this.viewModel.changeVersion(tool, versionSelector.getValue(), () -> showValidation(tool, versionSelector, versionErrorIcon));
          versionSelector.setValue(tool.getConfiguredVersion());
        }
      });
      return versionSelector;
    }

    private ComboBox<String> createComboBox() {

      ComboBox<String> comboBox = new ComboBox<>();
      comboBox.setPrefWidth(130);
      comboBox.setMaxWidth(Double.MAX_VALUE);
      comboBox.setPrefHeight(32);
      HBox.setHgrow(comboBox, Priority.ALWAYS);
      comboBox.setEditable(true);
      comboBox.setStyle(VERSION_STYLE);
      return comboBox;
    }

    private Label createErrorIcon() {

      Label icon = new Label("✗");
      icon.setStyle("-fx-text-fill: red; -fx-font-size: 14; -fx-font-weight: bold; -fx-cursor: hand;");
      icon.setPrefWidth(20);
      icon.setTooltip(new Tooltip(ToolConfigView.this.nlsService.get("tool_config.invalid_version")));
      return icon;
    }

    private void showValidation(ToolConfiguration tool, ComboBox<String> versionSelector, Label versionErrorIcon) {

      boolean invalid = ToolConfigView.this.viewModel.isInvalid(tool);
      versionSelector.setStyle(invalid ? INVALID_VERSION_STYLE : VERSION_STYLE);
      versionErrorIcon.setVisible(invalid);
      versionErrorIcon.setManaged(invalid);
    }

    private void applyEnabledState(ToolConfiguration tool) {

      double opacity = tool.isEnabled() ? 1.0 : 0.6;
      this.enabled.setOpacity(opacity);
      this.name.setOpacity(opacity);
      this.version.setOpacity(opacity);
      this.edition.setOpacity(opacity);
      this.version.setDisable(!tool.isEnabled());
      this.edition.setDisable(!tool.isEnabled());
    }
  }

}

package com.devonfw.ide.gui.feature.tool.config;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import com.devonfw.ide.gui.core.model.ToolConfiguration;
import com.devonfw.ide.gui.core.service.NlsService;

/**
 * Cell rendering a tool row of the {@link ToolConfigView} as: [checkbox | tool name | edition combo | version combo | error icon].
 */
final class ToolListCell extends ListCell<ToolConfiguration> {

  private static final String VERSION_STYLE = "-fx-font-size: 12;";

  private static final String INVALID_VERSION_STYLE = VERSION_STYLE + " -fx-border-color: red; -fx-border-width: 2;";

  private final ToolConfigViewModel viewModel;

  private final NlsService nlsService;

  private final HBox root;

  private CheckBox enabled;

  private Label name;

  private ComboBox<String> edition;

  private ComboBox<String> version;

  private Label errorIcon;

  /**
   * The constructor.
   *
   * @param viewModel the view model of the tool configuration tab.
   * @param nlsService the localization service.
   */
  ToolListCell(ToolConfigViewModel viewModel, NlsService nlsService) {
    this.viewModel = viewModel;
    this.nlsService = nlsService;
    this.root = new HBox(10);
    this.root.setAlignment(Pos.CENTER_LEFT);
    this.root.setStyle("-fx-padding: 5 0 5 0;");
  }

  @Override
  protected void updateItem(ToolConfiguration tool, boolean empty) {

    super.updateItem(tool, empty);
    setText(null);
    if (empty || (tool == null)) {
      setGraphic(null);
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
      editionSelector.setOnAction(_ -> this.viewModel.changeEdition(tool, editionSelector.getValue(), () -> {
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
      this.viewModel.loadVersions(tool, () -> {
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
        this.viewModel.changeVersion(tool, versionSelector.getValue(), () -> showValidation(tool, versionSelector, versionErrorIcon));
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
    icon.setTooltip(new Tooltip(this.nlsService.get("tool_config.invalid_version")));
    return icon;
  }

  private void showValidation(ToolConfiguration tool, ComboBox<String> versionSelector, Label versionErrorIcon) {

    boolean invalid = this.viewModel.isInvalid(tool);
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

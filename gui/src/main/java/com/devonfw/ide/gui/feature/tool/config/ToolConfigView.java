package com.devonfw.ide.gui.feature.tool.config;

import java.util.List;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.devonfw.ide.gui.core.model.ToolConfiguration;
import com.devonfw.ide.gui.core.service.NlsService;
import com.devonfw.ide.gui.core.tab.TabView;

/**
 * View for the tool configuration tab. It shows the tools grouped by type, each row as: [checkbox | tool name | edition combo | version combo | error icon].
 */
public class ToolConfigView extends TabView {

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
    this.toolsTree.setCellFactory(_ -> new ToolTreeCell(this.viewModel, this.nlsService));
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

}

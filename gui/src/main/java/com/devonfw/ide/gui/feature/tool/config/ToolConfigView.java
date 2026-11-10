package com.devonfw.ide.gui.feature.tool.config;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import com.devonfw.ide.gui.core.model.ToolConfiguration;
import com.devonfw.ide.gui.core.service.NlsService;
import com.devonfw.ide.gui.core.tab.TabView;

/**
 * View for the tool configuration tab. It shows the tools as a list sorted by name, each row rendered by a {@link ToolListCell}.
 */
public class ToolConfigView extends TabView {

  @FXML
  private ListView<ToolConfiguration> toolsList;

  @FXML
  private Button saveButton;

  @FXML
  private Label savedLabel;

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

    this.toolsList.setCellFactory(_ -> new ToolListCell(this.viewModel, this.nlsService));
    this.toolsList.setItems(this.viewModel.getTools());
    // editions are loaded in the background, so the rows have to be re-rendered once they are available
    this.viewModel.editionsLoadedProperty().addListener((_, _, loaded) -> {
      if (loaded) {
        this.toolsList.refresh();
      }
    });

    this.saveButton.disableProperty().bind(this.viewModel.saveDisabledProperty());
    this.saveButton.setOnAction(_ -> this.viewModel.save());
    this.savedLabel.visibleProperty().bind(this.viewModel.savedProperty());
  }

}

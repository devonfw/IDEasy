package com.devonfw.ide.gui.factory;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Locale;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.ide.gui.HeadlessApplicationTest;
import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.service.CommandletService;
import com.devonfw.ide.gui.service.NlsService;
import com.devonfw.ide.gui.ui.controls.console.ConsoleController;

/**
 * Tests for {@link TabFactory} focusing on how a tab is identified for de-duplication. A tab must be identified by its stable NLS key, not by its
 * (locale-dependent, translated) title text.
 */
class TabFactoryTest extends HeadlessApplicationTest {

  @TempDir
  Path ideRoot;

  private static final String LAUNCHER_TITLE_KEY = "tab.ide_launcher";

  private TabPane tabPane;

  private int tabCount;

  private Object userData;

  private String titleText;

  private boolean sameInstance;

  private boolean selectionRestoredToExisting;

  @Override
  public void start(Stage stage) {
    stage.setScene(new Scene(new Group()));
    stage.show();
  }

  /**
   * Builds a {@link TabFactory} and opens the IDE launcher tab twice, capturing the resulting tab count, the tab's stored user data and title, whether both
   * opens produced the same {@link Tab} instance, and whether the selection is restored to the existing tab on the second open.
   */
  private void openLauncherTabTwice() {
    interact(() -> {
      GuiStateManager guiStateManager = new GuiStateManager(new TaskManager(), this.ideRoot.toString());
      NlsService nlsService = new NlsService(Locale.ENGLISH);
      ConsoleController consoleController = new ConsoleController(nlsService);
      CommandletService commandletService = new CommandletService(guiStateManager, consoleController);
      TabFactory factory = new TabFactory(guiStateManager, nlsService, commandletService, consoleController);
      TabPane tabPane = new TabPane();
      factory.attach(tabPane);
      this.tabPane = tabPane;

      factory.openLauncherTab();
      Tab first = tabPane.getTabs().get(0);

      // Deselect so the second open has to actively re-select the existing tab
      tabPane.getSelectionModel().clearSelection();
      factory.openLauncherTab();

      Tab second = tabPane.getTabs().get(0);
      this.tabCount = tabPane.getTabs().size();
      this.userData = second.getUserData();
      this.titleText = second.getText();
      this.sameInstance = (first == second);
      this.selectionRestoredToExisting = tabPane.getSelectionModel().getSelectedItem() == second;
    });
  }

  /**
   * The opened tab must carry its NLS key as its stable identity, kept distinct from its rendered (translated) title, so de-duplication does not depend on the
   * rendered title.
   */
  @Test
  void testOpenStoresTheTitleKeyAsTheTabIdentity() {
    openLauncherTabTwice();

    assertThat(this.userData).as("The tab must be identifiable by its NLS key, not its translated title").isEqualTo(LAUNCHER_TITLE_KEY);
    assertThat(this.titleText).as("The visible title must be the localized text, not the raw key").isNotEqualTo(LAUNCHER_TITLE_KEY);
  }

  /**
   * Opening the launcher tab twice must not create a duplicate tab; the second open must focus the existing one.
   */
  @Test
  void testOpenTwiceDoesNotCreateADuplicateTab() {
    openLauncherTabTwice();

    assertThat(this.tabCount).as("Opening the same tab twice must not create a second tab").isEqualTo(1);
    assertThat(this.sameInstance).as("Opening the tab again must return to the existing tab").isTrue();
    assertThat(this.selectionRestoredToExisting).as("Opening an already-open tab must select it").isTrue();
  }

}

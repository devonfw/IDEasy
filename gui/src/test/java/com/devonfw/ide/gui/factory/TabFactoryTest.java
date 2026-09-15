package com.devonfw.ide.gui.factory;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.devonfw.ide.gui.ui.tab.TabComponent;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.ide.gui.HeadlessApplicationTest;
import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.event.GuiEventBus;
import com.devonfw.ide.gui.event.TabChangeEvent;
import com.devonfw.ide.gui.service.CommandletService;
import com.devonfw.ide.gui.service.NlsService;
import com.devonfw.ide.gui.ui.controls.console.ConsoleViewModel;

/**
 * Tests for {@link TabFactory} focusing on how a tab is identified for de-duplication. A tab must be identified by its stable NLS key, not by its
 * (locale-dependent, translated) title text.
 *
 * <p>The factory no longer owns a {@link javafx.scene.control.TabPane}; each {@link TabFactory#open(TabComponent)} publishes a
 * {@link TabChangeEvent} on the {@link GuiEventBus}. These tests subscribe to the bus and assert on the emitted tabs.
 */
class TabFactoryTest extends HeadlessApplicationTest {

  @TempDir
  Path ideRoot;

  private static final String LAUNCHER_TITLE_KEY = "tab.ide_launcher";

  private TabPane tabPane;

  private int distinctTabCount;

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
   * Builds a {@link TabFactory} wired to a bus with a capturing listener, opens two tabs with the given keys, and captures the emitted tabs.
   */
  private void openLauncherTabTwice() {
    interact(() -> {
      TabFactory factory = getFactory();
      TabPane tabPane = new TabPane();
      this.tabPane = tabPane;

      factory.openLauncherTab();
      Tab first = tabPane.getTabs().get(0);

      // Deselect so the second open has to actively re-select the existing tab
      tabPane.getSelectionModel().clearSelection();
      factory.openLauncherTab();

      Tab second = tabPane.getTabs().get(0);
      this.distinctTabCount = tabPane.getTabs().size();
      this.userData = second.getUserData();
      this.titleText = second.getText();
      this.sameInstance = (first == second);
      this.selectionRestoredToExisting = tabPane.getSelectionModel().getSelectedItem() == second;
    });
  }

  private @NonNull TabFactory getFactory() {
    GuiStateManager guiStateManager = new GuiStateManager(new TaskManager(), this.ideRoot.toString());
    NlsService nlsService = new NlsService(Locale.ENGLISH);
    ConsoleViewModel consoleViewModel = new ConsoleViewModel();
    CommandletService commandletService = new CommandletService(guiStateManager, consoleViewModel);
    GuiEventBus eventBus = new GuiEventBus();
    List<Tab> emitted = new ArrayList<>();
    eventBus.addListener(TabChangeEvent.class, event -> emitted.add(event.tab()));
    TabFactory factory = new TabFactory(guiStateManager, nlsService, commandletService, consoleViewModel, eventBus);
    return factory;
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

    assertThat(this.distinctTabCount).as("Opening the same tab twice must not create a second tab").isEqualTo(1);
    assertThat(this.sameInstance).as("Opening the tab again must return to the existing tab").isTrue();
    assertThat(this.selectionRestoredToExisting).as("Opening an already-open tab must select it").isTrue();
  }

}

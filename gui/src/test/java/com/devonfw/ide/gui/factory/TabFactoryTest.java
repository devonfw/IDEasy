package com.devonfw.ide.gui.factory;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

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
import com.devonfw.ide.gui.ui.mainwindow.MainWindowView;

/**
 * Tests for {@link TabFactory} focusing on how a tab is identified for de-duplication. A tab must be identified by its stable NLS key, not by its
 * (locale-dependent, translated) title text.
 *
 * <p>The factory no longer owns a {@link javafx.scene.control.TabPane}; each open publishes a {@link TabChangeEvent} on the {@link GuiEventBus}. These tests
 * subscribe to the bus and assert on the emitted tabs. De-duplication now happens in the main window's tab handling (see
 * {@link MainWindowView#selectOrAdd(TabPane, Tab)}), which the duplicate-tab test drives directly.
 */
class TabFactoryTest extends HeadlessApplicationTest {

  @TempDir
  Path ideRoot;

  private static final String LAUNCHER_TITLE_KEY = "tab.ide_launcher";

  private TabPane tabPane;

  private int distinctTabCount;

  private boolean sameInstance;

  private boolean selectionRestoredToExisting;

  /**
   * Builds a {@link TabFactory} wired to the given bus with a capturing listener that appends every emitted tab to {@code emitted}.
   */
  private @NonNull TabFactory getFactory(GuiEventBus eventBus, List<Tab> emitted) {
    GuiStateManager guiStateManager = new GuiStateManager(new TaskManager(), this.ideRoot.toString());
    NlsService nlsService = new NlsService(Locale.ENGLISH);
    CommandletService commandletService = new CommandletService(guiStateManager, eventBus);
    eventBus.addListener(TabChangeEvent.class, event -> emitted.add(event.tab()));
    return new TabFactory(guiStateManager, nlsService, commandletService, eventBus);
  }

  /**
   * The opened tab must carry its NLS key as its stable identity, kept distinct from its rendered (translated) title, so de-duplication does not depend on the
   * rendered title.
   */
  @Test
  void testOpenStoresTheTitleKeyAsTheTabIdentity() {
    GuiEventBus eventBus = new GuiEventBus();
    List<Tab> emitted = new ArrayList<>();

    interact(() -> {
      TabFactory factory = getFactory(eventBus, emitted);
      factory.openLauncherTab();
    });

    assertThat(emitted).hasSize(1);
    Tab tab = emitted.get(0);
    assertThat(tab.getUserData()).as("The tab must be identifiable by its NLS key, not its translated title").isEqualTo(LAUNCHER_TITLE_KEY);
    assertThat(tab.getText()).as("The visible title must be the localized text, not the raw key").isNotEqualTo(LAUNCHER_TITLE_KEY);
  }

  /**
   * Opening the launcher tab twice must not create a duplicate tab; the second open must focus the existing one.
   */
  @Test
  void testOpenTwiceDoesNotCreateADuplicateTab() {
    GuiEventBus eventBus = new GuiEventBus();
    List<Tab> emitted = new ArrayList<>();

    interact(() -> {
      TabFactory factory = getFactory(eventBus, emitted);
      this.tabPane = new TabPane();

      factory.openLauncherTab();
      factory.openLauncherTab();

      // First open adds the launcher tab.
      MainWindowView.selectOrAdd(this.tabPane, emitted.get(0));
      Tab first = this.tabPane.getTabs().get(0);

      // Deselect so the second open has to actively re-select the existing tab.
      this.tabPane.getSelectionModel().clearSelection();
      // Second open is a fresh Tab with the same identity and must focus the existing tab, not add a duplicate.
      MainWindowView.selectOrAdd(this.tabPane, emitted.get(1));

      this.distinctTabCount = this.tabPane.getTabs().size();
      this.sameInstance = first == this.tabPane.getTabs().get(0);
      this.selectionRestoredToExisting = this.tabPane.getSelectionModel().getSelectedItem() == first;
    });

    assertThat(emitted).hasSize(2);
    // Both opens emit the same logical tab (same identity) even though each is a freshly created Tab instance.
    assertThat(emitted.get(0).getUserData()).isEqualTo(LAUNCHER_TITLE_KEY);
    assertThat(emitted.get(1).getUserData()).isEqualTo(LAUNCHER_TITLE_KEY);
    assertThat(this.distinctTabCount).as("Opening the same tab twice must not create a second tab").isEqualTo(1);
    assertThat(this.sameInstance).as("Opening the tab again must return to the existing tab").isTrue();
    assertThat(this.selectionRestoredToExisting).as("Opening an already-open tab must select it").isTrue();
  }

}

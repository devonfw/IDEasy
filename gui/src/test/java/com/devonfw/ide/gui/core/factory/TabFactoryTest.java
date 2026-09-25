package com.devonfw.ide.gui.core.factory;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.ide.gui.HeadlessApplicationTest;
import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.event.GuiEventBus;
import com.devonfw.ide.gui.core.event.TabChangeEvent;

/**
 * Tests for {@link TabFactory}.      <
 */
class TabFactoryTest extends HeadlessApplicationTest {

  @TempDir
  Path ideRoot;

  private static final String LAUNCHER_TITLE_KEY = "tab.ide_launcher";

  private List<Tab> emittedTabs;

  @Override
  public void start(Stage stage) {
    stage.setScene(new Scene(new Group()));
    stage.show();
  }

  /**
   * Builds a {@link TabFactory} wired to a bus with a capturing listener, opens the IDE launcher tab {@code times} times and captures the emitted tabs.
   */
  private void openLauncherTab(int times) {
    interact(() -> {
      GuiStateManager guiStateManager = new GuiStateManager(this.ideRoot.toString());
      GuiEventBus eventBus = guiStateManager.getEventBus();
      this.emittedTabs = new ArrayList<>();
      eventBus.addListener(TabChangeEvent.class, event -> this.emittedTabs.add(event.tab()));
      TabFactory factory = guiStateManager.getTabFactory();

      for (int i = 0; i < times; i++) {
        factory.openLauncherTab();
      }
    });
  }

  /**
   * The opened tab must carry its NLS key as its stable identity, kept distinct from its rendered (translated) title, so de-duplication does not depend on the
   * rendered title.
   */
  @Test
  void testOpenStoresTheTitleKeyAsTheTabIdentity() {
    openLauncherTab(1);

    Tab tab = this.emittedTabs.get(0);
    assertThat(tab.getUserData()).as("The tab must be identifiable by its NLS key, not its translated title").isEqualTo(LAUNCHER_TITLE_KEY);
    assertThat(tab.getText()).as("The visible title must be the localized text, not the raw key").isNotEqualTo(LAUNCHER_TITLE_KEY);
  }

  /**
   * Each open must publish a {@link TabChangeEvent} carrying a fully built tab (with its view as content and closable enabled).
   */
  @Test
  void testOpenPublishesAKeyEventPerOpenWithABuiltTab() {
    openLauncherTab(2);

    assertThat(this.emittedTabs).as("The factory must publish a tab for each open").hasSize(2);
    for (Tab tab : this.emittedTabs) {
      assertThat(tab.getContent()).as("The published tab must carry its view as content").isInstanceOf(Node.class);
      assertThat(tab.isClosable()).as("The published launcher tab must be closable").isTrue();
    }
  }

}

package com.devonfw.ide.gui.factory;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.ide.gui.HeadlessApplicationTest;
import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.service.NlsService;
import com.devonfw.ide.gui.ui.controls.console.ConsoleController;

/**
 * Tests for {@link TabFactory} focusing on how a tab is identified for de-duplication. A tab must be identified by its stable NLS key, not by its
 * (locale-dependent, translated) title text.
 */
class TabFactoryTest extends HeadlessApplicationTest {

  @TempDir
  Path ideRoot;

  private Tab firstTab;

  private Tab secondTab;

  private int tabCount;

  private Object firstUserData;

  private boolean sameInstance;

  @Override
  public void start(Stage stage) {
    stage.setScene(new Scene(new Group()));
    stage.show();
  }

  /**
   * Builds a {@link TabFactory} with the given NLS service and opens two tabs with the given keys, capturing the resulting tab count, the first tab's
   * stored user data, and whether both opens returned the same {@link Tab} instance.
   */
  private void openTwoTabs(NlsService nlsService, String key1, String key2) {
    interact(() -> {
      GuiStateManager guiStateManager = new GuiStateManager(new TaskManager(), this.ideRoot.toString());
      TabFactory factory = new TabFactory(guiStateManager, nlsService, new ConsoleController(nlsService));
      TabPane tabPane = new TabPane();
      factory.attach(tabPane);

      Tab tab1 = factory.open(key1, new Label("content-a"));
      Tab tab2 = factory.open(key2, new Label("content-b"));

      this.firstTab = tab1;
      this.secondTab = tab2;
      this.tabCount = tabPane.getTabs().size();
      this.firstUserData = tab1.getUserData();
      this.sameInstance = (tab1 == tab2);
    });
  }

  /**
   * The opened tab must carry its NLS key as its stable identity so that de-duplication does not depend on the rendered title.
   */
  @Test
  void testOpenStoresTheTitleKeyAsTheTabIdentity() {
    openTwoTabs(new FixedNlsService(Map.of("tab.alpha", "Alpha Title")), "tab.alpha", "tab.alpha");

    assertThat(this.firstUserData).as("The tab must be identifiable by its NLS key, not its translated title").isEqualTo("tab.alpha");
  }

  /**
   * Opening the same key twice must not create a duplicate tab. This guard already holds today (via the rendered title) and must survive the switch to
   * key-based identity.
   */
  @Test
  void testOpenWithTheSameKeyDoesNotCreateADuplicateTab() {
    openTwoTabs(new FixedNlsService(Map.of("tab.alpha", "Alpha Title")), "tab.alpha", "tab.alpha");

    assertThat(this.tabCount).as("Opening the same key twice must not create a second tab").isEqualTo(1);
    assertThat(this.sameInstance).as("Opening the same key again must select the existing tab").isTrue();
  }

  /**
   * Two distinct keys whose translated titles are identical in the current locale must still open as two separate tabs. Identity is by key, never by the
   * rendered title text. This is the regression the title-matching de-duplication would mishandle.
   */
  @Test
  void testDistinctKeysWithIdenticalTitlesAreNotMerged() {
    openTwoTabs(new FixedNlsService(Map.of("tab.alpha", "Same Title", "tab.beta", "Same Title")), "tab.alpha", "tab.beta");

    assertThat(this.tabCount).as("Distinct keys must open as distinct tabs even when their titles translate identically").isEqualTo(2);
    assertThat(this.sameInstance).as("A tab with a different key must not be confused with another tab").isFalse();
  }

  /**
   * An {@link NlsService} that answers a fixed set of keys with caller-controlled text, so a test can force two distinct keys to translate to the same
   * string (the collision the title-based de-duplication cannot disambiguate).
   */
  private static final class FixedNlsService extends NlsService {

    private final Map<String, String> texts;

    private FixedNlsService(Map<String, String> texts) {
      super(Locale.ENGLISH);
      this.texts = texts;
    }

    @Override
    public String get(String key) {
      return this.texts.get(key);
    }
  }
}

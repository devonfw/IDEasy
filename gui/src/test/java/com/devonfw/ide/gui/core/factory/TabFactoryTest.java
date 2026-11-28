package com.devonfw.ide.gui.core.factory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.event.TabChangeEvent;

/**
 * Tests for {@link TabFactory}.
 */
class TabFactoryTest extends Assertions {

  @Test
  void testOpenPublishesTheRequestedTabKey(@TempDir Path ideRoot) {

    GuiStateManager guiStateManager = new GuiStateManager(ideRoot.toString());
    List<String> requestedKeys = new ArrayList<>();
    guiStateManager.getEventBus().addListener(TabChangeEvent.class, event -> requestedKeys.add(event.component().getTabTitleKey()));

    TabFactory factory = guiStateManager.getTabFactory();
    factory.openLauncherTab();
    factory.openToolConfigTab();

    assertThat(requestedKeys).containsExactly("tab.ide_launcher", "tab.tool_config");
  }
}

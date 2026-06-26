package com.devonfw.tools.ide.tool.gui;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Tests for {@link Gui}.
 */
@WireMockTest
class GuiTest extends AbstractIdeContextTest {

  private static final String PROJECT_GUI = "gui";

  @Test
  void testEnableLoggingUsesDefaultProcessMode(WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    IdeTestContext context = newContext(PROJECT_GUI, wmRuntimeInfo);
    Gui gui = new Gui(context);
    gui.enableExtendedLogging.setValue(true);

    // act
    gui.run();

    // assert: with ProcessMode.DEFAULT, process output is captured as INFO log
    assertThat(context).logAtInfo().hasMessage("Scanning for projects...");
  }

  @Test
  void testDisabledLoggingUsesBackgroundSilentMode(WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    IdeTestContext context = newContext(PROJECT_GUI, wmRuntimeInfo);
    Gui gui = new Gui(context);
    // enableExtendedLogging is false by default → ProcessMode.BACKGROUND_SILENT

    // act
    gui.run();

    // assert: with ProcessMode.BACKGROUND_SILENT, process output is suppressed
    assertThat(context).logAtInfo().hasNoMessage("Scanning for projects...");
  }
}

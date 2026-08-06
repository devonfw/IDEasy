package com.devonfw.tools.ide.tool.gui;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link Gui}.
 */
class GuiTest extends AbstractIdeContextTest {

  /**
   * Test that {@link Gui} does not require {@code IDE_HOME}. The GUI is launched from a desktop shortcut, which always starts outside of any IDEasy project.
   * Project selection happens inside the GUI, which reads the available projects from {@code IDE_ROOT}. See
   * <a href="https://github.com/devonfw/IDEasy/issues/2279">#2279</a> for reference.
   */
  @Test
  void testThatIdeHomeIsNotRequired() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Gui gui = new Gui(context);
    // act & assert
    assertThat(gui.isIdeHomeRequired()).isFalse();
  }

  /**
   * Test that {@link Gui} still requires {@code IDE_ROOT}, since the GUI resolves both the launcher {@code pom.xml} and the list of projects relative to it.
   */
  @Test
  void testThatIdeRootIsRequired() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Gui gui = new Gui(context);
    // act & assert
    assertThat(gui.isIdeRootRequired()).isTrue();
  }
}

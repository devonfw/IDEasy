package com.devonfw.tools.ide.tool.yarn;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Integration test of {@link Yarn}.
 */
public class YarnTest extends AbstractIdeContextTest {

  private static final String PROJECT_YARN = "yarn";

  /**
   * Tests if the {@link Yarn} install works correctly.
   */
  @Test
  public void testYarnInstall() {

    // arrange
    IdeTestContext context = newContext(PROJECT_YARN);
    Yarn commandlet = new Yarn(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if the {@link Yarn} uninstall works correctly.
   */
  @Test
  public void testYarnUninstall() {

    // arrange
    IdeTestContext context = newContext(PROJECT_YARN);
    Yarn commandlet = new Yarn(context);

    // act I
    commandlet.install();

    // assert I
    checkInstallation(context);

    // act II
    commandlet.uninstall();

    // assert II
    assertThat(context).logAtInfo().hasMessageContaining("npm uninstall -g corepack");

    assertThat(context).logAtSuccess().hasMessage("Successfully uninstalled yarn");
  }

  /**
   * Tests if {@link Yarn} run works correctly.
   */
  @Test
  public void testYarnRun() {

    // arrange
    IdeTestContext context = newContext(PROJECT_YARN);
    Yarn commandlet = new Yarn(context);
    commandlet.arguments.setValue("--version");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("yarn --version");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtInfo().hasMessageContaining("npm install -g corepack@0.34.0");
    assertThat(context).logAtInfo().hasMessageContaining("corepack prepare yarn@2.4.3 --activate");
    assertThat(context).logAtInfo().hasMessageContaining("corepack install -g yarn@2.4.3");

    assertThat(context).logAtSuccess().hasMessage("Successfully installed yarn in version 2.4.3");
  }
}

package com.devonfw.tools.ide.tool.task;


import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;


/**
 * Test of {@link Task}.
 */
@WireMockTest
class TaskTest extends AbstractIdeContextTest {

  private static final String PROJECT_TASK = "task";

  @Test
  void testTaskInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    IdeTestContext context = newContext(PROJECT_TASK, wireMockRuntimeInfo);
    Task commandlet = new Task(context);

    commandlet.install();

    checkInstallation(context);
  }

  @Test
  void testTaskUninstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    IdeTestContext context = newContext(PROJECT_TASK, wireMockRuntimeInfo);
    Task commandlet = new Task(context);

    commandlet.install();
    checkInstallation(context);

    commandlet.uninstall();

    assertThat(context).logAtInfo().hasMessageContaining("npm uninstall -g @go-task/cli");
    assertThat(context).logAtSuccess().hasMessage("Successfully uninstalled task");
  }

  @Test
  void testTaskRun(WireMockRuntimeInfo wireMockRuntimeInfo) {

    IdeTestContext context = newContext(PROJECT_TASK, wireMockRuntimeInfo);
    Task commandlet = new Task(context);
    commandlet.arguments.setValue("--version");

    commandlet.run();

    assertThat(context).logAtInfo().hasMessageContaining("task --version");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtInfo().hasMessageContaining("npm install -gf @go-task/cli@3.43.3");
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed task in version 3.43.3");
  }
}

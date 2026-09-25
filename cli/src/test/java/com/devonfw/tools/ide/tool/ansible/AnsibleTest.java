package com.devonfw.tools.ide.tool.ansible;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.tool.pip.PipBasedCommandlet;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Ansible}.
 */
@WireMockTest
class AnsibleTest extends AbstractIdeContextTest {

  private static final String PROJECT_PIP = "pip";

  /**
   * Tests that the {@link Ansible} commandlet can be installed via the pip-based installation logic.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testAnsibleInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Ansible commandlet = new Ansible(context);

    // act
    commandlet.install();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("pip install ansible==");
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed ansible");
  }

  /**
   * Tests that {@link Ansible} is implemented as a {@link PipBasedCommandlet} (a foreground CLI tool, not a background IDE tool).
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testAnsibleIsPipBasedCommandlet(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);

    // act
    Ansible commandlet = new Ansible(context);

    // assert
    assertThat(commandlet).isInstanceOf(PipBasedCommandlet.class);
  }

  /**
   * Tests that {@link Ansible} is classified as a configuration-management / python tool.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testAnsibleTags(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP, wireMockRuntimeInfo);

    // act
    Ansible commandlet = new Ansible(context);

    // assert
    assertThat(commandlet.getTags()).contains(Tag.CONFIG_MANAGEMENT, Tag.PYTHON);
  }

}

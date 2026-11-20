package com.devonfw.tools.ide.tool.ansible;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.claude.RecordingEnvironmentContext;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Ansible}.
 */
@WireMockTest
class AnsibleTest extends AbstractIdeContextTest {

  private static final String PROJECT_PIP = "pip";

  /**
   * Tests that the {@link Ansible} commandlet can be installed via the pip-based installation logic resolving the latest stable release (pre-releases such as
   * "12.0.0b1" must be ignored).
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
    assertThat(context).logAtInfo().hasMessageContaining("pip install ansible==11.5.0");
    assertThat(context).log().hasNoMessageContaining("12.0.0b1");
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed ansible");
  }

  /**
   * Tests that uninstalling {@link Ansible} also removes the {@code ansible-core} package that provides the actual binaries.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testAnsibleUninstall(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Ansible commandlet = new Ansible(context);
    commandlet.install();
    // the mock install does not create the binary, so create it to have the same state as after a real install
    Files.createFile(context.getSoftwarePath().resolve("python/bin/ansible"));

    // act
    commandlet.uninstall();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("pip uninstall ansible");
    assertThat(context).logAtInfo().hasMessageContaining("pip uninstall ansible-core");
  }

  /**
   * Tests that installing {@link Ansible} on Windows fails because Ansible does not support Windows as control node.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testAnsibleInstallFailsOnWindows(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    Ansible commandlet = new Ansible(context);

    // act + assert
    assertThatThrownBy(commandlet::install).isInstanceOf(CliException.class).hasMessageContaining("Ansible does not support Windows as control node");
  }

  /**
   * Tests that {@link Ansible#setEnvironment} points {@code ANSIBLE_HOME} to the project-specific config folder instead of the Python installation.
   */
  @Test
  void testAnsibleSetEnvironmentPointsAnsibleHomeToConf() {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP);
    Ansible commandlet = new Ansible(context);
    Path rootDir = context.getSoftwarePath().resolve("ansible");
    ToolInstallation installation =
        new ToolInstallation(rootDir, rootDir, rootDir.resolve("bin"), VersionIdentifier.ofRequired("1.0.0"), false);
    RecordingEnvironmentContext environmentContext = new RecordingEnvironmentContext();

    // act
    commandlet.setEnvironment(environmentContext, installation, false);

    // assert — ANSIBLE_HOME points to project conf/ansible
    assertThat(environmentContext.set).containsEntry("ANSIBLE_HOME", context.getConfPath().resolve("ansible").toString());
  }

  /**
   * Tests that {@link Ansible} is classified as an infrastructure-as-code / python tool.
   */
  @Test
  void testAnsibleTags() {

    // arrange
    IdeTestContext context = newContext(PROJECT_PIP);

    // act
    Ansible commandlet = new Ansible(context);

    // assert
    assertThat(commandlet.getTags()).containsExactlyInAnyOrder(Tag.IAC, Tag.PYTHON);
  }

}

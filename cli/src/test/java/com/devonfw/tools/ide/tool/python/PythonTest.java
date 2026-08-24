package com.devonfw.tools.ide.tool.python;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.environment.VariableSource;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.os.WindowsPathSyntax;
import com.devonfw.tools.ide.process.EnvironmentVariableCollectorContext;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Python}.
 */
@WireMockTest
public class PythonTest extends AbstractIdeContextTest {

  private static final String PROJECT_UV = "uv";


  /**
   * Test that a stale {@code .venv} folder left behind by a previously interrupted installation is removed before {@code uv venv} is invoked, see
   * <a href="https://github.com/devonfw/IDEasy/issues/2313">#2313</a>.
   */
  @Test
  public void testInstallRemovesStaleVenvFolder(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_UV, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    Path staleMarker = context.getSoftwarePath().resolve(Python.VENV_FOLDER).resolve("stale.txt");
    context.getFileAccess().writeFileContent("stale", staleMarker, true);
    Python python = context.getCommandletManager().getCommandlet(Python.class);

    // act
    python.install();

    // assert
    assertThat(context.getSoftwarePath().resolve("python").resolve("stale.txt")).doesNotExist();
    assertThat(context.getSoftwarePath().resolve(".venv")).doesNotExist();
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed python");
  }

  @Test
  public void testInstallOnIntelMacResolvesVersionFromUvNotIdeUrls(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_UV, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    Python python = context.getCommandletManager().getCommandlet(Python.class);

    // act
    python.install();

    // assert
    assertThat(context.getSoftwarePath().resolve("python").resolve(".ide.software.version")).hasContent("3.14.6");
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed python");
  }

  @Test
  public void testSetEnvironment() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Python python = new Python(context);
    Path rootDir = context.getSoftwarePath().resolve("python");
    ToolInstallation toolInstallation = new ToolInstallation(rootDir, rootDir, rootDir.resolve("bin"), VersionIdentifier.of("3.12.0"), true);
    Map<String, VariableLine> variables = new HashMap<>();
    EnvironmentVariableCollectorContext environmentContext = new EnvironmentVariableCollectorContext(variables,
        new VariableSource(EnvironmentVariablesType.WORKSPACE, null), WindowsPathSyntax.MSYS);

    // act
    python.setEnvironment(environmentContext, toolInstallation, false);

    // assert
    assertThat(variables.get("VIRTUAL_ENV").getValue().replace('\\', '/')).endsWith("/software/python");
    assertThat(variables.get("UV_PROJECT_ENVIRONMENT").getValue().replace('\\', '/')).endsWith("/software/python");
  }
}

package com.devonfw.tools.ide.tool.python;

import java.io.IOException;
import java.nio.file.Files;
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
   * Test that installation places the pristine interpreter in the shared software repository and creates the per-project virtual environment, see
   * <a href="https://github.com/devonfw/IDEasy/issues/2561">#2561</a>.
   */
  @Test
  public void testInstallCreatesPristineInterpreterAndPerProjectVenv(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_UV, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Python python = context.getCommandletManager().getCommandlet(Python.class);

    // act
    python.install();

    // assert - the pristine interpreter is shared in the software repository (linked into the per-project software folder)
    Path pythonLink = context.getSoftwarePath().resolve("python");
    assertThat(pythonLink).exists();
    assertThat(context.getFileAccess().toRealPath(pythonLink).getFileName().toString()).isEqualTo("3.14.6");
    // assert - the per-project packages live in a virtual environment inside the project (IDE_HOME), isolated from the pristine interpreter
    assertThat(context.getIdeHome().resolve(Python.VENV_FOLDER).resolve("bin").resolve("python")).exists();
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


  /**
   * Test that (re-)installing the pristine interpreter does not touch the per-project virtual environment, so the project's packages are preserved and projects
   * don't collide (see <a href="https://github.com/devonfw/IDEasy/issues/352">#352</a>).
   *
   * @param wireMockRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException on error.
   */
  @Test
  public void testInstallKeepsPerProjectVenvIsolated(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_UV, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Python python = context.getCommandletManager().getCommandlet(Python.class);
    python.install();
    // simulate a package installed into the per-project virtual environment (inside IDE_HOME)
    Path venvPath = context.getIdeHome().resolve(Python.VENV_FOLDER);
    Path userPackage = venvPath.resolve("lib").resolve("site-packages").resolve("mylib").resolve("__init__.py");
    Files.createDirectories(userPackage.getParent());
    Files.writeString(userPackage, "# installed via pip");

    // act - install the (shared) pristine interpreter again
    python.install();

    // assert - the per-project package is preserved and the interpreter is still detected at the pristine version
    assertThat(userPackage).exists();
    assertThat(venvPath.resolve("bin").resolve("python")).exists();
    assertThat(python.getInstalledVersion()).isEqualTo(VersionIdentifier.of("3.14.6"));
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

    // assert - the project's packages point to a per-project virtual environment (see #352), not to the pristine interpreter
    assertThat(variables.get("VIRTUAL_ENV").getValue().replace('\\', '/')).endsWith("/.venv");
    assertThat(variables.get("UV_PROJECT_ENVIRONMENT").getValue().replace('\\', '/')).endsWith("/.venv");
  }
}

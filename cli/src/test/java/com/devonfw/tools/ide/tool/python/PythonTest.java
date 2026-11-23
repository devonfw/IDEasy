package com.devonfw.tools.ide.tool.python;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliException;
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
    assertThat(python.getInstalledVersion()).isEqualTo(VersionIdentifier.ofRequired("3.14.6"));
  }


  /**
   * Test that on Windows the pristine interpreter keeps its executable resolvable at the installation root (not shadowed by a symlinked {@code bin} folder)
   * while the per-project virtual environment is exposed via {@code bin} (symlinked to the Windows {@code Scripts} folder), see
   * <a href="https://github.com/devonfw/IDEasy/issues/2561">#2561</a>.
   */
  @Test
  public void testInstallOnWindowsKeepsInterpreterAndVenvBinResolvable(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange - simulate the Windows layout of uv (interpreter executable at the installation root, empty "Scripts" folder) so the shared uv mock emits it
    IdeTestContext context = newContext(PROJECT_UV, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.WINDOWS_X64);
    context.setProcessEnv(Map.of("UV_LAYOUT", "windows"));
    Python python = context.getCommandletManager().getCommandlet(Python.class);

    // act
    python.install();

    // assert - the pristine interpreter keeps its executable at the installation root and gets no "bin" folder that would shadow it
    Path pristineInterpreter = context.getFileAccess().toRealPath(context.getSoftwarePath().resolve("python"));
    assertThat(pristineInterpreter.resolve("python")).exists();
    assertThat(pristineInterpreter.resolve("bin")).doesNotExist();
    // assert - the per-project virtual environment exposes its executable via "bin" (symlinked to the Windows "Scripts" folder)
    Path venvPath = context.getIdeHome().resolve(Python.VENV_FOLDER);
    assertThat(venvPath.resolve("Scripts").resolve("python")).exists();
    assertThat(venvPath.resolve("bin").resolve("python")).exists();
  }

  /**
   * Test that {@code ide uninstall python} also removes the per-project virtual environment, so it is not left behind like the uv-based tool store used to be
   * (see <a href="https://github.com/devonfw/IDEasy/issues/2561">issue #2561</a>). The pristine interpreter stays shared in the software repository - only the
   * per-project link and virtual environment are removed.
   *
   * @param wireMockRuntimeInfo the {@link WireMockRuntimeInfo}.
   */
  @Test
  public void testUninstallRemovesPerProjectVenv(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange - install python (creates the shared pristine interpreter and the per-project virtual environment)
    IdeTestContext context = newContext(PROJECT_UV, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Python python = context.getCommandletManager().getCommandlet(Python.class);
    python.install();
    Path venvPath = context.getIdeHome().resolve(Python.VENV_FOLDER);
    Path pythonLink = context.getSoftwarePath().resolve("python");
    Path pristineInterpreter = context.getFileAccess().toRealPath(pythonLink);
    assertThat(venvPath.resolve("bin").resolve("python")).exists();

    // act
    python.uninstall();

    // assert - the per-project virtual environment and the per-project link are gone...
    assertThat(venvPath).doesNotExist();
    assertThat(pythonLink).doesNotExist();
    // assert - but the shared pristine interpreter is kept (it is shared with other projects/versions, so the per-project link must not be followed)
    assertThat(pristineInterpreter).exists();
  }

  /**
   * Test that the user can relocate the per-project virtual environment via {@code PYTHON_VENV_DIR} (e.g. to a specific python module) instead of the default
   * location inside {@code IDE_HOME}.
   *
   * @param wireMockRuntimeInfo the {@link WireMockRuntimeInfo}.
   */
  @Test
  public void testVenvLocationCanBeRelocatedByUser(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange - relocate the virtual environment to a specific location instead of the default IDE_HOME/.venv
    IdeTestContext context = newContext(PROJECT_UV, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Path venvTarget = context.getIdeHome().resolve("venv-relocated");
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("PYTHON_VENV_DIR", venvTarget.toString());
    Python python = context.getCommandletManager().getCommandlet(Python.class);

    // act
    python.install();

    // assert - the virtual environment was created at the relocated location...
    assertThat(venvTarget.resolve("bin").resolve("python")).exists();
    // assert - ...and the default location was not used
    assertThat(context.getIdeHome().resolve(Python.VENV_FOLDER)).doesNotExist();
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed python");
  }

  @Test
  public void testSetEnvironment() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Python python = new Python(context);
    Path rootDir = context.getSoftwarePath().resolve("python");
    ToolInstallation toolInstallation = new ToolInstallation(rootDir, rootDir, rootDir.resolve("bin"), VersionIdentifier.ofRequired("3.12.0"), true);
    Map<String, VariableLine> variables = new HashMap<>();
    EnvironmentVariableCollectorContext environmentContext = new EnvironmentVariableCollectorContext(variables,
        new VariableSource(EnvironmentVariablesType.WORKSPACE, null), WindowsPathSyntax.MSYS);

    // act
    python.setEnvironment(environmentContext, toolInstallation, false);

    // assert - the project's packages point to a per-project virtual environment (see #352), not to the pristine interpreter
    assertThat(variables.get("VIRTUAL_ENV").getValue().replace('\\', '/')).endsWith("/.venv");
    assertThat(variables.get("UV_PROJECT_ENVIRONMENT").getValue().replace('\\', '/')).endsWith("/.venv");
  }

  /**
   * The {@code uv} dependency required to resolve the Python version must be taken from the {@code dependencies.json} of {@code ide-urls} and not hard-coded.
   * This is verified by removing the {@code uv} dependency from the {@code dependencies.json}: without it the {@code uv} tool is no longer installed, so the
   * Python version cannot be resolved and the installation must fail. If the dependency was still hard-coded (i.e. {@code uv} installed without consulting the
   * {@code dependencies.json}) the {@code uv} tool would be installed and the installation would succeed.
   */
  @Test
  public void testInstallUvDependencyIsReadFromDependenciesJson(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_UV, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.MAC_X64);
    // remove the uv dependency from the python tool: then uv is not installed and the Python version cannot be resolved
    context.getFileAccess().writeFileContent("{ }", context.getUrlsPath().resolve("python").resolve("dependencies.json"));
    Python python = context.getCommandletManager().getCommandlet(Python.class);

    // act & assert - the uv tool is taken from the dependencies.json and thus not installed, so installing python must fail
    assertThatThrownBy(python::install).isInstanceOf(CliException.class);
    assertThat(context.getSoftwarePath().resolve("uv")).doesNotExist();
  }
}

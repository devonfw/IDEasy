package com.devonfw.tools.ide.tool.python;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
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


  /**
   * Test that a missing {@code .ide.software.version} file is restored while the existing installation with all its packages is preserved.
   *
   * @param wireMockRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException on error.
   * @see <a href="https://github.com/devonfw/IDEasy/issues/2190">issue 2190</a>
   */
  @Test
  public void testInstallRestoresMissingVersionFileAndPreservesPackages(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_UV, wireMockRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    Python python = context.getCommandletManager().getCommandlet(Python.class);
    python.install();
    Path pythonPath = context.getSoftwarePath().resolve("python");
    Path versionFile = pythonPath.resolve(IdeContext.FILE_SOFTWARE_VERSION);
    Path userPackage = pythonPath.resolve("lib").resolve("site-packages").resolve("mylib").resolve("__init__.py");
    Files.createDirectories(userPackage.getParent());
    Files.writeString(userPackage, "# installed via pip");
    // simulate that uv or python has removed our version file from the virtual environment
    Files.delete(versionFile);
    // the version is still determined from the installation itself (e.g. for "ide get-version python")
    assertThat(python.getInstalledVersion(pythonPath)).isEqualTo(VersionIdentifier.of("3.14.6"));

    // act
    python.install();

    // assert
    assertThat(versionFile).exists().hasContent("3.14.6");
    assertThat(userPackage).exists();
    assertThat(python.getInstalledVersion()).isEqualTo(VersionIdentifier.of("3.14.6"));
    assertThat(context).logAtWarning().hasMessageContaining("Version file is missing");
    assertThat(context).logAtWarning().hasNoMessageContaining("Deleting corrupted installation");
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

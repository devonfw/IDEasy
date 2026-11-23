package com.devonfw.tools.ide.tool.uv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.SystemPath;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.environment.VariableSource;
import com.devonfw.tools.ide.os.WindowsPathSyntax;
import com.devonfw.tools.ide.process.EnvironmentVariableCollectorContext;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link Uv}.
 */
public class UvTest extends AbstractIdeContextTest {

  @Test
  public void testSetEnvironment() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Uv uv = new Uv(context);
    Path rootDir = context.getSoftwarePath().resolve("uv");
    ToolInstallation toolInstallation = new ToolInstallation(rootDir, rootDir, rootDir, VersionIdentifier.ofRequired("0.1.0"), true);
    Map<String, VariableLine> variables = new HashMap<>();
    EnvironmentVariableCollectorContext environmentContext = new EnvironmentVariableCollectorContext(variables,
        new VariableSource(EnvironmentVariablesType.WORKSPACE, null), WindowsPathSyntax.MSYS);

    // act
    uv.setEnvironment(environmentContext, toolInstallation, false);

    // assert - the uv tool store is per-project (inside IDE_HOME) so that two projects don't collide (see #352). It lives inside the per-project "software"
    // folder so that its launchers are picked up by SystemPath and the uv-based tools are uninstallable (see #2561).
    assertThat(variables.get("UV_TOOL_DIR").getValue().replace('\\', '/')).endsWith("/software/uv-tools");
    assertThat(variables.get("UV_TOOL_BIN_DIR").getValue().replace('\\', '/')).endsWith("/software/uv-tools/bin");
  }

  @Test
  public void testSetEnvironmentMigratesLegacyToolStore() {

    // arrange - seed a legacy uv tool store inside the per-project software folder (the location used before #2561 made the store per-project, see #1518)
    IdeTestContext context = newContext(PROJECT_BASIC);
    Path ideHome = context.getIdeHome();
    Path legacyStore = ideHome.resolve("software").resolve("python").resolve("tools");
    context.getFileAccess().mkdirs(legacyStore);
    Path marker = legacyStore.resolve("ruff");
    context.getFileAccess().mkdirs(marker);
    context.getFileAccess().touch(marker.resolve("tool"));
    Uv uv = new Uv(context);
    Path rootDir = context.getSoftwarePath().resolve("uv");
    ToolInstallation toolInstallation = new ToolInstallation(rootDir, rootDir, rootDir, VersionIdentifier.ofRequired("0.1.0"), true);
    Map<String, VariableLine> variables = new HashMap<>();
    EnvironmentVariableCollectorContext environmentContext = new EnvironmentVariableCollectorContext(variables,
        new VariableSource(EnvironmentVariablesType.WORKSPACE, null), WindowsPathSyntax.MSYS);

    // act
    uv.setEnvironment(environmentContext, toolInstallation, false);

    // assert - the legacy store was moved into the per-project store and is gone from its old location (see #2561)
    Path uvToolDir = ideHome.resolve("software").resolve("uv-tools");
    assertThat(uvToolDir.resolve("ruff").resolve("tool")).exists();
    assertThat(legacyStore).doesNotExist();
    // assert - UV_TOOL_DIR still points at the per-project store
    assertThat(variables.get("UV_TOOL_DIR").getValue().replace('\\', '/')).endsWith("/software/uv-tools");
  }

  @Test
  public void testSetEnvironmentMigratesPreviousUvToolsFolder() {

    // arrange - seed the pre-fix uv tool store dot-folder directly under IDE_HOME (the location #2561 used before moving the store into the software folder)
    IdeTestContext context = newContext(PROJECT_BASIC);
    Path ideHome = context.getIdeHome();
    Path previousStore = ideHome.resolve(".uv-tools");
    context.getFileAccess().mkdirs(previousStore);
    Path marker = previousStore.resolve("ruff");
    context.getFileAccess().mkdirs(marker);
    context.getFileAccess().touch(marker.resolve("tool"));
    Uv uv = new Uv(context);
    Path rootDir = context.getSoftwarePath().resolve("uv");
    ToolInstallation toolInstallation = new ToolInstallation(rootDir, rootDir, rootDir, VersionIdentifier.ofRequired("0.1.0"), true);
    Map<String, VariableLine> variables = new HashMap<>();
    EnvironmentVariableCollectorContext environmentContext = new EnvironmentVariableCollectorContext(variables,
        new VariableSource(EnvironmentVariablesType.WORKSPACE, null), WindowsPathSyntax.MSYS);

    // act
    uv.setEnvironment(environmentContext, toolInstallation, false);

    // assert - the pre-fix store was moved into the per-project store and is gone from its old dot-folder location (see #2561)
    Path uvToolDir = ideHome.resolve("software").resolve("uv-tools");
    assertThat(uvToolDir.resolve("ruff").resolve("tool")).exists();
    assertThat(previousStore).doesNotExist();
    // assert - UV_TOOL_DIR still points at the per-project store
    assertThat(variables.get("UV_TOOL_DIR").getValue().replace('\\', '/')).endsWith("/software/uv-tools");
  }

  /**
   * Tests that a launcher of a uv-based tool (e.g. {@code ruff}) is resolvable via {@link SystemPath}, so that {@code ide uninstall «tool»} finds the installed
   * tool and actually runs {@code uv tool uninstall}. This is the regression the store location has to guard against: the store lives in a plain
   * {@code software/uv-tools/bin} folder (a child of the per-project {@code software} folder, like the per-project global npm prefix) rather than in a
   * dot-folder directly under {@code IDE_HOME} that {@link SystemPath} would never scan (see
   * <a href="https://github.com/devonfw/IDEasy/issues/2561">issue #2561</a>).
   */
  @Test
  public void testUvToolLaunchersAreResolvableViaSystemPath() throws IOException {

    // arrange - a uv tool installed into the per-project store places its launcher in the store's bin folder
    IdeTestContext context = newContext(PROJECT_BASIC);
    Path uvToolBin = Uv.getUvToolStorePath(context).resolve("bin");
    Files.createDirectories(uvToolBin);
    Path ruff = uvToolBin.resolve("ruff");
    Files.writeString(ruff, "fake");
    SystemPath systemPath = new SystemPath(context, "", context.getIdeRoot(), context.getSoftwarePath(), ';', new ArrayList<>());

    // act
    Path resolved = systemPath.findBinary(Path.of("ruff"));

    // assert - the uv tool launcher is resolvable through the generic software PATH scan (mirrors the per-project global npm prefix)
    assertThat(resolved).isEqualTo(ruff);
    assertThat(systemPath.toString()).contains(uvToolBin.toString());
  }

  @Test
  public void testSetEnvironmentWithNullIdeHome() {

    // arrange — force getIdeHome() to return null (global mode, no project)
    IdeTestContext context = new IdeTestContext() {
      @Override
      public Path getIdeHome() {
        return null;
      }
    };
    Uv uv = new Uv(context);
    Path toolDir = Path.of("/software/uv");
    ToolInstallation toolInstallation = new ToolInstallation(toolDir, toolDir, toolDir, VersionIdentifier.ofRequired("0.1.0"), true);
    Map<String, VariableLine> variables = new HashMap<>();
    EnvironmentVariableCollectorContext environmentContext = new EnvironmentVariableCollectorContext(variables,
        new VariableSource(EnvironmentVariablesType.WORKSPACE, null), WindowsPathSyntax.MSYS);

    // act — must not throw a NullPointerException when there is no project
    assertThatCode(() -> uv.setEnvironment(environmentContext, toolInstallation, false)).doesNotThrowAnyException();

    // assert — the per-project uv tool directories are not registered when there is no project
    assertThat(variables).doesNotContainKey("UV_TOOL_DIR");
    assertThat(variables).doesNotContainKey("UV_TOOL_BIN_DIR");
  }

  @Test
  public void testParsePythonListJson() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Uv uv = new Uv(context);
    String json = """
        [
          {"version": "3.14.6", "implementation": "cpython"},
          {"version": "7.3.17", "implementation": "pypy"},
          {"implementation": "cpython"}
        ]
        """;

    // act
    var entries = uv.parsePythonListJson(java.util.List.of(json));

    // assert
    assertThat(entries).hasSize(2);
    assertThat(entries.get(0).version()).isEqualTo("3.14.6");
    assertThat(entries.get(0).isCpython()).isTrue();
    assertThat(entries.get(1).version()).isEqualTo("7.3.17");
    assertThat(entries.get(1).isCpython()).isFalse();
  }

  @Test
  public void testParsePythonListJsonEmpty() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Uv uv = new Uv(context);

    // act & assert
    assertThat(uv.parsePythonListJson(java.util.List.of())).isEmpty();
    assertThat(uv.parsePythonListJson(java.util.List.of("[]"))).isEmpty();
  }
}

package com.devonfw.tools.ide.tool.uv;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

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
    ToolInstallation toolInstallation = new ToolInstallation(rootDir, rootDir, rootDir, VersionIdentifier.of("0.1.0"), true);
    Map<String, VariableLine> variables = new HashMap<>();
    EnvironmentVariableCollectorContext environmentContext = new EnvironmentVariableCollectorContext(variables,
        new VariableSource(EnvironmentVariablesType.WORKSPACE, null), WindowsPathSyntax.MSYS);

    // act
    uv.setEnvironment(environmentContext, toolInstallation, false);

    // assert
    assertThat(variables.get("UV_TOOL_DIR").getValue().replace('\\', '/')).endsWith("software/python/tools");
    assertThat(variables.get("UV_TOOL_BIN_DIR").getValue().replace('\\', '/')).endsWith("software/python/bin");
  }

  @Test
  public void testSetEnvironmentWithNullSoftwarePath() {

    // arrange — force getSoftwarePath() to return null to reproduce the condition of #2312
    IdeTestContext context = new IdeTestContext() {
      @Override
      public Path getSoftwarePath() {
        return null;
      }
    };
    Uv uv = new Uv(context);
    Path toolDir = Path.of("/software/uv");
    ToolInstallation toolInstallation = new ToolInstallation(toolDir, toolDir, toolDir, VersionIdentifier.of("0.1.0"), true);
    Map<String, VariableLine> variables = new HashMap<>();
    EnvironmentVariableCollectorContext environmentContext = new EnvironmentVariableCollectorContext(variables,
        new VariableSource(EnvironmentVariablesType.WORKSPACE, null), WindowsPathSyntax.MSYS);

    // act — must not throw a NullPointerException when the software path is null
    assertThatCode(() -> uv.setEnvironment(environmentContext, toolInstallation, false)).doesNotThrowAnyException();

    // assert — the uv tool directories are not registered when the software path is null
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

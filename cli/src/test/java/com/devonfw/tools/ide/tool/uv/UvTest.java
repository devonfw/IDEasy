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
    EnvironmentVariableCollectorContext environmentContext = new EnvironmentVariableCollectorContext(variables, new VariableSource(EnvironmentVariablesType.WORKSPACE, null), WindowsPathSyntax.MSYS);

    // act
    uv.setEnvironment(environmentContext, toolInstallation, false);

    // assert
    Path expectedPythonBinPath = context.getSoftwarePath().resolve("python").resolve("bin");
    assertThat(variables.get("XDG_BIN_HOME").getValue()).isEqualTo(expectedPythonBinPath.toString());
  }
}

package com.devonfw.tools.ide.tool.dotnet;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.environment.VariableSource;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.os.WindowsPathSyntax;
import com.devonfw.tools.ide.process.EnvironmentVariableCollectorContext;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link DotNet}.
 */
class DotNetTest extends AbstractIdeContextTest {

  private static final Path PROJECTS_TARGET_PATH = Path.of("target/test-projects");

  private static final String PROJECT_DOTNET = "dotnet";

  private final IdeTestContext context = newContext(PROJECT_DOTNET);

  private final DotNet commandlet = new DotNet(this.context);

  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  void dotnetShouldInstallSuccessful(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    assignDummyUserHome(this.context, "dummyUserHome");

    // act
    this.commandlet.install();

    // assert
    assertThat(this.context.getSoftwarePath().resolve("dotnet")).exists();

    if (this.context.getSystemInfo().isWindows()) {
      assertThat(this.context.getSoftwarePath().resolve("dotnet/dotnet.exe")).exists();
    } else {
      assertThat(this.context.getSoftwarePath().resolve("dotnet/dotnet")).exists();
    }

    assertThat(this.context.getSoftwarePath().resolve("dotnet/.ide.software.version")).exists();
    assertThat(this.context.getSoftwarePath().resolve("dotnet/.ide.software.version")).hasContent("6.0.419");

    assertThat(this.context).logAtSuccess().hasMessageContaining("Successfully installed dotnet in version 6.0.419");
  }

  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  void dotnetShouldRunExecutableSuccessful(String os) {

    // TODO: Check: https://github.com/devonfw/IDEasy/issues/701 for reference.
    if (SystemInfoImpl.INSTANCE.isWindows()) {
      String expectedOutputLinux = "Dummy dotnet 6.0.419 on linux ";
      String expectedOutputMacOs = "Dummy dotnet 6.0.419 on mac ";
      String expectedOutputWindows = "Dummy dotnet 6.0.419 on windows ";
      runExecutable(os);

      if (this.context.getSystemInfo().isLinux()) {
        checkExpectedOutput(expectedOutputLinux);
      } else if (this.context.getSystemInfo().isMac()) {
        checkExpectedOutput(expectedOutputMacOs);
      } else if (this.context.getSystemInfo().isWindows()) {
        checkExpectedOutput(expectedOutputWindows);
      }
    }
  }

  private void checkExpectedOutput(String expectedOutput) {

    assertThat(this.context).logAtInfo().hasMessage(expectedOutput);
  }

  private void runExecutable(String operatingSystem) {

    SystemInfo systemInfo = SystemInfoMock.of(operatingSystem);
    this.context.setSystemInfo(systemInfo);
    this.commandlet.run();
  }

  private static void assignDummyUserHome(IdeTestContext context, String pathString) {

    Path dummyUserHomePath = PROJECTS_TARGET_PATH.resolve(PROJECT_DOTNET).resolve(pathString);
    context.setUserHome(dummyUserHomePath);
  }

  @Test
  void testSetEnvironment() {

    // arrange
    Path dotnetPath = this.context.getSoftwarePath().resolve("dotnet");
    ToolInstallation installation = new ToolInstallation(
        dotnetPath,
        dotnetPath,
        dotnetPath,
        VersionIdentifier.of("6.0.419"),
        true);

    Map<String, VariableLine> variables = new HashMap<>();
    EnvironmentVariableCollectorContext environmentContext =
        new EnvironmentVariableCollectorContext(
            variables,
            new VariableSource(EnvironmentVariablesType.WORKSPACE, null),
            WindowsPathSyntax.MSYS);

    this.commandlet.setEnvironment(environmentContext, installation, false);

    assertThat(variables.get("DOTNET_HOME").getValue())
        .isEqualTo(dotnetPath.toString());
    assertThat(variables.get("DOTNET_ROOT").getValue())
        .isEqualTo(dotnetPath.toString());
  }
}

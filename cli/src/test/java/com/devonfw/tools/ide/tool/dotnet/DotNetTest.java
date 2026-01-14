package com.devonfw.tools.ide.tool.dotnet;

import java.nio.file.Path;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.os.SystemInfoMock;

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

    assertThat(this.context).logAtSuccess().hasMessage("Successfully installed dotnet in version 6.0.419");
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
    this.context.info("Running dotnet binary from: {}", this.commandlet.getToolBinPath());
    this.commandlet.run();
  }

  private static void assignDummyUserHome(IdeTestContext context, String pathString) {

    Path dummyUserHomePath = PROJECTS_TARGET_PATH.resolve(PROJECT_DOTNET).resolve(pathString);
    context.setUserHome(dummyUserHomePath);
  }
}

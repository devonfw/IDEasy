package com.devonfw.tools.ide.tool.dotnet;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.os.SystemInfoMock;

public class DotNetTest extends AbstractIdeContextTest {

  private static final Path PROJECTS_TARGET_PATH = Path.of("target/test-projects");

  private static final String PROJECT_DOTNET = "dotnet";

  private final IdeTestContext context = newContext(PROJECT_DOTNET);

  private final DotNet commandlet = new DotNet(this.context);

  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void dotnetShouldInstallSuccessful(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    assignDummyUserHome(this.context, "dummyUserHome");

    // act
    this.commandlet.install();

    // assert
    assertThat(this.context.getSoftwarePath().resolve("dotnet")).exists();

    if (this.context.getSystemInfo().isWindows()) {
      assertThat(this.context.getSoftwarePath().resolve("dotnet/dotnet.cmd")).exists();
    }

    if (this.context.getSystemInfo().isLinux() || this.context.getSystemInfo().isMac()) {
      assertThat(this.context.getSoftwarePath().resolve("dotnet/dotnet")).exists();
    }

    assertThat(this.context.getSoftwarePath().resolve("dotnet/.ide.software.version")).exists();
    assertThat(this.context.getSoftwarePath().resolve("dotnet/.ide.software.version")).hasContent("6.0.419");

    assertLogMessage(this.context, IdeLogLevel.SUCCESS, "Successfully installed dotnet in version 6.0.419", false);
  }

  @Test
  public void dotnetShouldRunExecutableForWindowsSuccessful() {

    String expectedOutputWindows = "Dummy dotnet 6.0.419 on windows ";
    if (SystemInfoImpl.INSTANCE.isWindows()) {
      runExecutable("windows");
      checkExpectedOutput(expectedOutputWindows);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "mac", "linux" })
  public void dotnetShouldRunExecutableSuccessful(String os) {

    String expectedOutputLinux = "Dummy dotnet 6.0.419 on linux ";
    String expectedOutputMacOs = "Dummy dotnet 6.0.419 on mac ";
    runExecutable(os);

    if (this.context.getSystemInfo().isLinux()) {
      checkExpectedOutput(expectedOutputLinux);
    } else if (this.context.getSystemInfo().isMac()) {
      checkExpectedOutput(expectedOutputMacOs);
    }
  }

  private void checkExpectedOutput(String expectedOutput) {

    assertLogMessage(this.context, IdeLogLevel.INFO, expectedOutput);
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
}

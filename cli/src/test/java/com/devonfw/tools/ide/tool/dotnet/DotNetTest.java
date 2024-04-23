package com.devonfw.tools.ide.tool.dotnet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

public class DotNetTest extends AbstractIdeContextTest {

  private static final Path PROJECTS_TARGET_PATH = Path.of("target/test-projects");

  private static final String PROJECT_DOTNET = "dotnet";

  private final IdeTestContext context = newContext(PROJECT_DOTNET);

  private final DotNet commandlet = new DotNet(context);

  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void dotnetShouldInstallSuccessful(String os) {

    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    assignDummyUserHome(context, "dummyUserHome");

    // act
    commandlet.install();

    // assert
    assertThat(context.getSoftwarePath().resolve("dotnet")).exists();

    if (context.getSystemInfo().isWindows()) {
      assertThat(context.getSoftwarePath().resolve("dotnet/dotnet.cmd")).exists();
    }

    if (context.getSystemInfo().isLinux() || context.getSystemInfo().isMac()) {
      assertThat(context.getSoftwarePath().resolve("dotnet/dotnet")).exists();
    }

    assertThat(context.getSoftwarePath().resolve("dotnet/.ide.software.version")).exists();
    assertThat(context.getSoftwarePath().resolve("dotnet/.ide.software.version")).hasContent("6.0.419");

    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed dotnet in version 6.0.419", false);
  }

  private static void assignDummyUserHome(IdeTestContext context, String pathString) {

    Path dummyUserHomePath = PROJECTS_TARGET_PATH.resolve(PROJECT_DOTNET).resolve(pathString);
    context.setUserHome(dummyUserHomePath);
  }
}

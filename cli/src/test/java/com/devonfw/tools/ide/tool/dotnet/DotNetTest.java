package com.devonfw.tools.ide.tool.dotnet;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;

public class DotNetTest extends AbstractIdeContextTest {

  private static final String PROJECT_DOTNET = "dotnet";

  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void dotnetShouldInstallSuccessful(String os) {

    // arrange
    IdeTestContext context = newContext(PROJECT_DOTNET);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    DotNet commandlet = new DotNet(context);

    // act
    commandlet.install();

    // assert

    /*
    assertThat(context.getSoftwarePath().resolve("dotnet")).exists();
    assertThat(context.getSoftwarePath().resolve("dotnet/InstallTest.txt")).hasContent("This is a test file.");

    if (context.getSystemInfo().isWindows()) {
      assertThat(context.getSoftwarePath().resolve("dotnet/dotnet.cmd")).exists();
    }

    if (context.getSystemInfo().isLinux() || context.getSystemInfo().isMac()) {
      assertThat(context.getSoftwarePath().resolve("dotnet/dotnet")).exists();
    }

    assertThat(context.getSoftwarePath().resolve("dotnet/.ide.software.version")).exists();
    assertThat(context.getSoftwarePath().resolve("dotnet/.ide.software.version")).hasContent("6.0.419");

    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed dotnet in version 6.0.419", false);
    assertLogMessage(context, IdeLogLevel.DEBUG, "Devon4net template already installed.", false);


     */

  }
}

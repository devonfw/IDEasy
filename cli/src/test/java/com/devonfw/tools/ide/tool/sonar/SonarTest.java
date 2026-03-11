package com.devonfw.tools.ide.tool.sonar;

import java.nio.file.Path;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Sonar}.
 */
@WireMockTest
class SonarTest extends AbstractIdeContextTest {

  private static final String PROJECT_SONAR = "sonar";
  private static final String SONAR_VERSION = "26.3.0.120487";

  /**
   * Tests the installation of {@link Sonar}.
   *
   * @param os String of the OS to use.
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  void testSonarInstall(String os, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_SONAR, wmRuntimeInfo);
    context.setSystemInfo(SystemInfoMock.of(os));
    Sonar sonar = context.getCommandletManager().getCommandlet(Sonar.class);
    sonar.command.setValueAsString("start", context);

    // act
    sonar.install(true);

    // assert
    checkInstallation(context);
  }

  /**
   * Tests parsing of command values for {@link Sonar#command}.
   */
  @ParameterizedTest
  @CsvSource({ "start, START", "stop, STOP", "analyze, ANALYZE" })
  void testSonarCommandParsing(String command, SonarCommand expectedCommand) {

    // arrange
    IdeTestContext context = new IdeTestContext();
    Sonar sonar = new Sonar(context);

    // act
    sonar.command.setValueAsString(command, context);

    // assert
    assertThat(sonar.command.getValue()).isEqualTo(expectedCommand);
  }

  /**
   * Tests that {@link Sonar#getBinaryName} returns correct values for different commands.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  void testSonarGetBinaryName(String os) {

    // arrange
    IdeTestContext context = new IdeTestContext();
    context.setSystemInfo(SystemInfoMock.of(os));
    Sonar sonar = new Sonar(context);

    // act & assert - START command
    sonar.command.setValueAsString("start", context);
    String startBinary = sonar.getBinaryName();
    if (context.getSystemInfo().isWindows()) {
      assertThat(startBinary).isEqualTo("windows-x86-64/StartSonar.bat");
    } else if (context.getSystemInfo().isMac()) {
      assertThat(startBinary).isEqualTo("macosx-universal-64/sonar.sh");
    } else {
      assertThat(startBinary).isEqualTo("linux-x86-64/sonar.sh");
    }

    // act & assert - STOP command
    sonar.command.setValueAsString("stop", context);
    String stopBinary = sonar.getBinaryName();
    if (context.getSystemInfo().isWindows()) {
      assertThat(stopBinary).isEqualTo("windows-x86-64/SonarService.bat");
    } else if (context.getSystemInfo().isMac()) {
      assertThat(stopBinary).isEqualTo("macosx-universal-64/sonar.sh");
    } else {
      assertThat(stopBinary).isEqualTo("linux-x86-64/sonar.sh");
    }
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("sonar/.ide.software.version")).exists().hasContent(SONAR_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed sonar in version " + SONAR_VERSION);

    // Verify the binary exists based on OS
    Path softwarePath = context.getSoftwarePath();
    if (context.getSystemInfo().isWindows()) {
      assertThat(softwarePath.resolve("sonar/bin/windows-x86-64/StartSonar.bat")).exists();
      assertThat(softwarePath.resolve("sonar/bin/windows-x86-64/SonarService.bat")).exists();
    } else if (context.getSystemInfo().isMac()) {
      assertThat(softwarePath.resolve("sonar/bin/macosx-universal-64/sonar.sh")).exists();
    } else {
      assertThat(softwarePath.resolve("sonar/bin/linux-x86-64/sonar.sh")).exists();
    }
  }

}

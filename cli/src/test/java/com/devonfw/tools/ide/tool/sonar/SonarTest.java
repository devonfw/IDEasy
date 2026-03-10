package com.devonfw.tools.ide.tool.sonar;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
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
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testSonarInstall(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_SONAR, wmRuntimeInfo);
    Sonar sonar = context.getCommandletManager().getCommandlet(Sonar.class);
    sonar.command.setValueAsString("start", context);

    // act
    sonar.install(true);

    // assert
    checkInstallation(context);
  }

  /**
   * Tests the {@link Sonar} commandlet initialization with START command.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testSonarStartCommand(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_SONAR, wmRuntimeInfo);
    Sonar sonar = context.getCommandletManager().getCommandlet(Sonar.class);
    sonar.command.setValueAsString("start", context);

    // assert
    assertThat(sonar).isNotNull();
    assertThat(sonar.command.getValue()).isEqualTo(SonarCommand.START);
  }

  /**
   * Tests the {@link Sonar} commandlet initialization with STOP command.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testSonarStopCommand(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_SONAR, wmRuntimeInfo);
    Sonar sonar = context.getCommandletManager().getCommandlet(Sonar.class);
    sonar.command.setValueAsString("stop", context);

    // assert
    assertThat(sonar).isNotNull();
    assertThat(sonar.command.getValue()).isEqualTo(SonarCommand.STOP);
  }

  /**
   * Tests the {@link Sonar} commandlet initialization with ANALYZE command.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testSonarAnalyzeCommand(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_SONAR, wmRuntimeInfo);
    Sonar sonar = context.getCommandletManager().getCommandlet(Sonar.class);
    sonar.command.setValueAsString("analyze", context);

    // assert
    assertThat(sonar).isNotNull();
    assertThat(sonar.command.getValue()).isEqualTo(SonarCommand.ANALYZE);
  }


  /**
   * Tests the repository setup for {@link Sonar}. Verifies that the test repository files are properly structured.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testSonarRepositorySetup(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_SONAR, wmRuntimeInfo);
    Sonar sonar = context.getCommandletManager().getCommandlet(Sonar.class);
    sonar.command.setValueAsString("start", context);

    // assert - verify repository structure exists in the copied fixture
    Path repositoryPath = TEST_PROJECTS_COPY.resolve(PROJECT_SONAR).resolve("repository/sonar/sonar/default");
    assertThat(repositoryPath.resolve("bin")).exists();
    assertThat(repositoryPath.resolve(".ide.software.version")).exists().hasContent(SONAR_VERSION);
    assertThat(repositoryPath.resolve("conf/sonar.properties")).exists();

    // Verify the binary exists based on OS
    if (context.getSystemInfo().isWindows()) {
      assertThat(repositoryPath.resolve("bin/windows-x86-64/StartSonar.bat")).exists();
      assertThat(repositoryPath.resolve("bin/windows-x86-64/SonarService.bat")).exists();
    } else if (context.getSystemInfo().isMac()) {
      assertThat(repositoryPath.resolve("bin/macosx-universal-64/sonar.sh")).exists();
    } else {
      assertThat(repositoryPath.resolve("bin/linux-x86-64/sonar.sh")).exists();
    }
  }

  /**
   * Tests that {@link Sonar#getBinaryName} returns correct values for different commands. This is a regression test for issue #1731 to ensure no NPE occurs
   * when command is accessed.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testSonarGetBinaryName(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_SONAR, wmRuntimeInfo);
    Sonar sonar = context.getCommandletManager().getCommandlet(Sonar.class);

    // act & assert - START command
    sonar.command.setValueAsString("start", context);
    String startBinary = sonar.getBinaryName();
    assertThat(startBinary).isNotNull();
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
    assertThat(stopBinary).isNotNull();
    if (context.getSystemInfo().isWindows()) {
      assertThat(stopBinary).isEqualTo("windows-x86-64/SonarService.bat");
    } else if (context.getSystemInfo().isMac()) {
      assertThat(stopBinary).isEqualTo("macosx-universal-64/sonar.sh");
    } else {
      assertThat(stopBinary).isEqualTo("linux-x86-64/sonar.sh");
    }
  }

  /**
   * Tests that {@link Sonar#getToolBinPath} returns an absolute path. This verifies the fix for path resolution issues where relative paths failed.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testSonarToolBinPathIsAbsolute(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_SONAR, wmRuntimeInfo);
    Sonar sonar = context.getCommandletManager().getCommandlet(Sonar.class);
    sonar.command.setValueAsString("start", context);

    // act - install sonar first so getToolBinPath() has a valid path
    sonar.install(true);
    Path toolBinPath = sonar.getToolBinPath();

    // assert - verify the binary path would be absolute
    assertThat(toolBinPath).isNotNull();
    assertThat(toolBinPath).isAbsolute();

    String binaryName = sonar.getBinaryName();
    Path expectedBinaryPath = toolBinPath.resolve(binaryName);
    assertThat(expectedBinaryPath).isAbsolute();
  }


  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();

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

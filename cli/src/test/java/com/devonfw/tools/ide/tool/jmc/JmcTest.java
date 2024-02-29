package com.devonfw.tools.ide.tool.jmc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.CommandLetExtractorMock;
import com.devonfw.tools.ide.commandlet.InstallCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.repo.ToolRepositoryMock;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Integration test of {@link com.devonfw.tools.ide.tool.jmc.Jmc}.
 */
public class JmcTest extends AbstractIdeContextTest {

  private static WireMockServer server;

  private static final Path RESOURCE_PATH = Path.of("src/test/resources");

  @BeforeAll
  static void setUp() {

    // TODO use random port number and create url file dynamically in project
    // TODO ISSUE:https://github.com/devonfw/IDEasy/issues/223
    server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(1112));

    server.start();
  }

  @AfterAll
  static void tearDown() {

    server.shutdownServer();
  }

  private void mockWebServer() throws IOException {

    // Jmc under test
    String windowsFilenameJmc = "org.openjdk.jmc-8.3.0-win32.win32.x86_64.zip";
    Path windowsFilePathJmc = RESOURCE_PATH.resolve("__files").resolve(windowsFilenameJmc);
    String windowsLengthJmc = String.valueOf(Files.size(windowsFilePathJmc));

    String linuxFilenameJmc = "org.openjdk.jmc-8.3.0-linux.gtk.x86_64.tar.gz";
    Path linuxFilePathJmc = RESOURCE_PATH.resolve("__files").resolve(linuxFilenameJmc);
    String linuxLengthJmc = String.valueOf(Files.size(linuxFilePathJmc));

    String macOSFilenameJmc = "org.openjdk.jmc-8.3.0-macosx.cocoa.x86_64.tar.gz";
    Path macOSFilePathJmc = RESOURCE_PATH.resolve("__files").resolve(macOSFilenameJmc);
    String maxOSLengthJmc = String.valueOf(Files.size(macOSFilePathJmc));

    setupMockServerResponse("/jmcTest/windows", "application/zip", windowsLengthJmc, windowsFilenameJmc);
    setupMockServerResponse("/jmcTest/linux", "application/gz", linuxLengthJmc, linuxFilenameJmc);
    setupMockServerResponse("/jmcTest/macOS", "application/gz", maxOSLengthJmc, macOSFilenameJmc);

    // Java prerequisite

    String windowsFilenameJava = "java-17.0.6-windows-x64.zip";
    String linuxFilenameJava = "java-17.0.6-linux-x64.tgz";

    Path windowsFilePathJava = RESOURCE_PATH.resolve("__files").resolve(windowsFilenameJava);
    String windowsLengthJava = String.valueOf(Files.size(windowsFilePathJava));

    Path linuxFilePathJava = RESOURCE_PATH.resolve("__files").resolve(linuxFilenameJava);
    String linuxLengthJava = String.valueOf(Files.size(linuxFilePathJava));

    setupMockServerResponse("/installTest/windows", "application/zip", windowsLengthJava, windowsFilenameJava);
    setupMockServerResponse("/installTest/linux", "application/tgz", linuxLengthJava, linuxFilenameJava);
    setupMockServerResponse("/installTest/macOS", "application/tgz", linuxLengthJava, linuxFilenameJava);

  }

  private void setupMockServerResponse(String testUrl, String contentType, String contentLength, String bodyFile) {

    server.stubFor(get(urlPathEqualTo(testUrl)).willReturn(aResponse().withHeader("Content-Type", contentType)
        .withHeader("Content-Length", contentLength).withStatus(200).withBodyFile(bodyFile)));
  }

  @Test
  public void jmcPostInstallShouldMoveFilesIfRequiredMockedServer() throws IOException {

    // arrange
    String path = "workspaces/foo-test/my-git-repo";
    IdeContext context = newContext("jmc", path, true);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("jmc", context);
    mockWebServer();
    // act
    install.run();

    // assert
    performPostInstallAssertion(context);
  }

  @Test
  public void jmcPostInstallShouldMoveFilesIfRequired() {

    // arrange
    String path = "workspaces/foo-test/my-git-repo";
    String projectTestCaseName = "jmc";

    ToolRepositoryMock toolRepositoryMock = buildToolRepositoryMockForJmc(projectTestCaseName);

    IdeContext context = newContext(projectTestCaseName, path, true, toolRepositoryMock);
    toolRepositoryMock.setContext(context);

    CommandLetExtractorMock commandLetExtractorMock = new CommandLetExtractorMock(context);
    Jmc commandlet = new Jmc(context);
    commandlet.setCommandletFileExtractor(commandLetExtractorMock);

    // act
    commandlet.install();

    // assert
    performPostInstallAssertion(context);
  }

  @Test
  public void jmcShouldRunExecutableSuccessful() {

    // arrange
    String path = "workspaces/foo-test/my-git-repo";
    String projectTestCaseName = "jmc";
    String expectedOutputWindows = "Dummy jmc 8.3.0 on windows";
    String expectedOutputLinux = "Dummy jmc 8.3.0 on linux";
    String expectedOutputMacOs = "Dummy jmc 8.3.0 on macOs";
    Path mockResultPath = Path.of("target/test-projects/jmc/project");

    ToolRepositoryMock toolRepositoryMock = buildToolRepositoryMockForJmc(projectTestCaseName);

    AbstractIdeContext context = newContext(projectTestCaseName, path, true, toolRepositoryMock);
    toolRepositoryMock.setContext(context);
    context.setDefaultExecutionDirectory(mockResultPath);

    CommandLetExtractorMock commandLetExtractorMock = new CommandLetExtractorMock(context);
    Jmc commandlet = new Jmc(context);
    commandlet.setCommandletFileExtractor(commandLetExtractorMock);

    commandlet.install();

    // act
    commandlet.run();

    // assert

    String expectedOutput = determineExpectedOutput(context, expectedOutputWindows, expectedOutputLinux,
        expectedOutputMacOs);

    assertThat(mockResultPath.resolve("jmcTestRestult.txt")).exists();
    assertThat(mockResultPath.resolve("jmcTestRestult.txt")).hasContent(expectedOutput);

  }

  private static ToolRepositoryMock buildToolRepositoryMockForJmc(String projectTestCaseName) {

    String windowsFileFolder = "org.openjdk.jmc-8.3.0-win32.win32.x86_64";
    String linuxFileFolder = "org.openjdk.jmc-8.3.0-linux.gtk.x86_64";
    String macFileFolder = "org.openjdk.jmc-8.3.0-macosx.cocoa.x86_64";

    ToolRepositoryMock toolRepositoryMock = new ToolRepositoryMock("jmc", "8.3.0", projectTestCaseName,
        windowsFileFolder, linuxFileFolder, macFileFolder);

    toolRepositoryMock.addAlreadyInstalledTool("java", "17.0.10_7");

    return toolRepositoryMock;
  }

  private void performPostInstallAssertion(IdeContext context) {

    String expectedMessage = "Successfully installed jmc in version 8.3.0";

    assertThat(context.getSoftwarePath().resolve("jmc")).exists();
    assertThat(context.getSoftwarePath().resolve("jmc/InstallTest.txt")).hasContent("This is a test file.");

    if (context.getSystemInfo().isWindows()) {
      assertThat(context.getSoftwarePath().resolve("jmc/jmc.cmd")).exists();
    }

    if (context.getSystemInfo().isLinux()) {
      assertThat(context.getSoftwarePath().resolve("jmc/jmc")).exists();
    }

    if (context.getSystemInfo().isWindows() || context.getSystemInfo().isLinux()) {
      assertThat(context.getSoftwarePath().resolve("jmc/HelloWorld.txt")).hasContent("Hello World!");
      assertThat(context.getSoftwarePath().resolve("jmc/JDK Mission Control")).doesNotExist();
    }

    if (context.getSystemInfo().isMac()) {
      assertThat(context.getSoftwarePath().resolve("jmc/JDK Mission Control.app")).exists();
      assertThat(context.getSoftwarePath().resolve("jmc/JDK Mission Control.app/Contents")).exists();
    }

    assertThat(context.getSoftwarePath().resolve("jmc/.ide.software.version")).exists();
    assertThat(context.getSoftwarePath().resolve("jmc/.ide.software.version")).hasContent("8.3.0");

    assertLogMessage((IdeTestContext) context, IdeLogLevel.SUCCESS, expectedMessage, false);

  }

}

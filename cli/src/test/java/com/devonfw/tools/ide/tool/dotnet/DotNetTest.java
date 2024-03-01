package com.devonfw.tools.ide.tool.dotnet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.devonfw.tools.ide.commandlet.CommandLetExtractorMock;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.property.StringListProperty;
import com.devonfw.tools.ide.repo.ToolRepositoryMock;

public class DotNetTest extends AbstractIdeContextTest {

  private static final Path PROJECTS_TARGET_PATH = Path.of("target/test-projects");

  private static final Path MOCK_RESULT_PATH = Path.of("target/test-projects/dotnet/project");

  private static final String GIT_REPO = "workspaces/foo-test/my-git-repo";

  private static final String PROJECT_TEST_CASE_NAME = "dotnet";

  @Test
  public void dotnetShouldInstallSuccessful() {

    // arrange

    ToolRepositoryMock toolRepositoryMock = buildToolRepositoryMockForDotNet();
    IdeTestContext context = newContext(PROJECT_TEST_CASE_NAME, GIT_REPO, true, toolRepositoryMock);
    toolRepositoryMock.setContext(context);
    CommandLetExtractorMock commandLetExtractorMock = new CommandLetExtractorMock(context);

    DotNet comandlet = new DotNet(context);
    comandlet.setCommandletFileExtractor(commandLetExtractorMock);
    assignFakeUserHome(context, "dummyUserHome");

    // act
    comandlet.install();

    // assert

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

  }

  @Test
  public void dotnetShouldInstallSuccessfulAndTryInstallTemplate() {

    // arrange

    final String[] expectedWinArgs = { "new", "install", "devon4net.WebApi.Template" };
    final String[] expectedUnixArgs = { "new", "--install", "devon4net.WebApi.Template" };

    ToolRepositoryMock toolRepositoryMock = buildToolRepositoryMockForDotNet();
    IdeTestContext context = newContext(PROJECT_TEST_CASE_NAME, GIT_REPO, true, toolRepositoryMock);
    toolRepositoryMock.setContext(context);
    CommandLetExtractorMock commandLetExtractorMock = new CommandLetExtractorMock(context);

    DotNet comandlet = new DotNet(context);
    comandlet.setCommandletFileExtractor(commandLetExtractorMock);

    assignFakeUserHome(context, "dummyEmptyUserHome");

    // installation may install template, so we set up a mocked processContext

    ProcessContext mockProcessContext = mock(ProcessContext.class);
    context.setMockProcessContext(mockProcessContext);

    when(mockProcessContext.executable(Mockito.anyString())).thenReturn(mockProcessContext);
    when(mockProcessContext.addArgs(Mockito.any(String[].class))).thenReturn(mockProcessContext);

    when(mockProcessContext.executable(Mockito.any(Path.class))).thenReturn(mockProcessContext);
    when(mockProcessContext.errorHandling(Mockito.any())).thenReturn(mockProcessContext);

    // act
    comandlet.install();

    // assert passed arguments of process context
    ArgumentCaptor<String[]> processContextCapture = ArgumentCaptor.forClass(String[].class);
    int numberInvocations = context.getSystemInfo().isWindows() ? 2 : 1;
    verify(mockProcessContext, times(numberInvocations)).addArgs(processContextCapture.capture());
    String[] args = processContextCapture.getValue();

    if (context.getSystemInfo().isWindows()) {
      assertThat(expectedWinArgs).isEqualTo(args);
    } else {
      assertThat(expectedUnixArgs).isEqualTo(args);
    }

  }

  @Test
  public void dotnetShouldRunExecutableSuccessful() {

    // arrange
    String expectedOutputWindows = "Dummy dotnet 6.0.419 on windows ";
    String expectedOutputLinux = "Dummy dotnet 6.0.419 on linux ";
    String expectedOutputMacOs = "Dummy dotnet 6.0.419 on mac ";

    ToolRepositoryMock toolRepositoryMock = buildToolRepositoryMockForDotNet();
    IdeTestContext context = newContext(PROJECT_TEST_CASE_NAME, GIT_REPO, true, toolRepositoryMock);
    toolRepositoryMock.setContext(context);
    CommandLetExtractorMock commandLetExtractorMock = new CommandLetExtractorMock(context);

    assignFakeUserHome(context, "dummyUserHome");

    DotNet comandlet = new DotNet(context);
    comandlet.setCommandletFileExtractor(commandLetExtractorMock);

    comandlet.install();

    // act
    comandlet.run();

    String expectedOutput = determineExpectedOutput(context, expectedOutputWindows, expectedOutputLinux,
        expectedOutputMacOs);

    assertThat(MOCK_RESULT_PATH.resolve("dotnetTestRestult.txt")).exists();
    assertThat(MOCK_RESULT_PATH.resolve("dotnetTestRestult.txt")).hasContent(expectedOutput);
    assertThat(context.getIdeHome()).isEqualTo(context.getDefaultExecutionDirectory());

  }

  @Test
  public void argumentsShouldBeModifiedInCreateCase() throws IllegalAccessException {

    // arrange

    String expectedModifiedArgsAsString = "new Devon4NetAPI";

    ToolRepositoryMock toolRepositoryMock = buildToolRepositoryMockForDotNet();
    IdeTestContext context = newContext(PROJECT_TEST_CASE_NAME, GIT_REPO, true, toolRepositoryMock);
    toolRepositoryMock.setContext(context);
    CommandLetExtractorMock commandLetExtractorMock = new CommandLetExtractorMock(context);

    assignFakeUserHome(context, "dummyUserHome");

    DotNet comandlet = new DotNet(context);
    comandlet.setCommandletFileExtractor(commandLetExtractorMock);

    modifyArgumentsOfCommandlet(comandlet);

    comandlet.install();

    comandlet.run();

    // assert
    assertThat(MOCK_RESULT_PATH.resolve("dotnetTestRestult.txt")).exists();
    assertThat(MOCK_RESULT_PATH.resolve("dotnetTestRestult.txt")).content().contains(expectedModifiedArgsAsString);

  }

  private static ToolRepositoryMock buildToolRepositoryMockForDotNet() {

    String windowsFileFolder = "dotnet-6.0.419-windows-x64";
    String linuxFileFolder = "dotnet-6.0.419-linux-x64";
    String macFileFolder = "dotnet-6.0.419-mac-x64";

    return new ToolRepositoryMock("dotnet", "6.0.419", DotNetTest.PROJECT_TEST_CASE_NAME, windowsFileFolder,
        linuxFileFolder, macFileFolder);
  }

  private static void modifyArgumentsOfCommandlet(DotNet commandlet) throws IllegalAccessException {

    ArrayList<String> argsValues = new ArrayList<>();
    argsValues.add("create");
    StringListProperty fakeProperties = new StringListProperty("", false, "args");
    Field field = ReflectionUtils
        .findFields(DotNet.class, f -> f.getName().equals("arguments"), ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
        .get(0);
    fakeProperties.setValue(argsValues);

    field.setAccessible(true);
    field.set(commandlet, fakeProperties);
    field.setAccessible(false);
  }

  private static void assignFakeUserHome(IdeTestContext context, String pathString) {

    Path dummyUserHomePath = PROJECTS_TARGET_PATH.resolve(PROJECT_TEST_CASE_NAME).resolve(pathString);
    context.setDummyUserHome(dummyUserHomePath);
  }

}
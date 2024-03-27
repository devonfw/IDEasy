package com.devonfw.tools.ide.tool.dotnet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.property.StringListProperty;

public class DotNetTest extends AbstractIdeContextTest {

  private static final Path PROJECTS_TARGET_PATH = Path.of("target/test-projects");

  private static final Path MOCK_RESULT_PATH = Path.of("target/test-projects/dotnet/project");

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
    assertLogMessage(context, IdeLogLevel.DEBUG, "Devon4net template already installed.", false);
  }

  @Test
  public void dotnetShouldInstallSuccessfulAndTryInstallTemplate() {

    // arrange
    final String[] expectedWinArgs = { "new", "install", "devon4net.WebApi.Template" };
    final String[] expectedUnixArgs = { "new", "--install", "devon4net.WebApi.Template" };

    assignDummyUserHome(context, "dummyEmptyUserHome");

    // installation may install template, so we set up a mocked processContext
    ProcessContext mockProcessContext = mock(ProcessContext.class);
    context.setMockProcessContext(mockProcessContext);

    when(mockProcessContext.executable(Mockito.anyString())).thenReturn(mockProcessContext);
    when(mockProcessContext.addArgs(Mockito.any(String[].class))).thenReturn(mockProcessContext);

    when(mockProcessContext.executable(Mockito.any(Path.class))).thenReturn(mockProcessContext);
    when(mockProcessContext.errorHandling(Mockito.any())).thenReturn(mockProcessContext);

    // act
    commandlet.install();

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
  public void dotnetShouldRunExecutableForWindowsSuccessful() {

    if(SystemInfoImpl.INSTANCE.isWindows()) {

      String expectedOutputWindows = "Dummy dotnet 6.0.419 on windows ";
      runExecutable("windows");
      checkExpectedOutput(expectedOutputWindows);
    }
  }

  @Test
  public void dotnetShouldRunExecutableForLinuxSuccessful() {

    if(SystemInfoImpl.INSTANCE.isLinux()) {

      String expectedOutputLinux = "Dummy dotnet 6.0.419 on linux ";
      runExecutable("linux");
      checkExpectedOutput(expectedOutputLinux);
    }
  }

  @Test
  public void dotnetShouldRunExecutableForMacOSSuccessful() {

    if(SystemInfoImpl.INSTANCE.isMac()) {

      String expectedOutputMacOs = "Dummy dotnet 6.0.419 on mac ";
      runExecutable("mac");
      checkExpectedOutput(expectedOutputMacOs);
    }
  }

  private void runExecutable(String operatingSystem){

    SystemInfo systemInfo = SystemInfoMock.of(operatingSystem);
    context.setSystemInfo(systemInfo);
    assignDummyUserHome(context, "dummyUserHome");

    commandlet.run();
  }

  private void checkExpectedOutput(String expectedOutput){
    assertThat(MOCK_RESULT_PATH.resolve("dotnetTestResult.txt")).exists();
    assertThat(MOCK_RESULT_PATH.resolve("dotnetTestResult.txt")).hasContent(expectedOutput);
    assertThat(context.getIdeHome()).isEqualTo(context.getDefaultExecutionDirectory());
  }

  @Test
  public void argumentsShouldBeModifiedInCreateCase() throws IllegalAccessException {

    // arrange
    String expectedModifiedArgsAsString = "new Devon4NetAPI";

    assignDummyUserHome(context, "dummyUserHome");
    commandlet.install();
    modifyArgumentsOfCommandlet(commandlet);

    //act
    commandlet.run();

    // assert
    assertThat(MOCK_RESULT_PATH.resolve("dotnetTestResult.txt")).exists();
    assertThat(MOCK_RESULT_PATH.resolve("dotnetTestResult.txt")).content().contains(expectedModifiedArgsAsString);
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

  private static void assignDummyUserHome(IdeTestContext context, String pathString) {

    Path dummyUserHomePath = PROJECTS_TARGET_PATH.resolve(PROJECT_DOTNET).resolve(pathString);
    context.setDummyUserHome(dummyUserHomePath);
  }
}

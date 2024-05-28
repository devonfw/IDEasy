package com.devonfw.tools.ide.tool.mvn;

import com.devonfw.tools.ide.commandlet.InstallCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.GitContext;
import com.devonfw.tools.ide.context.GitContextMock;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

/**
 * Integration test of {@link Mvn}.
 */
public class MvnTest extends AbstractIdeContextTest {

  private static final String PROJECT_MVN = "mvn";

  @Test
  public void testMvnInstallCommandlet() {

    // arrange
    IdeTestContext context = newContext(PROJECT_MVN);
    InstallCommandlet install = context.getCommandletManager().getCommandlet(InstallCommandlet.class);
    install.tool.setValueAsString("mvn", context);
    context.setInputValues(List.of("value1", "value2"));
    // act
    install.run();

    // assert
    checkInstallation(context);
  }

  @Test
  public void testMvnInstall() {

    // arrange
    IdeTestContext context = newContext(PROJECT_MVN);
    Mvn commandlet = new Mvn(context);
    context.setInputValues(List.of("value1", "value2"));

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  @Test
  public void testMvnInstallCustomSettingsFile() {

    // arrange
    IdeTestContext context = newContext(PROJECT_MVN);

    GitContextMock mockedGitContext = (GitContextMock) context.getGitContext();
    mockedGitContext.setGitUrlValue(GitContext.DEFAULT_SETTINGS_GIT_URL);

    Mvn commandlet = new Mvn(context);

    context.setInputValues(List.of("value1", "value2"));

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testMvnRun(String os) {

    // arrange
    IdeTestContext context = newContext(PROJECT_MVN);
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    Mvn commandlet = new Mvn(context);
    commandlet.arguments.setValue(List.of("foo", "bar"));
    // act
    commandlet.run();

    // assert
    assertLogMessage(context, IdeLogLevel.INFO, "mvn " + os + " foo bar");
    checkInstallation(context);
  }

  //  private void checkInstallation(IdeTestContext context) {
  //
  //    // install - java
  //    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();
  //
  //    // commandlet - mvn
  //    assertThat(context.getSoftwarePath().resolve("mvn/mvn-1.9.3.jar")).hasContent("This is a jar file.");
  //    assertThat(context.getSoftwarePath().resolve("mvn/.ide.software.version")).exists().hasContent("1.9.3");
  //    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed mvn in version 1.9.3");
  //  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();

    //    if (context.getSystemInfo().isWindows() || context.getSystemInfo().isLinux()) {
    //      assertThat(context.getSoftwarePath().resolve("mvn/HelloWorld.txt")).hasContent("Hello World!");
    //      assertThat(context.getSoftwarePath().resolve("mvn/JDK Mission Control")).doesNotExist();
    //    } else if (context.getSystemInfo().isMac()) {
    //      assertThat(context.getSoftwarePath().resolve("mvn/mvn")).exists();
    //    }
    assertThat(context.getSoftwarePath().resolve("mvn/.ide.software.version")).exists().hasContent("3.9.7");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed mvn in version 3.9.7");
  }
}

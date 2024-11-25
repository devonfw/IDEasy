package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Test of {@link EnvironmentCommandlet}.
 */
public class EnvironmentCommandletTest extends AbstractIdeContextTest {

  /**
   * Test of {@link EnvironmentCommandlet} run with DEBUG logging (partitioning per type/source).
   */
  @Test
  public void testRunDebugLogging() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    IdeTestContext context = newContext(PROJECT_BASIC, path, false);
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    EnvironmentCommandlet env = context.getCommandletManager().getCommandlet(EnvironmentCommandlet.class);
    Path userProperties = context.getUserHomeIde().resolve("ide.properties");
    Path settingsIdeProperties = context.getSettingsPath().resolve("ide.properties");
    Path confIdeProperties = context.getConfPath().resolve("ide.properties");
    Path workspaceIdeProperties = context.getWorkspacePath().resolve("ide.properties");
    // act
    env.run();
    // assert
    assertThat(context).log().hasEntriesWithNothingElseInBetween( //
        IdeLogEntry.ofDebug("from USER@" + userProperties.toString() + ":"), //
        IdeLogEntry.ofProcessable("DOCKER_EDITION=\"docker\""), //
        IdeLogEntry.ofProcessable("FOO=\"foo-bar-some-${UNDEFINED}\""), //

        IdeLogEntry.ofDebug("from SETTINGS@" + settingsIdeProperties + ":"),
        IdeLogEntry.ofProcessable("BAR=\"bar-some-${UNDEFINED}\""), //
        IdeLogEntry.ofProcessable("ECLIPSE_VERSION=\"2023-03\""), //
        IdeLogEntry.ofProcessable("IDE_TOOLS=\"mvn,eclipse\""), //
        IdeLogEntry.ofProcessable("INTELLIJ_EDITION=\"ultimate\""), //
        IdeLogEntry.ofProcessable("JAVA_VERSION=\"17*\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS4=\" settings4\""), //
        IdeLogEntry.ofProcessable("TEST_ARGSb=\"user10 workspace10 settingsb  user1 settings1 workspace1 conf1  user3 workspace3 confa userb\""), //

        IdeLogEntry.ofDebug("from WORKSPACE@" + workspaceIdeProperties + ":"), //
        IdeLogEntry.ofProcessable("TEST_ARGS10=\"user10 workspace10\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS3=\" user3 workspace3\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS9=\"settings9 workspace9\""), //
        IdeLogEntry.ofProcessable("TEST_ARGSd=\" user1 settings1 workspace1 conf1 userd workspaced\""), //

        IdeLogEntry.ofDebug("from CONF@" + confIdeProperties + ":"), //
        IdeLogEntry.ofProcessable("MVN_VERSION=\"3.9.1\""), //
        IdeLogEntry.ofProcessable("SOME=\"some-${UNDEFINED}\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS1=\" user1 settings1 workspace1 conf1\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS2=\" user2 conf2\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS5=\" settings5 conf5\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS6=\" settings6 workspace6 conf6\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS7=\"user7 settings7 workspace7 conf7\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS8=\"settings8 workspace8 conf8\""), //
        IdeLogEntry.ofProcessable("TEST_ARGSa=\" user1 settings1 workspace1 conf1  user3 workspace3 confa\""), //
        IdeLogEntry.ofProcessable("TEST_ARGSc=\" user1 settings1 workspace1 conf1 userc settingsc confc\""), //

        IdeLogEntry.ofDebug("from RESOLVED:"), //
        IdeLogEntry.ofProcessable("HOME=\"" + normalize(context.getUserHome()) + "\""), //
        IdeLogEntry.ofProcessable("IDE_HOME=\"" + normalize(context.getIdeHome()) + "\""), //
        IdeLogEntry.ofProcessable("export M2_REPO=\"" + context.getUserHome() + "/.m2/repository\""), //
        new IdeLogEntry(IdeLogLevel.PROCESSABLE, "export PATH=", true), //
        IdeLogEntry.ofProcessable("WORKSPACE=\"foo-test\""), //
        IdeLogEntry.ofProcessable("WORKSPACE_PATH=\"" + normalize(context.getWorkspacePath()) + "\"") //
    );
  }

  /**
   * Test of {@link EnvironmentCommandlet} run with INFO logging (plain variables without source).
   */
  @Test
  public void testRunInfoLogging() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    IdeTestContext context = newContext(PROJECT_BASIC, path, false, IdeLogLevel.INFO);
    context.setSystemInfo(SystemInfoMock.MAC_ARM64);
    EnvironmentCommandlet env = context.getCommandletManager().getCommandlet(EnvironmentCommandlet.class);
    Path userProperties = context.getUserHomeIde().resolve("ide.properties");
    Path settingsIdeProperties = context.getSettingsPath().resolve("ide.properties");
    Path confIdeProperties = context.getConfPath().resolve("ide.properties");
    Path workspaceIdeProperties = context.getWorkspacePath().resolve("ide.properties");
    // act
    env.run();
    // assert
    assertThat(context).log().hasEntriesWithNothingElseInBetween( //
        IdeLogEntry.ofProcessable("BAR=\"bar-some-${UNDEFINED}\""), //
        IdeLogEntry.ofProcessable("DOCKER_EDITION=\"docker\""), //
        IdeLogEntry.ofProcessable("ECLIPSE_VERSION=\"2023-03\""), //
        IdeLogEntry.ofProcessable("FOO=\"foo-bar-some-${UNDEFINED}\""), //
        IdeLogEntry.ofProcessable("HOME=\"" + normalize(context.getUserHome()) + "\""), //
        IdeLogEntry.ofProcessable("IDE_HOME=\"" + normalize(context.getIdeHome()) + "\""), //
        IdeLogEntry.ofProcessable("IDE_TOOLS=\"mvn,eclipse\""), //
        IdeLogEntry.ofProcessable("INTELLIJ_EDITION=\"ultimate\""), //
        IdeLogEntry.ofProcessable("JAVA_VERSION=\"17*\""), //
        IdeLogEntry.ofProcessable("export M2_REPO=\"" + context.getUserHome() + "/.m2/repository\""), //
        IdeLogEntry.ofProcessable("MVN_VERSION=\"3.9.1\""), //
        new IdeLogEntry(IdeLogLevel.PROCESSABLE, "export PATH=", true), //
        IdeLogEntry.ofProcessable("SOME=\"some-${UNDEFINED}\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS1=\" user1 settings1 workspace1 conf1\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS10=\"user10 workspace10\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS2=\" user2 conf2\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS3=\" user3 workspace3\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS4=\" settings4\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS5=\" settings5 conf5\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS6=\" settings6 workspace6 conf6\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS7=\"user7 settings7 workspace7 conf7\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS8=\"settings8 workspace8 conf8\""), //
        IdeLogEntry.ofProcessable("TEST_ARGS9=\"settings9 workspace9\""), //
        IdeLogEntry.ofProcessable("TEST_ARGSa=\" user1 settings1 workspace1 conf1  user3 workspace3 confa\""), //
        IdeLogEntry.ofProcessable("TEST_ARGSb=\"user10 workspace10 settingsb  user1 settings1 workspace1 conf1  user3 workspace3 confa userb\""), //
        IdeLogEntry.ofProcessable("TEST_ARGSc=\" user1 settings1 workspace1 conf1 userc settingsc confc\""), //
        IdeLogEntry.ofProcessable("TEST_ARGSd=\" user1 settings1 workspace1 conf1 userd workspaced\""), //
        IdeLogEntry.ofProcessable("WORKSPACE=\"foo-test\""), //
        IdeLogEntry.ofProcessable("WORKSPACE_PATH=\"" + normalize(context.getWorkspacePath()) + "\"") //
    );
  }

  /**
   * Test that {@link EnvironmentCommandlet} requires home.
   */
  @Test
  public void testThatHomeIsRequired() {

    // arrange
    EnvironmentCommandlet env = new EnvironmentCommandlet(IdeTestContextMock.get());
    // act & assert
    assertThat(env.isIdeHomeRequired()).isTrue();
  }

  private String normalize(Path path) {

    return path.toString().replace('\\', '/');
  }
}

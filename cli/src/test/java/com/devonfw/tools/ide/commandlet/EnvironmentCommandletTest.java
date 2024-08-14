package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfoImpl;

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
        IdeLogEntry.ofInfo("DOCKER_EDITION=\"docker\""), //
        IdeLogEntry.ofInfo("FOO=\"foo-bar-some-${UNDEFINED}\""), //

        IdeLogEntry.ofDebug("from SETTINGS@" + settingsIdeProperties + ":"),
        IdeLogEntry.ofInfo("BAR=\"bar-some-${UNDEFINED}\""), //
        IdeLogEntry.ofInfo("ECLIPSE_VERSION=\"2023-03\""), //
        IdeLogEntry.ofInfo("IDE_TOOLS=\"mvn,eclipse\""), //
        IdeLogEntry.ofInfo("INTELLIJ_EDITION=\"ultimate\""), //
        IdeLogEntry.ofInfo("JAVA_VERSION=\"17*\""), //
        IdeLogEntry.ofInfo("TEST_ARGS4=\" settings4\""), //
        IdeLogEntry.ofInfo("TEST_ARGSb=\"user10 workspace10 settingsb  user1 settings1 workspace1 conf1  user3 workspace3 confa userb\""), //

        IdeLogEntry.ofDebug("from WORKSPACE@" + workspaceIdeProperties + ":"), //
        IdeLogEntry.ofInfo("TEST_ARGS10=\"user10 workspace10\""), //
        IdeLogEntry.ofInfo("TEST_ARGS3=\" user3 workspace3\""), //
        IdeLogEntry.ofInfo("TEST_ARGS9=\"settings9 workspace9\""), //
        IdeLogEntry.ofInfo("TEST_ARGSd=\" user1 settings1 workspace1 conf1 userd workspaced\""), //

        IdeLogEntry.ofDebug("from CONF@" + confIdeProperties + ":"), //
        IdeLogEntry.ofInfo("MVN_VERSION=\"3.9.1\""), //
        IdeLogEntry.ofInfo("SOME=\"some-${UNDEFINED}\""), //
        IdeLogEntry.ofInfo("TEST_ARGS1=\" user1 settings1 workspace1 conf1\""), //
        IdeLogEntry.ofInfo("TEST_ARGS2=\" user2 conf2\""), //
        IdeLogEntry.ofInfo("TEST_ARGS5=\" settings5 conf5\""), //
        IdeLogEntry.ofInfo("TEST_ARGS6=\" settings6 workspace6 conf6\""), //
        IdeLogEntry.ofInfo("TEST_ARGS7=\"user7 settings7 workspace7 conf7\""), //
        IdeLogEntry.ofInfo("TEST_ARGS8=\"settings8 workspace8 conf8\""), //
        IdeLogEntry.ofInfo("TEST_ARGSa=\" user1 settings1 workspace1 conf1  user3 workspace3 confa\""), //
        IdeLogEntry.ofInfo("TEST_ARGSc=\" user1 settings1 workspace1 conf1 userc settingsc confc\""), //

        IdeLogEntry.ofDebug("from RESOLVED:"), //
        IdeLogEntry.ofInfo("HOME=\"" + context.getUserHome() + "\""), //
        IdeLogEntry.ofInfo("IDE_HOME=\"" + context.getIdeHome() + "\""), //
        IdeLogEntry.ofInfo("export M2_REPO=\"" + context.getUserHome() + "/.m2/repository\""), //
        new IdeLogEntry(IdeLogLevel.INFO, "export PATH=", true), //
        IdeLogEntry.ofInfo("WORKSPACE=\"foo-test\""), //
        IdeLogEntry.ofInfo("WORKSPACE_PATH=\"" + context.getWorkspacePath() + "\"") //
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
    EnvironmentCommandlet env = context.getCommandletManager().getCommandlet(EnvironmentCommandlet.class);
    Path userProperties = context.getUserHomeIde().resolve("ide.properties");
    Path settingsIdeProperties = context.getSettingsPath().resolve("ide.properties");
    Path confIdeProperties = context.getConfPath().resolve("ide.properties");
    Path workspaceIdeProperties = context.getWorkspacePath().resolve("ide.properties");
    // act
    env.run();
    // assert
    if (SystemInfoImpl.INSTANCE.isWindows()) {
      assertThat(context).log().hasEntries /*WithNothingElseInBetween*/( //
          IdeLogEntry.ofInfo("BAR=bar-some-${UNDEFINED}"), //
          IdeLogEntry.ofInfo("DOCKER_EDITION=docker"), //
          IdeLogEntry.ofInfo("ECLIPSE_VERSION=2023-03"), //
          IdeLogEntry.ofInfo("FOO=foo-bar-some-${UNDEFINED}"), //
          IdeLogEntry.ofInfo("HOME=" + context.getUserHome() + ""), //
          IdeLogEntry.ofInfo("IDE_HOME=" + context.getIdeHome() + ""), //
          IdeLogEntry.ofInfo("IDE_TOOLS=mvn,eclipse"), //
          IdeLogEntry.ofInfo("INTELLIJ_EDITION=ultimate"), //
          IdeLogEntry.ofInfo("JAVA_VERSION=17*"), //
          IdeLogEntry.ofInfo("M2_REPO=" + context.getUserHome() + "/.m2/repository"), //
          IdeLogEntry.ofInfo("MVN_VERSION=3.9.1"), //
          new IdeLogEntry(IdeLogLevel.INFO, "PATH=", true), //
          IdeLogEntry.ofInfo("SOME=some-${UNDEFINED}"), //
          IdeLogEntry.ofInfo("TEST_ARGS1= user1 settings1 workspace1 conf1"), //
          IdeLogEntry.ofInfo("TEST_ARGS10=user10 workspace10"), //
          IdeLogEntry.ofInfo("TEST_ARGS2= user2 conf2"), //
          IdeLogEntry.ofInfo("TEST_ARGS3= user3 workspace3"), //
          IdeLogEntry.ofInfo("TEST_ARGS4= settings4"), //
          IdeLogEntry.ofInfo("TEST_ARGS5= settings5 conf5"), //
          IdeLogEntry.ofInfo("TEST_ARGS6= settings6 workspace6 conf6"), //
          IdeLogEntry.ofInfo("TEST_ARGS7=user7 settings7 workspace7 conf7"), //
          IdeLogEntry.ofInfo("TEST_ARGS8=settings8 workspace8 conf8"), //
          IdeLogEntry.ofInfo("TEST_ARGS9=settings9 workspace9"), //
          IdeLogEntry.ofInfo("TEST_ARGSa= user1 settings1 workspace1 conf1  user3 workspace3 confa"), //
          IdeLogEntry.ofInfo("TEST_ARGSb=user10 workspace10 settingsb  user1 settings1 workspace1 conf1  user3 workspace3 confa userb"), //
          IdeLogEntry.ofInfo("TEST_ARGSc= user1 settings1 workspace1 conf1 userc settingsc confc"), //
          IdeLogEntry.ofInfo("TEST_ARGSd= user1 settings1 workspace1 conf1 userd workspaced"), //
          IdeLogEntry.ofInfo("WORKSPACE=foo-test"), //
          IdeLogEntry.ofInfo("WORKSPACE_PATH=" + context.getWorkspacePath()) //
      );
    } else {
      assertThat(context).log().hasEntriesWithNothingElseInBetween( //
          IdeLogEntry.ofInfo("BAR=\"bar-some-${UNDEFINED}\""), //
          IdeLogEntry.ofInfo("DOCKER_EDITION=\"docker\""), //
          IdeLogEntry.ofInfo("ECLIPSE_VERSION=\"2023-03\""), //
          IdeLogEntry.ofInfo("FOO=\"foo-bar-some-${UNDEFINED}\""), //
          IdeLogEntry.ofInfo("HOME=\"" + context.getUserHome() + "\""), //
          IdeLogEntry.ofInfo("IDE_HOME=\"" + context.getIdeHome() + "\""), //
          IdeLogEntry.ofInfo("IDE_TOOLS=\"mvn,eclipse\""), //
          IdeLogEntry.ofInfo("INTELLIJ_EDITION=\"ultimate\""), //
          IdeLogEntry.ofInfo("JAVA_VERSION=\"17*\""), //
          IdeLogEntry.ofInfo("export M2_REPO=\"" + context.getUserHome() + "/.m2/repository\""), //
          IdeLogEntry.ofInfo("MVN_VERSION=\"3.9.1\""), //
          new IdeLogEntry(IdeLogLevel.INFO, "export PATH=", true), //
          IdeLogEntry.ofInfo("SOME=\"some-${UNDEFINED}\""), //
          IdeLogEntry.ofInfo("TEST_ARGS1=\" user1 settings1 workspace1 conf1\""), //
          IdeLogEntry.ofInfo("TEST_ARGS10=\"user10 workspace10\""), //
          IdeLogEntry.ofInfo("TEST_ARGS2=\" user2 conf2\""), //
          IdeLogEntry.ofInfo("TEST_ARGS3=\" user3 workspace3\""), //
          IdeLogEntry.ofInfo("TEST_ARGS4=\" settings4\""), //
          IdeLogEntry.ofInfo("TEST_ARGS5=\" settings5 conf5\""), //
          IdeLogEntry.ofInfo("TEST_ARGS6=\" settings6 workspace6 conf6\""), //
          IdeLogEntry.ofInfo("TEST_ARGS7=\"user7 settings7 workspace7 conf7\""), //
          IdeLogEntry.ofInfo("TEST_ARGS8=\"settings8 workspace8 conf8\""), //
          IdeLogEntry.ofInfo("TEST_ARGS9=\"settings9 workspace9\""), //
          IdeLogEntry.ofInfo("TEST_ARGSa=\" user1 settings1 workspace1 conf1  user3 workspace3 confa\""), //
          IdeLogEntry.ofInfo("TEST_ARGSb=\"user10 workspace10 settingsb  user1 settings1 workspace1 conf1  user3 workspace3 confa userb\""), //
          IdeLogEntry.ofInfo("TEST_ARGSc=\" user1 settings1 workspace1 conf1 userc settingsc confc\""), //
          IdeLogEntry.ofInfo("TEST_ARGSd=\" user1 settings1 workspace1 conf1 userd workspaced\""), //
          IdeLogEntry.ofInfo("WORKSPACE=\"foo-test\""), //
          IdeLogEntry.ofInfo("WORKSPACE_PATH=\"" + context.getWorkspacePath() + "\"") //
      );
    }
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
}

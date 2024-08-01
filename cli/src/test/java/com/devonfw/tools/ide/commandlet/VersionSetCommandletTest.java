package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariablesFiles;

/**
 * Integration test of {@link VersionSetCommandlet}.
 */
public class VersionSetCommandletTest extends AbstractIdeContextTest {

  /**
   * Test of {@link VersionSetCommandlet} run.
   *
   * @throws IOException on error.
   */
  @Test
  public void testVersionSetCommandletRun() throws IOException {

    // arrange
    IdeContext context = newContext(PROJECT_BASIC);
    VersionSetCommandlet versionSet = context.getCommandletManager().getCommandlet(VersionSetCommandlet.class);
    versionSet.tool.setValueAsString("mvn", context);
    versionSet.version.setValueAsString("3.1.0", context);
    // act
    versionSet.run();
    // assert
    Path settingsIdeProperties = context.getSettingsPath().resolve("ide.properties");
    assertThat(settingsIdeProperties).hasContent("""
        #********************************************************************************
        # This file contains project specific environment variables
        #********************************************************************************
        JAVA_VERSION=17*
        MVN_VERSION=3.1.0
        ECLIPSE_VERSION=2023-03
        INTELLIJ_EDITION=ultimate
        IDE_TOOLS=mvn,eclipse
        BAR=bar-${SOME}
        TEST_ARGS1=${TEST_ARGS1} settings1
        TEST_ARGS4=${TEST_ARGS4} settings4
        TEST_ARGS5=${TEST_ARGS5} settings5
        TEST_ARGS6=${TEST_ARGS6} settings6
        TEST_ARGS7=${TEST_ARGS7} settings7
        TEST_ARGS8=settings8
        TEST_ARGS9=settings9
        TEST_ARGSb=${TEST_ARGS10} settingsb ${TEST_ARGSa} ${TEST_ARGSb}
        TEST_ARGSc=${TEST_ARGSc} settingsc""");
  }

  /**
   * Test of {@link VersionSetCommandlet} run.
   *
   * @throws IOException on error.
   */
  @Test
  public void testVersionSetCommandletConfRun() throws IOException {

    // arrange
    IdeContext context = newContext(PROJECT_BASIC);
    VersionSetCommandlet versionSet = context.getCommandletManager().getCommandlet(VersionSetCommandlet.class);
    versionSet.tool.setValueAsString("mvn", context);
    versionSet.version.setValueAsString("3.2.1", context);
    versionSet.cfg.setValue(EnvironmentVariablesFiles.CONF);
    // act
    versionSet.run();
    // assert
    Path confIdeProperties = context.getConfPath().resolve("ide.properties");
    assertThat(confIdeProperties).hasContent("""
        #********************************************************************************
        # This file contains project specific environment variables defined by the user
        #********************************************************************************
        M2_REPO=~/.m2/repository
        MVN_VERSION=3.2.1
        SOME=some-${UNDEFINED}
        TEST_ARGS1=${TEST_ARGS1} conf1
        TEST_ARGS2=${TEST_ARGS2} conf2
        TEST_ARGS5=${TEST_ARGS5} conf5
        TEST_ARGS6=${TEST_ARGS6} conf6
        TEST_ARGS7=${TEST_ARGS7} conf7
        TEST_ARGS8=${TEST_ARGS8} conf8
        TEST_ARGSa=${TEST_ARGS1} ${TEST_ARGS3} confa
        TEST_ARGSc=${TEST_ARGSc} confc""");
  }
}

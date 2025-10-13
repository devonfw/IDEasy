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

  private static final String PROJECT_SETTINGS = "settings";

  /**
   * Test of {@link VersionSetCommandlet} run.
   *
   * @throws IOException on error.
   */
  @Test
  public void testVersionSetCommandletRun() {

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
        BAR=bar-${SOME}""");
  }

  /**
   * Test of {@link VersionSetCommandlet} run.
   *
   * @throws IOException on error.
   */
  @Test
  public void testVersionSetCommandletConfRun() {

    // arrange
    IdeContext context = newContext(PROJECT_SETTINGS);
    VersionSetCommandlet versionSet = context.getCommandletManager().getCommandlet(VersionSetCommandlet.class);
    versionSet.tool.setValueAsString("mvn", context);
    versionSet.cfg.setValue(EnvironmentVariablesFiles.CONF);
    versionSet.version.setValueAsString("3.2.0", context);
    // act
    versionSet.run();
    // assert
    Path confIdeProperties = context.getConfPath().resolve("ide.properties");
    assertThat(confIdeProperties).hasContent("""
        #********************************************************************************
        # This file contains project specific environment variables defined by the user
        #********************************************************************************
        MVN_VERSION=3.2.0""");
  }

  @Test
  public void testVersionSetCommandletWorkspaceRun() {

    // arrange
    IdeContext context = newContext(PROJECT_SETTINGS);
    VersionSetCommandlet versionSet = context.getCommandletManager().getCommandlet(VersionSetCommandlet.class);
    versionSet.tool.setValueAsString("mvn", context);
    versionSet.cfg.setValue(EnvironmentVariablesFiles.WORKSPACE);
    versionSet.version.setValueAsString("3.2.1", context);
    // act
    versionSet.run();
    // assert
    Path confIdeProperties = context.getWorkspacePath().resolve("ide.properties");
    assertThat(confIdeProperties).hasContent("""
        #********************************************************************************
        # This file contains workspace specific environment variables
        #********************************************************************************
        MVN_VERSION=3.2.1""");
  }

  @Test
  public void testVersionSetCommandletUserRun() {

    // arrange
    IdeContext context = newContext(PROJECT_SETTINGS);
    VersionSetCommandlet versionSet = context.getCommandletManager().getCommandlet(VersionSetCommandlet.class);
    versionSet.tool.setValueAsString("mvn", context);
    versionSet.cfg.setValue(EnvironmentVariablesFiles.USER);
    versionSet.version.setValueAsString("3.2.2", context);
    // act
    versionSet.run();
    // assert
    Path confIdeProperties = context.getUserHome().resolve(".ide").resolve("ide.properties");
    assertThat(confIdeProperties).hasContent("""
        #********************************************************************************
        # This file contains the global configuration from the user HOME directory.
        #********************************************************************************
        MVN_VERSION=3.2.2""");
  }

  @Test
  public void testVersionSetCommandletSettingsRun() {

    // arrange
    IdeContext context = newContext(PROJECT_SETTINGS);
    VersionSetCommandlet versionSet = context.getCommandletManager().getCommandlet(VersionSetCommandlet.class);
    versionSet.tool.setValueAsString("mvn", context);
    versionSet.cfg.setValue(EnvironmentVariablesFiles.SETTINGS);
    versionSet.version.setValueAsString("3.2.3", context);
    // act
    versionSet.run();
    // assert
    Path confIdeProperties = context.getSettingsPath().resolve("ide.properties");
    assertThat(confIdeProperties).hasContent("""
        #********************************************************************************
        # This file contains project specific environment variables
        #********************************************************************************
        MVN_VERSION=3.2.3""");
  }
}

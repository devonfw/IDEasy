package com.devonfw.tools.ide.tool.npm;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.claude.RecordingEnvironmentContext;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Npm}.
 */
@WireMockTest
class NpmTest extends AbstractIdeContextTest {

  private static final String PROJECT_NPM = "npm";

  /**
   * Tests if the {@link Npm} install works correctly across all three operating systems.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testNpmInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, wireMockRuntimeInfo);
    Npm commandlet = new Npm(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if npm can be run properly.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testNpmRun(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, wireMockRuntimeInfo);
    Npm commandlet = new Npm(context);
    commandlet.arguments.setValue("--version");

    // act
    commandlet.run();

    // assert
    assertThat(context).logAtInfo().hasMessage("9.9.2");
  }

  /**
   * Tests if the {@link Npm} uninstall works correctly.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  @Test
  void testNpmUninstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, wireMockRuntimeInfo);
    Npm commandlet = new Npm(context);

    // act I
    commandlet.install();

    // assert I
    checkInstallation(context);

    // act II
    commandlet.uninstall();

    // assert II
    assertThat(context).logAtInfo().hasNoMessageContaining("npm uninstall -g npm");

    assertThat(context).logAtSuccess().hasMessage("Successfully uninstalled npm");
  }

  /**
   * Tests that {@link Npm#setEnvironment(EnvironmentContext, ToolInstallation, boolean)} points the npm global prefix at a per-project
   * {@code .npm-global} folder inside the IDE home so that projects do not interfere with each other (see
   * <a href="https://github.com/devonfw/IDEasy/issues/352">issue #352</a> and <a href=
   * "https://github.com/devonfw/IDEasy/issues/2381">issue #2381</a>).
   */
  @Test
  void testSetEnvironmentPointsGlobalPrefixToPerProjectNpmGlobal() {

    // arrange
    IdeTestContext context = newContext(PROJECT_NPM, (String) null, false);
    Npm commandlet = new Npm(context);
    Path dummy = context.getSoftwarePath().resolve("npm");
    ToolInstallation installation = new ToolInstallation(dummy, dummy, dummy, VersionIdentifier.of("9.9.2"), false);
    RecordingEnvironmentContext environmentContext = new RecordingEnvironmentContext();

    // act
    commandlet.setEnvironment(environmentContext, installation, false);

    // assert
    Path npmGlobalPath = context.getIdeHome().resolve(Npm.NPM_GLOBAL_FOLDER);
    assertThat(environmentContext.set).containsEntry("npm_config_prefix", npmGlobalPath.toString());
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed npm in version 9.9.2");
  }
}

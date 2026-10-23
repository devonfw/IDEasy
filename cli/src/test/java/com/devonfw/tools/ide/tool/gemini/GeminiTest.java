package com.devonfw.tools.ide.tool.gemini;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.npm.Npm;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Gemini}.
 */
@WireMockTest
public class GeminiTest extends AbstractIdeContextTest {

  private static final String PROJECT_GEMINI = "gemini";

  @Test
  void testGeminiInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    IdeTestContext context = newContext(PROJECT_GEMINI, wireMockRuntimeInfo);
    Gemini commandlet = new Gemini(context);

    commandlet.install();

    checkInstallation(context);
  }

  @Test
  void testGeminiUninstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    IdeTestContext context = newContext(PROJECT_GEMINI, wireMockRuntimeInfo);
    Gemini commandlet = new Gemini(context);

    commandlet.install();

    checkInstallation(context);

    commandlet.uninstall();

    assertThat(context).logAtInfo().hasMessageContaining("npm uninstall -g @google/gemini-cli");

    assertThat(context).logAtSuccess().hasMessage("Successfully uninstalled gemini");
  }

  @Test
  void testGeminiRun(WireMockRuntimeInfo wireMockRuntimeInfo) {

    IdeTestContext context = newContext(PROJECT_GEMINI, wireMockRuntimeInfo);
    Gemini commandlet = new Gemini(context);
    commandlet.arguments.setValue("--version");

    commandlet.run();

    assertThat(context).logAtInfo().hasMessageContaining("gemini --version");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context).logAtInfo().hasMessageContaining("npm install -gf @google/gemini-cli@");
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed gemini in version");
    assertThat(context).logAtSuccess().hasMessageContaining("at " + context.getSoftwarePath().resolve(Npm.NPM_GLOBAL_FOLDER));
  }

  @Test
  void testGetSortedVersionsSkipsInvalidVersions(WireMockRuntimeInfo wireMockRuntimeInfo) {
    IdeTestContext context = newContext(PROJECT_GEMINI, wireMockRuntimeInfo);
    Gemini commandlet = new Gemini(context);

    List<VersionIdentifier> versions = context.getNpmRepository().getSortedVersions(commandlet.getName(), null, commandlet);

    assertThat(versions).contains(VersionIdentifier.ofRequired("0.61.0"));
    assertThat(context).logAtWarning().hasMessageContaining("Skipping invalid npm version '0.0.8888888888'");
  }
}

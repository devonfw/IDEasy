package com.devonfw.tools.ide.tool.gcloganalyzer;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link GcLogAnalyzer}.
 */
@WireMockTest
class GcLogAnalyzerTest extends AbstractIdeContextTest {

  private static final String PROJECT_GCLOGANALYZER = "gcloganalyzer";

  private static final String VERSION = "24.10.0.0";

  private static final String ARTIFACT_VERSION = "24.10.0";

  @Test
  void testGcLogAnalyzerInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    IdeTestContext context = newContext(PROJECT_GCLOGANALYZER, wireMockRuntimeInfo);
    GcLogAnalyzer commandlet = context.getCommandletManager().getCommandlet(GcLogAnalyzer.class);

    commandlet.install();

    assertThat(commandlet.getInstalledVersion().toString()).isEqualTo(VERSION);
    assertThat(context.getSoftwarePath().resolve("gcloganalyzer/.ide.software.version")).hasContent(VERSION);
    assertThat(context.getSoftwarePath()
        .resolve("gcloganalyzer/GCLogAnalyzer-" + ARTIFACT_VERSION + "-ca.jar"))
        .hasContent("This is a fake GC Log Analyzer jar file.\n");
    assertThat(context.getSoftwarePath()
        .resolve("gcloganalyzer/GCLogAnalyzer-" + ARTIFACT_VERSION + "-ca"))
        .doesNotExist();
    assertThat(context).logAtSuccess()
        .hasMessageContaining("Successfully installed gcloganalyzer in version " + VERSION);
  }

  @Test
  void testGcLogAnalyzerRunWithArguments(WireMockRuntimeInfo wireMockRuntimeInfo) {

    IdeTestContext context = newContext(PROJECT_GCLOGANALYZER, wireMockRuntimeInfo);
    GcLogAnalyzer commandlet = context.getCommandletManager().getCommandlet(GcLogAnalyzer.class);

    commandlet.arguments.addValue("--generate-html");
    commandlet.arguments.addValue("output");
    commandlet.arguments.addValue("gc.log");

    commandlet.run();

    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();
    assertThat(context).logAtInfo().hasMessageContaining(
        "java -jar GCLogAnalyzer-" + ARTIFACT_VERSION + "-ca.jar --generate-html output gc.log");
  }
}

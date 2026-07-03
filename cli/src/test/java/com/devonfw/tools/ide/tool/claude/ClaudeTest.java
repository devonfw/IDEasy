package com.devonfw.tools.ide.tool.claude;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@WireMockTest
class ClaudeTest extends AbstractIdeContextTest {

  private static final String PROJECT_CLAUDE = "claude";

  private static final String CLAUDE_VERSION = "2.1.118";

  @Test
  void testClaudeInstallSucceedsOnAllPlatformsViaHttpDownload(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_CLAUDE, wireMockRuntimeInfo);
    Claude claude = new Claude(context);

    // act
    claude.install();

    // assert
    assertInstalled(context);
  }

  @Test
  void testClaudeRunInstallsAndPassesArguments(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_CLAUDE, wireMockRuntimeInfo);
    Claude claude = new Claude(context);
    claude.arguments.addValue("hello");
    claude.arguments.addValue("world");

    // act
    claude.run();

    // assert
    assertInstalled(context);
    assertThat(context).logAtInfo().hasMessage("claude hello world");
  }

  @Test
  void testSetEnvironmentIsolatesConfigDirAndScrubsLeakingVars() {

    // arrange
    IdeTestContext context = newContext(PROJECT_CLAUDE, null, false);
    Claude claude = new Claude(context);
    Path dummy = context.getSoftwarePath().resolve("claude");
    ToolInstallation installation = new ToolInstallation(dummy, dummy, dummy, VersionIdentifier.of(CLAUDE_VERSION), false);
    RecordingEnvironmentContext ec = new RecordingEnvironmentContext();

    // act
    claude.setEnvironment(ec, installation, false);

    // assert
    Path expectedConfigDir = context.getConfPath().resolve("claude");
    assertThat(ec.set).containsEntry("CLAUDE_CONFIG_DIR", expectedConfigDir.toString());
    assertThat(ec.removed).contains("ANTHROPIC_API_KEY", "ANTHROPIC_AUTH_TOKEN", "ANTHROPIC_BASE_URL",
        "CLAUDE_CODE_USE_BEDROCK", "CLAUDE_CODE_OAUTH_TOKEN", "AWS_PROFILE", "AWS_REGION",
        "AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY", "AWS_SESSION_TOKEN", "AWS_BEARER_TOKEN_BEDROCK");
    assertThat(ec.removed).doesNotContain("CLAUDE_CONFIG_DIR");
    // ANTHROPIC_MODEL is declared in settings/ide.properties (team-shared config) and must be preserved
    assertThat(ec.removed).doesNotContain("ANTHROPIC_MODEL");
  }

  @Test
  void testSetEnvironmentScrubsAmbientSystemValueButKeepsIdePropertiesValue() {

    // arrange
    IdeTestContext context = newContext(PROJECT_CLAUDE, null, false);
    // ANTHROPIC_MODEL is exported by settings/ide.properties, but a leaked value is also present in the ambient system environment
    context.getSystem().setEnv("ANTHROPIC_MODEL", "leaked-ambient-model");
    context.getSystem().setEnv("ANTHROPIC_API_KEY", "leaked-ambient-key");
    Claude claude = new Claude(context);
    Path dummy = context.getSoftwarePath().resolve("claude");
    ToolInstallation installation = new ToolInstallation(dummy, dummy, dummy, VersionIdentifier.of(CLAUDE_VERSION), false);
    RecordingEnvironmentContext ec = new RecordingEnvironmentContext();

    // act
    claude.setEnvironment(ec, installation, false);

    // assert
    // ANTHROPIC_MODEL is defined in ide.properties, so the intentional value wins and it is not scrubbed
    assertThat(ec.removed).doesNotContain("ANTHROPIC_MODEL");
    // ANTHROPIC_API_KEY only comes from the ambient system environment, so it is scrubbed
    assertThat(ec.removed).contains("ANTHROPIC_API_KEY");
  }

  @Test
  void testInstallSeedsSettingsSkeletonWhenAbsent(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_CLAUDE, wireMockRuntimeInfo);
    Claude claude = new Claude(context);

    // act
    claude.install();

    // assert
    Path settings = context.getConfPath().resolve("claude/settings.json");
    Path readme = context.getConfPath().resolve("claude/README.md");
    assertThat(settings).exists().content().contains("\"env\"");
    assertThat(readme).exists();
  }

  @Test
  void testInstallDoesNotOverwriteExistingSettings(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {

    // arrange
    IdeTestContext context = newContext(PROJECT_CLAUDE, wireMockRuntimeInfo);
    Path settings = context.getConfPath().resolve("claude/settings.json");
    Files.createDirectories(settings.getParent());
    String userContent = "{\n  \"env\": { \"CLAUDE_CODE_USE_BEDROCK\": \"1\" }\n}\n";
    Files.writeString(settings, userContent);
    Claude claude = new Claude(context);

    // act
    claude.install();

    // assert
    assertThat(settings).content().isEqualTo(userContent);
  }

  @Test
  void testRunExportsIsolatedConfigDirToProcess(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_CLAUDE, wireMockRuntimeInfo);
    Claude claude = new Claude(context);
    claude.arguments.addValue("hello");
    claude.arguments.addValue("world");

    // act
    claude.run();

    // assert
    String expectedConfigDir = context.getConfPath().resolve("claude").toString();
    assertThat(context).logAtInfo().hasMessage("claude hello world");
    assertThat(context).logAtInfo().hasMessageContaining("CLAUDE_CONFIG_DIR=" + expectedConfigDir);
  }

  private void assertInstalled(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("claude/.ide.software.version")).exists().hasContent(CLAUDE_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed claude in version " + CLAUDE_VERSION);
  }
}

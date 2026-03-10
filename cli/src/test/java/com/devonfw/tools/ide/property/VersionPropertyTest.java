package com.devonfw.tools.ide.property;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.HelpCommandlet;
import com.devonfw.tools.ide.commandlet.InstallCommandlet;
import com.devonfw.tools.ide.completion.CompletionCandidate;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.completion.CompletionCandidateCollectorDefault;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link VersionProperty}.
 */
class VersionPropertyTest {

  @Test
  void testParse() {
    IdeContext context = new IdeTestContext();
    VersionProperty versionProp = new VersionProperty("", false, "");

    assertThat(versionProp.parse("1", context)).isEqualTo(VersionIdentifier.of("1"));
    assertThat(versionProp.parse("4.2-beta", context)).isEqualTo(VersionIdentifier.of("4.2-beta"));
  }

  /**
   * Test of {@link VersionProperty#completeValue(String, IdeContext, Commandlet, CompletionCandidateCollector)}. When a commandlet that does not handle
   * versions is provided as argument, we except on versions as candidates to be returned.
   */
  @Test
  void testCompleteValueUnfitCommandlet() {
    IdeContext context = new IdeTestContext();
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);
    VersionProperty versionProp = new VersionProperty("", false, "");

    // no version completion for context commandlet
    versionProp.completeValue("", context, new HelpCommandlet(context), collector);
    assertThat(collector.getCandidates().stream().map(CompletionCandidate::text)).isEmpty();
  }

  /**
   * Test of {@link VersionProperty#completeValue(String, IdeContext, Commandlet, CompletionCandidateCollector)}. When a pattern is provided as argument to be
   * completed, we expect this argument to be kept as is, and given as the sole candidate.
   */
  @Test
  void testCompleteValuePatternGiven() {
    IdeContext context = new IdeTestContext();
    String anyVersion = "*";
    String anyVersionAfter2 = "2.*";
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);
    VersionProperty versionProp = new VersionProperty("", false, "");
    InstallCommandlet installCmd = new InstallCommandlet(context);
    installCmd.tool.setValueAsString("mvn", context);

    versionProp.completeValue(anyVersion, context, installCmd, collector);
    assertThat(collector.getCandidates().stream().map(CompletionCandidate::text)).containsExactly(anyVersion);

    collector = new CompletionCandidateCollectorDefault(context);
    versionProp.completeValue(anyVersionAfter2, context, installCmd, collector);
    assertThat(collector.getCandidates().stream().map(CompletionCandidate::text)).containsExactly(anyVersionAfter2);
  }
}

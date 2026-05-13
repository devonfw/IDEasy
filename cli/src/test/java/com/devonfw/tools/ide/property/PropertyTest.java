package com.devonfw.tools.ide.property;

import static org.assertj.core.api.Assertions.assertThat;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.ContextCommandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.completion.CompletionCandidateCollectorDefault;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for class `Property`.
 */
public class PropertyTest {
  @Test
  public void testApplyAddsPropertyAsCompletionCandidate() {
    IdeContext context = new IdeTestContext();
    CliArguments args = CliArguments.ofCompletion("--for");

    Commandlet ctxCmd = new ContextCommandlet();
    Property<Boolean> flagProperty = new FlagProperty("--force");
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(
      context
    );

    args.next();

    flagProperty.apply("--force", args, context, ctxCmd, collector);

    assertThat(collector.getSortedCandidates().getFirst().text()).isEqualTo("--force");
  }

  @Test
  public void testApplyWithCliArgumentAddsPropertyAsCompletionCandidate() {
    IdeContext context = new IdeTestContext();
    CliArguments args = CliArguments.ofCompletion("mv");

    Commandlet mvnCmd = context.getCommandletManager().getCommandletByFirstKeyword("mvn");
    Property<Boolean> keywordProperty = new KeywordProperty("mvn", false, "");
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(
      context
    );

    args.next();

    keywordProperty.apply(args, context, mvnCmd, collector);

    assertThat(collector.getSortedCandidates().getFirst().text()).isEqualTo("mvn");
  }

  @Test
  public void testMismatchingApplyReturnsFalse() {
    IdeContext context = new IdeTestContext();
    CliArguments args = CliArguments.ofCompletion("mv");

    Commandlet mvnCmd = context.getCommandletManager().getCommandletByFirstKeyword("mvn");
    Property<Boolean> flagProperty = new FlagProperty("--force");
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(
      context
    );

    args.next();

    assertThat(flagProperty.apply(args, context, mvnCmd, collector)).isFalse();
  }

  @Test
  public void testMismatchingApplyOnKeywordReturnsFalse() {
    IdeContext context = new IdeTestContext();
    CliArguments args = CliArguments.ofCompletion("st");

    Commandlet mvnCmd = context.getCommandletManager().getCommandletByFirstKeyword("mvn");
    Property<Boolean> keywordProperty = new KeywordProperty("mvn", false, "");
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(
      context
    );

    args.next();

    assertThat(keywordProperty.apply(args, context, mvnCmd, collector)).isFalse();
  }

  @Test
  public void testCompleteAddsToCandidatesOnMatch() {
    IdeContext context = new IdeTestContext();
    CliArguments args = CliArguments.ofCompletion("--for");

    Commandlet ctxCmd = context.getCommandletManager().getCommandletByFirstKeyword("context");
    Property<Boolean> flagProperty = new FlagProperty("--force");
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(
      context
    );

    args.next();

    flagProperty.complete("--force", args.current(), args, context, ctxCmd, collector);

    assertThat(collector.getSortedCandidates().getFirst().text()).isEqualTo("--force");
  }

  @Test
  public void testCompleteAddsCandidateForNonOption() {
    IdeContext context = new IdeTestContext();
    CliArguments args = CliArguments.ofCompletion("mv");

    Commandlet ctxCmd = context.getCommandletManager().getCommandletByFirstKeyword("mvn");
    Property<Boolean> keywordProperty = new KeywordProperty("mvn", false, "");
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(
      context
    );

    args.next();

    keywordProperty.complete("mvn", args.current(), args, context, ctxCmd, collector);

    assertThat(collector.getSortedCandidates().getFirst().text()).isEqualTo("mvn");
  }
}

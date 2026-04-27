package com.devonfw.tools.ide.commandlet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.completion.CompletionCandidate;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.completion.CompletionCandidateCollectorDefault;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import org.junit.jupiter.api.Test;

public class CommandletManagerTest {
  @Test
  public void testMvnCommandletIsPartOfCommandletIterator() {
    IdeTestContext context = new IdeTestContext();
    CommandletManager manager = context.getCommandletManager();
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(
      context
    );

    assertThat(manager.findCommandlet(new CliArguments("mvn"), collector).hasNext()).isTrue();
  }

  @Test
  public void testCollectCompletionCandidatesEmptyInputReturnsAllCommandlets() {
    IdeTestContext context = new IdeTestContext();
    CommandletManager manager = context.getCommandletManager();
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);

    manager.collectCompletionCandidates(CliArguments.ofCompletion(""), collector);

    List<String> lines = collector.getSortedCandidates().stream()
                         .map(CompletionCandidate::text).toList();
    assertThat(lines).contains("complete", "create", "env", "install", "shell", "status", "update",
                               "mvn");
  }

  @Test
  public void testCollectCompletionCandidatesPartialInputFiltersToMatches() {
    IdeTestContext context = new IdeTestContext();
    CommandletManager manager = context.getCommandletManager();
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);

    manager.collectCompletionCandidates(CliArguments.ofCompletion("ins"), collector);

    List<String> lines = collector.getSortedCandidates().stream()
                         .map(CompletionCandidate::text).toList();
    assertThat(lines).containsExactly("install");
  }

  @Test
  public void testCollectCompletionCandidatesNoMatchReturnsEmpty() {
    IdeTestContext context = new IdeTestContext();
    CommandletManager manager = context.getCommandletManager();
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);

    manager.collectCompletionCandidates(CliArguments.ofCompletion("xyz"), collector);

    assertThat(collector.getSortedCandidates()).isEmpty();
  }
}

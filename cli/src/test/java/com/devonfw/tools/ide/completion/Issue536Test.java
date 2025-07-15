package com.devonfw.tools.ide.completion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;

/**
 * Test for the specific scenario mentioned in issue #536 - "ide complete in"
 */
public class Issue536Test extends AbstractIdeContextTest {

  /** 
   * Test the specific scenario from issue #536: "Debug through `ide complete in`"
   * This test verifies that completion for "in" does not produce duplicates.
   */
  @Test
  public void testIdeCompleteInNoDuplicates() {
    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    CliArguments args = CliArguments.ofCompletion("in");
    
    // act
    List<CompletionCandidate> candidates = context.complete(args, true);
    
    // assert - verify no duplicates
    long totalCandidates = candidates.size();
    long uniqueCandidates = candidates.stream().map(CompletionCandidate::text).distinct().count();
    
    assertThat(totalCandidates).as("Should not have duplicate completion candidates").isEqualTo(uniqueCandidates);
    
    // Additional verification - check that expected candidates are present
    List<String> candidateTexts = candidates.stream().map(CompletionCandidate::text).toList();
    assertThat(candidateTexts).contains("install", "install-plugin", "intellij");
  }
  
  /**
   * Test completion for empty string to ensure context options are included without duplicates
   */
  @Test
  public void testCompleteEmptyWithContextOptions() {
    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    CliArguments args = CliArguments.ofCompletion("");
    args.next(); // move to first argument
    
    // act
    List<CompletionCandidate> candidates = context.complete(args, true);
    
    // assert - verify no duplicates
    long totalCandidates = candidates.size();
    long uniqueCandidates = candidates.stream().map(CompletionCandidate::text).distinct().count();
    
    assertThat(totalCandidates).as("Should not have duplicate completion candidates").isEqualTo(uniqueCandidates);
    
    // Should include both context options and commandlets
    assertThat(candidates).isNotEmpty();
  }
}
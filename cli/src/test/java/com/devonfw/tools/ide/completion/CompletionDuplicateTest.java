package com.devonfw.tools.ide.completion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;

/**
 * Test to verify that completion candidates are not duplicated. This test verifies the fix for issue <a href="https://github.com/devonfw/IDEasy/issues/536">#536</a> where
 * commandlets were matched twice during completion.
 */
public class CompletionDuplicateTest extends AbstractIdeContextTest {

  private static final String PROJECT_COMPLETION = "completion";

  /**
   * Test to verify that completion candidates are not duplicated for various input scenarios.
   */
  @Test
  public void testCompletionNoDuplicates() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_COMPLETION, null, false);

    // Test various completion scenarios that could potentially produce duplicates
    String[] testInputs = { "i", "in", "install", "h", "help", "j", "java" };

    for (String input : testInputs) {
      // act
      CliArguments args = CliArguments.ofCompletion(input);
      List<CompletionCandidate> candidates = context.complete(args, false);

      // assert
      Map<String, Integer> textCounts = new HashMap<>();
      for (CompletionCandidate candidate : candidates) {
        String text = candidate.text();
        textCounts.put(text, textCounts.getOrDefault(text, 0) + 1);
      }

      // Check for duplicates
      long uniqueCount = candidates.stream().map(CompletionCandidate::text).distinct().count();

      assertThat(candidates.size()).as("Input '%s' should not have duplicate candidates", input)
          .isEqualTo(uniqueCount);
    }
  }

  /**
   * Test the specific scenario from issue <a href="https://github.com/devonfw/IDEasy/issues/536">#536</a>: completion for "in" should not produce duplicates and should contain
   * expected candidates.
   */
  @Test
  public void testCompletionForInPrefix() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_COMPLETION, null, false);
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
   * Test completion for empty string to ensure context options are included without duplicates.
   */
  @Test
  public void testCompletionEmptyInput() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_COMPLETION, null, false);
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
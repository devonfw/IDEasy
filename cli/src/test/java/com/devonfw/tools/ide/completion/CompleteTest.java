package com.devonfw.tools.ide.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.ContextCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.property.KeywordProperty;
import com.devonfw.tools.ide.property.Property;

/**
 * Test of {@link AbstractIdeContext#complete(CliArguments, boolean) auto-completion}.
 */
public class CompleteTest extends AbstractIdeContextTest {

  private static final String PROJECT_COMPLETION = "completion";

  /** Test of {@link AbstractIdeContext#complete(CliArguments, boolean) auto-completion} for empty input. */
  @Test
  public void testCompleteEmpty() {

    // arrange
    boolean includeContextOptions = true;
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    CliArguments args = CliArguments.ofCompletion("");
    args.next();
    List<String> expectedCandidates = getExpectedCandidates(context, true, includeContextOptions, true);
    // act
    List<CompletionCandidate> candidates = context.complete(args, includeContextOptions);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text))
        .containsExactly(expectedCandidates.toArray(String[]::new));
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, boolean) auto-completion} for long option. */
  @Test
  public void testCompleteLongOptionBatch() {

    // arrange
    boolean includeContextOptions = true;
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    CliArguments args = CliArguments.ofCompletion("--b");
    args.next();
    List<String> expectedCandidates = List.of("--batch");
    // act
    List<CompletionCandidate> candidates = context.complete(args, includeContextOptions);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text))
        .containsExactly(expectedCandidates.toArray(String[]::new));
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, boolean) auto-completion} for empty input. */
  @Test
  public void testCompleteEmptyNoCtxOptions() {

    // arrange
    boolean includeContextOptions = false;
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    CliArguments args = CliArguments.ofCompletion("");
    args.next();
    List<String> expectedCandidates = getExpectedCandidates(context, true, includeContextOptions, true);
    // act
    List<CompletionCandidate> candidates = context.complete(args, includeContextOptions);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text))
        .containsExactly(expectedCandidates.toArray(String[]::new));
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, boolean) auto-completion} for input "h". */
  @Test
  public void testCompleteCommandletFirstLetter() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    CliArguments args = CliArguments.ofCompletion("h");
    // act
    List<CompletionCandidate> candidates = context.complete(args, true);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text)).containsExactly("helm", "help");
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, boolean) auto-completion} for input "-f". */
  @Test
  public void testCompleteShortOptsCombined() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    CliArguments args = CliArguments.ofCompletion("-f");
    // act
    List<CompletionCandidate> candidates = context.complete(args, true);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text)).containsExactly("-f", "-fb", "-fd", "-fh", "-fo", "-fp", "-fq",
        "-ft", "-fv");
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, boolean) auto-completion} for input "-fbdoqt". */
  @Test
  public void testCompleteShortOptsCombinedAllButVersion() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    CliArguments args = CliArguments.ofCompletion("-fbdopqt");
    // act
    List<CompletionCandidate> candidates = context.complete(args, true);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text)).containsExactly("-fbdopqt", "-fbdopqth", "-fbdopqtv");
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, boolean) auto-completion} for input "help", "". */
  @Test
  public void testCompleteHelpEmptyArgs() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    CliArguments args = CliArguments.ofCompletion("help", "");
    List<String> expectedCandidates = getExpectedCandidates(context, true, false, false);
    // act
    List<CompletionCandidate> candidates = context.complete(args, true);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text))
        .containsExactly(expectedCandidates.toArray(String[]::new));
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, boolean) auto-completion} for input "help", "". */
  @Test
  public void testCompleteVersionNoMoreArgs() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    CliArguments args = CliArguments.ofCompletion("--version", "");
    // act
    List<CompletionCandidate> candidates = context.complete(args, true);
    // assert
    assertThat(candidates).isEmpty();
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, boolean) auto-completion} for an option inside a commandlet. */
  @Test
  public void testCompleteCommandletOption() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    CliArguments args = CliArguments.ofCompletion("get-version", "--c");
    // act
    List<CompletionCandidate> candidates = context.complete(args, true);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text)).containsExactly("--configured");
  }

  private static List<String> getExpectedCandidates(AbstractIdeContext context, boolean commandlets,
      boolean ctxOptions, boolean addAlias) {

    List<String> expectedCandidates = new ArrayList<>();
    if (ctxOptions) {
      ContextCommandlet ctxCmd = new ContextCommandlet();
      for (Property<?> p : ctxCmd.getProperties()) {
        expectedCandidates.add(p.getName());
        String alias = p.getAlias();
        if (alias != null) {
          expectedCandidates.add(alias);
        }
      }
    }
    if (commandlets) {
      for (Commandlet cmd : context.getCommandletManager().getCommandlets()) {
        expectedCandidates.add(cmd.getName());
        if (addAlias) {
          Property<?> firstProperty = cmd.getValues().get(0);
          assert (firstProperty instanceof KeywordProperty);
          String alias = firstProperty.getAlias();
          if (alias != null) {
            expectedCandidates.add(alias);
          }
        }
      }
    }
    Collections.sort(expectedCandidates);
    return expectedCandidates;
  }

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

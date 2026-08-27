package com.devonfw.tools.ide.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.ContextCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.property.KeywordProperty;
import com.devonfw.tools.ide.property.Property;

/**
 * Test of {@link AbstractIdeContext#complete(CliArguments, CompletionCandidateCollector, boolean) auto-completion}.
 */
class CompleteTest extends AbstractIdeContextTest {

  private static final String PROJECT_COMPLETION = "completion";

  private CompletionCandidateCollector createCollector(AbstractIdeContext context, String[] args) {
    Set<String> alreadyProvided = new HashSet<>();
    for (int i = 0; i < args.length - 1; i++) {
      alreadyProvided.add(args[i]);
    }
    return new CompletionCandidateCollectorDefault(context, alreadyProvided);
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, CompletionCandidateCollector, boolean) auto-completion} for empty input. */
  @Test
  void testCompleteEmpty() {

    // arrange
    boolean includeContextOptions = true;
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    String[] argsArray = { "" };
    CliArguments args = CliArguments.ofCompletion(argsArray);
    args.next();
    List<String> expectedCandidates = getExpectedCandidates(context, true, includeContextOptions, true);
    // act
    List<CompletionCandidate> candidates = context.complete(args, createCollector(context, argsArray), includeContextOptions);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text))
        .containsExactly(expectedCandidates.toArray(String[]::new));
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, CompletionCandidateCollector, boolean) auto-completion} for long option. */
  @Test
  void testCompleteLongOptionBatch() {

    // arrange
    boolean includeContextOptions = true;
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    String[] argsArray = { "--b" };
    CliArguments args = CliArguments.ofCompletion(argsArray);
    args.next();
    List<String> expectedCandidates = List.of("--batch");
    // act
    List<CompletionCandidate> candidates = context.complete(args, createCollector(context, argsArray), includeContextOptions);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text))
        .containsExactly(expectedCandidates.toArray(String[]::new));
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, CompletionCandidateCollector, boolean) auto-completion} for empty input. */
  @Test
  void testCompleteEmptyNoCtxOptions() {

    // arrange
    boolean includeContextOptions = false;
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    String[] argsArray = { "" };
    CliArguments args = CliArguments.ofCompletion(argsArray);
    args.next();
    List<String> expectedCandidates = getExpectedCandidates(context, true, includeContextOptions, true);
    // act
    List<CompletionCandidate> candidates = context.complete(args, createCollector(context, argsArray), includeContextOptions);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text))
        .containsExactly(expectedCandidates.toArray(String[]::new));
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, CompletionCandidateCollector, boolean) auto-completion} for input "h". */
  @Test
  void testCompleteCommandletFirstLetter() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    String[] argsArray = { "h" };
    CliArguments args = CliArguments.ofCompletion(argsArray);
    // act
    List<CompletionCandidate> candidates = context.complete(args, createCollector(context, argsArray), true);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text)).containsExactly("helm", "help");
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, CompletionCandidateCollector, boolean) auto-completion} for input "-f". */
  @Test
  void testCompleteShortOptsCombined() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    String[] argsArray = { "-f" };
    CliArguments args = CliArguments.ofCompletion(argsArray);
    // act
    List<CompletionCandidate> candidates = context.complete(args, createCollector(context, argsArray), true);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text)).containsExactly("-f", "-fb", "-fd", "-fh", "-fo", "-fp", "-fq",
        "-ft", "-fv");
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, CompletionCandidateCollector, boolean) auto-completion} for input "-fbdoqt". */
  @Test
  void testCompleteShortOptsCombinedAllButVersion() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    String[] argsArray = { "-fbdopqt" };
    CliArguments args = CliArguments.ofCompletion(argsArray);
    // act
    List<CompletionCandidate> candidates = context.complete(args, createCollector(context, argsArray), true);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text)).containsExactly("-fbdopqt", "-fbdopqth", "-fbdopqtv");
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, CompletionCandidateCollector, boolean) auto-completion} for input "help", "". */
  @Test
  void testCompleteHelpEmptyArgs() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    String[] argsArray = { "help", "" };
    CliArguments args = CliArguments.ofCompletion(argsArray);
    List<String> expectedCandidates = getExpectedCandidates(context, true, false, false);
    // act
    List<CompletionCandidate> candidates = context.complete(args, createCollector(context, argsArray), true);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text))
        .containsExactly(expectedCandidates.toArray(String[]::new));
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, CompletionCandidateCollector, boolean) auto-completion} for input "help", "". */
  @Test
  void testCompleteVersionNoMoreArgs() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    String[] argsArray = { "--version", "" };
    CliArguments args = CliArguments.ofCompletion(argsArray);
    // act
    List<CompletionCandidate> candidates = context.complete(args, createCollector(context, argsArray), true);
    // assert
    assertThat(candidates).isEmpty();
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, CompletionCandidateCollector, boolean) auto-completion} for an option inside a commandlet. */
  @Test
  void testCompleteCommandletOption() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    String[] argsArray = { "get-version", "--c" };
    CliArguments args = CliArguments.ofCompletion(argsArray);
    // act
    List<CompletionCandidate> candidates = context.complete(args, createCollector(context, argsArray), true);
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
  void testCompletionNoDuplicates() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_COMPLETION, null, false);

    // Test various completion scenarios that could potentially produce duplicates
    String[] testInputs = { "i", "in", "install", "h", "help", "j", "java" };

    for (String input : testInputs) {
      // act
      CliArguments args = CliArguments.ofCompletion(input);
      List<CompletionCandidate> candidates = context.complete(args, createCollector(context, new String[] { input }), false);

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
   * Test the specific scenario from issue <a href="https://github.com/devonfw/IDEasy/issues/536">#536</a>: completion for "in" should not produce duplicates
   * and should contain expected candidates.
   */
  @Test
  void testCompletionForInPrefix() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_COMPLETION, null, false);
    String[] argsArray = { "in" };
    CliArguments args = CliArguments.ofCompletion(argsArray);

    // act
    List<CompletionCandidate> candidates = context.complete(args, createCollector(context, argsArray), true);

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
  void testCompletionEmptyInput() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_COMPLETION, null, false);
    String[] argsArray = { "" };
    CliArguments args = CliArguments.ofCompletion(argsArray);
    args.next(); // move to first argument

    // act
    List<CompletionCandidate> candidates = context.complete(args, createCollector(context, argsArray), true);

    // assert - verify no duplicates
    long totalCandidates = candidates.size();
    long uniqueCandidates = candidates.stream().map(CompletionCandidate::text).distinct().count();

    assertThat(totalCandidates).as("Should not have duplicate completion candidates").isEqualTo(uniqueCandidates);

    // Should include both context options and commandlets
    assertThat(candidates).isNotEmpty();
  }

  /**
   * Test of tool argument auto-completion for Maven.
   */
  @Test
  void testCompleteMavenToolArguments() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    String[] argsArray = { "mvn", "dep" };
    CliArguments args = CliArguments.ofCompletion(argsArray);

    // act
    List<CompletionCandidate> candidates = context.complete(args, createCollector(context, argsArray), true);

    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text))
        .contains("dependency:list", "dependency:tree", "deploy");
  }

  /**
   * Test of Maven tool argument auto-completion for empty input.
   */
  @Test
  void testCompleteMavenToolArgumentsForEmptyInput() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    String[] argsArray = { "mvn", "" };
    CliArguments args = CliArguments.ofCompletion("mvn", "");

    // act
    List<CompletionCandidate> candidates = context.complete(args, createCollector(context, argsArray), true);

    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text))
        .contains("clean", "package", "install", "dependency:list", "dependency:tree", "-DskipTests");
  }

  /**
   * Test of tool argument auto-completion for Maven Daemon.
   */
  @Test
  void testCompleteMavenDaemonToolArguments() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    String[] argsArray = { "mvnd", "-Dmvnd.c" };
    CliArguments args = CliArguments.ofCompletion(argsArray);

    // act
    List<CompletionCandidate> candidates = context.complete(args, createCollector(context, argsArray), true);

    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text))
        .contains("-Dmvnd.cancelConnectTimeout=", "-Dmvnd.connectTimeout=", "-Dmvnd.coreExtensionsExclude=");
  }

  /**
   * Test of Maven Daemon tool argument auto-completion for empty input.
   */
  @Test
  void testCompleteMavenDaemonToolArgumentsForEmptyInput() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    String[] argsArray = { "mvnd", "" };
    CliArguments args = CliArguments.ofCompletion(argsArray);

    // act
    List<CompletionCandidate> candidates = context.complete(args, createCollector(context, argsArray), true);

    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text))
        .contains("--purge", "--settings", "--completion", "dependency:list", "dependency:tree", "-DskipTests");
  }

  /**
   * Test that synonyms and canonical candidates are not suggested when the canonical candidate has already been provided.
   */
  @Test
  void testSynonymFilteringWithProvidedCanonicalCandidate() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    String[] argsArray = { "mvn", "-s", "" };
    CliArguments args = CliArguments.ofCompletion(argsArray);
    CompletionCandidateCollector collector = createCollector(context, argsArray);

    // act
    List<CompletionCandidate> candidates = context.complete(args, collector, true);

    // assert
    List<String> texts = candidates.stream().map(CompletionCandidate::text).toList();
    assertThat(texts).doesNotContain("-s");
    assertThat(texts).doesNotContain("--settings");
  }

  /**
   * Test that synonyms and canonical candidates are not suggested when the synonym has already been provided.
   */
  @Test
  void testSynonymFilteringWithProvidedSynonym() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    String[] argsArray = { "mvn", "--settings", "" };
    CliArguments args = CliArguments.ofCompletion(argsArray);
    CompletionCandidateCollector collector = createCollector(context, argsArray);

    // act
    List<CompletionCandidate> candidates = context.complete(args, collector, true);

    // assert
    List<String> texts = candidates.stream().map(CompletionCandidate::text).toList();
    assertThat(texts).doesNotContain("-s");
    assertThat(texts).doesNotContain("--settings");
  }

  /**
   * Test that completion works for a second tool argument (e.g. "ide mvn clean [tab]"), which is the real-world scenario that previously failed because the
   * multivalued arguments property consumed the completion marker greedily.
   */
  @Test
  void testCompleteMavenSecondToolArgument() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    String[] argsArray = { "mvn", "clean", "dep" };
    CliArguments args = CliArguments.ofCompletion(argsArray);

    // act
    List<CompletionCandidate> candidates = context.complete(args, createCollector(context, argsArray), true);

    // assert - should complete the second argument after 'clean'
    assertThat(candidates.stream().map(CompletionCandidate::text))
        .contains("dependency:list", "dependency:tree", "deploy");
  }

  @Test
  void testAlternativeFilteringWhenOtherAlternativeProvidedJava() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    String[] argsArray = { "mvn", "exec:java", "" };
    CliArguments args = CliArguments.ofCompletion(argsArray);
    CompletionCandidateCollector collector = createCollector(context, argsArray);

    // act
    List<CompletionCandidate> candidates = context.complete(args, collector, true);

    // assert
    List<String> texts = candidates.stream().map(CompletionCandidate::text).toList();
    assertThat(texts).doesNotContain("exec:exec");
  }

  @Test
  void testAlternativeFilteringWhenOtherAlternativeProvidedExec() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    String[] argsArray = { "mvn", "exec:exec", "" };
    CliArguments args = CliArguments.ofCompletion(argsArray);
    CompletionCandidateCollector collector = createCollector(context, argsArray);

    // act
    List<CompletionCandidate> candidates = context.complete(args, collector, true);

    // assert
    List<String> texts = candidates.stream().map(CompletionCandidate::text).toList();
    assertThat(texts).doesNotContain("exec:java");
  }
}

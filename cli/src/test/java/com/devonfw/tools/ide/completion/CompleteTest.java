package com.devonfw.tools.ide.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    assertThat(candidates.stream().map(CompletionCandidate::text)).containsExactly("-f", "-fb", "-fd", "-fh", "-fo", "-fq",
        "-ft", "-fv");
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, boolean) auto-completion} for input "-fbdoqt". */
  @Test
  public void testCompleteShortOptsCombinedAllButVersion() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    CliArguments args = CliArguments.ofCompletion("-fbdoqt");
    // act
    List<CompletionCandidate> candidates = context.complete(args, true);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text)).containsExactly("-fbdoqt", "-fbdoqth", "-fbdoqtv");
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
}

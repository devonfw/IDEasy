package com.devonfw.tools.ide.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.ContextCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContextTest;
import com.devonfw.tools.ide.property.Property;

/**
 * Test of {@link AbstractIdeContext#complete(CliArguments, boolean) auto-completion}.
 */
public class CompleteTest extends IdeContextTest {

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
    assertThat(candidates.stream().map(CompletionCandidate::text)).containsExactly("-f", "-fb", "-fd", "-fo", "-fq", "-fs",
        "-ft", "-fv");
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, boolean) auto-completion} for input "-fbdoqt". */
  @Test
  public void testCompleteShortOptsCombinedAllButVersion() {

    // arrange
    AbstractIdeContext context = newContext(PROJECT_BASIC, null, false);
    CliArguments args = CliArguments.ofCompletion("-fbdoqts");
    // act
    List<CompletionCandidate> candidates = context.complete(args, true);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text)).containsExactly("-fbdoqts", "-fbdoqtsv");
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

  private static List<String> getExpectedCandidates(AbstractIdeContext context, boolean commandlets,
      boolean ctxOptions, boolean addVersionAlias) {

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
      }
      if (addVersionAlias) {
        expectedCandidates.add("-v"); // alias for VersionCommandlet (--version)
      }
    }
    Collections.sort(expectedCandidates);
    return expectedCandidates;
  }
}

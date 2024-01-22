package com.devonfw.tools.ide.completion;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.ContextCommandlet;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.property.Property;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test of {@link AbstractIdeContext#complete(CliArguments, boolean) auto-completion}.
 */
public class CompleteTest extends Assertions {

  /** Test of {@link AbstractIdeContext#complete(CliArguments, boolean) auto-completion} for empty input. */
  @Test
  public void testCompleteEmpty() {

    // arrange
    AbstractIdeContext context = IdeTestContextMock.get();
    CliArguments args = CliArguments.ofCompletion("");
    args.next();
    List<String> expectedCandidates = getExpectedCandidates(context);
    // act
    List<CompletionCandidate> candidates = context.complete(args, true);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text))
        .containsExactly(expectedCandidates.toArray(String[]::new));
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, boolean) auto-completion} for input "h". */
  @Test
  public void testCompleteCommandletFirstLetter() {

    // arrange
    AbstractIdeContext context = IdeTestContextMock.get();
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
    AbstractIdeContext context = IdeTestContextMock.get();
    CliArguments args = CliArguments.ofCompletion("-f");
    // act
    List<CompletionCandidate> candidates = context.complete(args, true);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text)).containsExactly("-f", "-fb", "-fd", "-fo", "-fq", "-ft", "-fv");
  }

  /** Test of {@link AbstractIdeContext#complete(CliArguments, boolean) auto-completion} for input "-fbdoqt". */
  @Test
  public void testCompleteShortOptsCombinedAllButVersion() {

    // arrange
    AbstractIdeContext context = IdeTestContextMock.get();
    CliArguments args = CliArguments.ofCompletion("-fbdoqt");
    // act
    List<CompletionCandidate> candidates = context.complete(args, true);
    // assert
    assertThat(candidates.stream().map(CompletionCandidate::text)).containsExactly("-fbdoqt", "-fbdoqtv");
  }

  private static List<String> getExpectedCandidates(AbstractIdeContext context) {

    List<String> expectedCandidates = new ArrayList<>();
    ContextCommandlet ctxCmd = new ContextCommandlet();
    for (Property<?> p : ctxCmd.getProperties()) {
      expectedCandidates.add(p.getName());
      String alias = p.getAlias();
      if (alias != null) {
        expectedCandidates.add(alias);
      }
    }
    for (Commandlet cmd : context.getCommandletManager().getCommandlets()) {
      expectedCandidates.add(cmd.getName());
    }
    expectedCandidates.add("-v"); // alias for VersionCommandlet (--version)
    Collections.sort(expectedCandidates);
    return expectedCandidates;
  }
}

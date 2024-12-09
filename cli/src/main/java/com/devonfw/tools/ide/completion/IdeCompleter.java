package com.devonfw.tools.ide.completion;

import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.context.AbstractIdeContext;

/**
 * Implements the {@link Completer} for jline3 autocompletion. Inspired by picocli
 */
public class IdeCompleter implements Completer {

  private final AbstractIdeContext context;

  /**
   * The constructor.
   *
   * @param context the {@link AbstractIdeContext}.
   */
  public IdeCompleter(AbstractIdeContext context) {

    super();
    this.context = context;
  }

  @Override
  public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
    List<String> words = commandLine.words();
    CliArguments args = CliArguments.ofCompletion(words.toArray(String[]::new));
    List<CompletionCandidate> completion = this.context.complete(args, true);
    int i = 0;
    for (CompletionCandidate candidate : completion) {
      candidates.add(new Candidate(candidate.text(), candidate.text(), null, null, null, null, true, i++));
    }
  }

}

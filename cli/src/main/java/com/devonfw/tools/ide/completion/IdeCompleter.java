package com.devonfw.tools.ide.completion;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

  private static final String EXIT_COMMAND = "exit";

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

    String currentWord = commandLine.word();
    int wordIndex = commandLine.wordIndex();

    if (wordIndex == 0 && !currentWord.isEmpty() && EXIT_COMMAND.startsWith(currentWord)) {
      candidates.add(new Candidate(EXIT_COMMAND));
    }

    List<String> words = commandLine.words();
    String[] argsArray = words.toArray(String[]::new);
    CliArguments args = CliArguments.ofCompletion(argsArray);
    Set<String> alreadyProvided = new HashSet<>();
    for (int i = 0; i < argsArray.length - 1; i++) {
      alreadyProvided.add(argsArray[i]);
    }
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(this.context, alreadyProvided);
    List<CompletionCandidate> completion = this.context.complete(args, collector, true);
    int i = 0;
    for (CompletionCandidate candidate : completion) {

      // candidates ending with "=" avoid appending a whitespace
      boolean complete = !candidate.text().endsWith("=");
      candidates.add(new Candidate(candidate.text(), candidate.text(), null, null, null, null, complete, i++));
    }
  }

}

package com.devonfw.tools.ide.completion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.completion.CompletionCandidate;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.utils.AttributedString;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.HelpCommandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.property.VersionProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

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
    List<CompletionCandidate> completion = this.context.complete(args, false);
    for (CompletionCandidate candidate : completion) {
      candidates.add(new Candidate(candidate.text(), candidate.text(), null, null, null, null, true));
    }
  }

}

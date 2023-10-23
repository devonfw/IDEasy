package com.devonfw.tools.ide.cli;

import java.util.Arrays;
import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.utils.AttributedString;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.ContextCommandlet;
import com.devonfw.tools.ide.context.IdeContext;

/**
 * Implements the {@link Completer} for jline3 autocompletion.
 */
public class IdeCompleter extends ArgumentCompleter implements Completer {

  private final ContextCommandlet cmd;

  private final IdeContext context;

  public IdeCompleter(ContextCommandlet cmd, IdeContext context) {

    super(NullCompleter.INSTANCE);
    this.cmd = cmd;
    this.context = context;
  }

  @Override
  public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {

    assert commandLine != null;
    assert candidates != null;
    String word = commandLine.word();
    List<String> words = commandLine.words();
    // TODO: implement rest of this
    Commandlet sub = findSubcommandlet(words, commandLine.wordIndex());
    addCandidates(candidates, Arrays.asList("install", ""));
    if (sub == null) {
      return;
    }
    // if (word.startsWith("-")) {
    // String buffer = word.substring(0, commandLine.wordCursor());
    // int eq = buffer.indexOf('=');
    // for (OptionSpec option : sub.getCommandSpec().options()) {
    // if (option.hidden()) continue;
    // if (option.arity().max() == 0 && eq < 0) {
    // addCandidates(candidates, Arrays.asList(option.names()));
    // } else {
    // if (eq > 0) {
    // String opt = buffer.substring(0, eq);
    // if (Arrays.asList(option.names()).contains(opt) && option.completionCandidates() != null) {
    // addCandidates(candidates, option.completionCandidates(), buffer.substring(0, eq + 1), "", true);
    // }
    // } else {
    // addCandidates(candidates, Arrays.asList(option.names()), "", "=", false);
    // }
    // }
    // }
    // } else {
    // addCandidates(candidates, sub.getSubcommands().keySet());
    // for (Commandlet s : sub.getSubcommands().values()) {
    // if (!s.getCommandSpec().usageMessage().hidden()) {
    // addCandidates(candidates, Arrays.asList(s.getCommandSpec().aliases()));
    // }
    // }
    // }
  }

  private void addCandidates(List<Candidate> candidates, Iterable<String> cands) {

    addCandidates(candidates, cands, "", "", true);
  }

  private void addCandidates(List<Candidate> candidates, Iterable<String> cands, String preFix, String postFix,
      boolean complete) {

    for (String s : cands) {
      candidates
          .add(new Candidate(AttributedString.stripAnsi(preFix + s + postFix), s, null, null, null, null, complete));
    }
  }

  protected Commandlet findSubcommandlet(List<String> args, int lastIdx) {

    Commandlet out = cmd;
    for (int i = 0; i < lastIdx; i++) {
      if (!args.get(i).startsWith("-")) {
        out = findSubcommandlet(out, args.get(i));
        if (out == null) {
          break;
        }
      }
    }
    return out;
  }

  private Commandlet findSubcommandlet(Commandlet cmdlet, String command) {

    // for (Commandlet s : cmdlet.getSubcommands().values()) {
    // if (s.getCommandName().equals(command) || Arrays.asList(s.getCommandSpec().aliases()).contains(command)) {
    // return s;
    // }
    // }
    return null;
  }
}

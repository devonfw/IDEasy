package com.devonfw.tools.ide.cli;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.property.VersionProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Implements the {@link Completer} for jline3 autocompletion. Inspired by picocli
 */
public class IdeCompleter extends ArgumentCompleter implements Completer {

  private final ContextCommandlet cmd;

  private final IdeContext context;

  private final Set<String> commandlets = new HashSet<>();

  private final Set<String> toolCommandlets = new HashSet<>();

  private final Set<String> commandletOptions = new HashSet<>();

  public IdeCompleter(ContextCommandlet cmd, IdeContext context) {

    super(NullCompleter.INSTANCE);
    this.cmd = cmd;
    this.context = context;

    List<Property<?>> options = cmd.getProperties();

    for (Property option : options) {
      if (option instanceof FlagProperty) {
        commandletOptions.add(option.getName());
        commandletOptions.add(option.getAlias());
      }
    }

    Collection<Commandlet> commandletCollection = context.getCommandletManager().getCommandlets();

    for (Commandlet commandlet : commandletCollection) {
      commandlets.add(commandlet.getName());
      commandlets.add(commandlet.getKeyword());
    }

    for (Commandlet commandlet : commandletCollection) {
      if (commandlet instanceof ToolCommandlet) {
        toolCommandlets.add(commandlet.getName());
        toolCommandlets.add(commandlet.getKeyword());
      }

    }

  }

  @Override
  public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {

    assert commandLine != null;
    assert candidates != null;
    String word = commandLine.word();
    List<String> words = commandLine.words();
    // TODO: implement rest of this
    Commandlet sub = findSubcommandlet(words, commandLine.wordIndex());
    // first layer
    if (words.size() == 1) {
      if (word.startsWith("-")) {
        addCandidates(candidates, this.commandletOptions); // adds all options
      }
      addCandidates(candidates, this.commandlets); // adds all commandlets
    } else if (words.size() == 2) {
      // 2nd layer..
      if (words.get(0).startsWith("-")) { // options
        Set<String> cleanedOptions = new HashSet<>();
        for (String option : this.commandletOptions) {
          // TODO: remove aliases and vice versa too (--trace removes -t too)
          if (!option.equals(words.get(0))) {
            cleanedOptions.add(option);
          }
        }
        addCandidates(candidates, cleanedOptions); // adds rest of options without used option
        addCandidates(candidates, this.commandlets); // adds all commandlets
      } else {
        Commandlet commandlet = context.getCommandletManager().getCommandlet(words.get(0));
        List<Property<?>> properties = commandlet.getProperties();
        for (Property<?> property : properties) {
          if (property instanceof ToolProperty) {
            addCandidates(candidates, this.toolCommandlets);
          }
        }
      }
      // 3rd layer
    } else if (words.size() == 3) {
      Commandlet commandlet = context.getCommandletManager().getCommandlet(words.get(0));
      List<Property<?>> properties = commandlet.getProperties();
      for (Property<?> property : properties) {
        if (property instanceof VersionProperty) {
          Commandlet subCommandlet = context.getCommandletManager().getCommandlet(words.get(1));
          if (subCommandlet != null) {
            String toolEdition = context.getVariables().getToolEdition(subCommandlet.getName());
            List<VersionIdentifier> versions = context.getUrls().getSortedVersions(subCommandlet.getName(),
                toolEdition);
            Set<String> versionNames = new HashSet<>();
            for (VersionIdentifier vi : versions) {
              versionNames.add(vi.toString());
            }
            // TODO: sort versions descending
            addCandidates(candidates, versionNames);
          }

        }
      }
    }
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

    Commandlet commandlet = context.getCommandletManager().getCommandlet(command);
    List<Property<?>> properties = commandlet.getProperties();
    for (Property<?> property : properties) {
      if (property.getValue() instanceof ToolCommandlet) {
        return null;

      }
    }
    // for (Commandlet s : cmdlet.getSubcommands().values()) {
    // if (s.getCommandName().equals(command) || Arrays.asList(s.getCommandSpec().aliases()).contains(command)) {
    // return s;
    // }
    // }
    return null;
  }
}

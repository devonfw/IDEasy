package com.devonfw.tools.ide.cli;

import java.util.ArrayList;
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
import com.devonfw.tools.ide.commandlet.HelpCommandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.LocaleProperty;
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

  private final Set<Property<?>> commandletOptions = new HashSet<>();

  public IdeCompleter(ContextCommandlet cmd, IdeContext context) {

    super(NullCompleter.INSTANCE);
    this.cmd = cmd;
    this.context = context;

    List<Property<?>> options = cmd.getProperties();

    for (Property option : options) {
      if (option instanceof FlagProperty) {
        commandletOptions.add(option);
      }
    }

    Collection<Commandlet> commandletCollection = context.getCommandletManager().getCommandlets();

    for (Commandlet commandlet : commandletCollection) {
      // TODO: add more logic to remove unused keyword
      commandlets.add(commandlet.getName());
      commandlets.add(commandlet.getKeyword());
    }

    for (Commandlet commandlet : commandletCollection) {
      if (commandlet instanceof ToolCommandlet) {
        // TODO: add more logic to remove unused keyword
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
    // options
    List<Property<?>> optionList = new ArrayList<>();
    for (String singleWord : words) {
      if (singleWord.startsWith("-")) {
        Property<?> commandletOption = cmd.getOption(singleWord);
        if (commandletOption != null) {
          optionList.add(commandletOption);
        }
      }
    }
    // cleanup options
    Set<String> cleanedOptions = new HashSet<>();
    List<Property<?>> properties = cmd.getProperties();
    Set<Property<?>> options = new HashSet<>(properties);
    for (Property<?> option : optionList) {
      // removes aliases and vice versa too (--trace removes -t too)
      options.remove(option);
    }
    for (Property<?> option : options) {
      if (option instanceof FlagProperty) {
        cleanedOptions.add(option.getName());
        cleanedOptions.add(option.getAlias());
      }
      if (option instanceof LocaleProperty) {
        cleanedOptions.add(option.getName());
      }
    }

    List<String> wordsWithoutOptions = new ArrayList<>();
    for (String singleWord : words) {
      if (!singleWord.startsWith("-")) {
        wordsWithoutOptions.add(singleWord);
      }
    }

    // extra case to prevent options after tool commands
    if (word.startsWith("-") && wordsWithoutOptions.isEmpty()) {
      addCandidates(candidates, cleanedOptions); // adds rest of options without used option
    }

    if (wordsWithoutOptions.size() == 1) {
      addCandidates(candidates, this.commandlets); // adds all commandlets
    } else if (wordsWithoutOptions.size() == 2) {
      // 2nd layer..

      Commandlet commandlet = context.getCommandletManager().getCommandlet(wordsWithoutOptions.get(0));
      if (commandlet != null) {
        properties = commandlet.getProperties();
        for (Property<?> property : properties) {
          if (property instanceof ToolProperty) {
            addCandidates(candidates, this.toolCommandlets);
          }
          if (commandlet instanceof HelpCommandlet) { // help completion
            Set<String> commandletsWithoutHelp;
            commandletsWithoutHelp = this.commandlets;
            commandletsWithoutHelp.remove(commandlet.getName());
            addCandidates(candidates, commandletsWithoutHelp);
          }
        }
      }
      // 3rd layer
    } else if (wordsWithoutOptions.size() == 3) {
      Commandlet commandlet = context.getCommandletManager().getCommandlet(wordsWithoutOptions.get(0));
      if (commandlet != null) {
        properties = commandlet.getProperties();
        for (Property<?> property : properties) {
          if (property instanceof VersionProperty) { // add version numbers
            Commandlet subCommandlet = context.getCommandletManager().getCommandlet(wordsWithoutOptions.get(1));
            if (subCommandlet != null) {
              String toolEdition = context.getVariables().getToolEdition(subCommandlet.getName());
              List<VersionIdentifier> versions = context.getUrls().getSortedVersions(subCommandlet.getName(),
                  toolEdition);
              int sort = 0;
              // adds version numbers in sorted order (descending)
              for (VersionIdentifier vi : versions) {
                sort++;
                String versionName = vi.toString();
                candidates.add(new Candidate(versionName, versionName, null, null, null, null, true, sort));
              }
            }
          }
        }
      }
    }
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
}

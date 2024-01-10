package com.devonfw.tools.ide.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class IdeCompleter extends ArgumentCompleter {

  /** Checks for invalid pattern e.g. '- foo foobar' */
  private static final String INVALID_PATTERN_PART1 = "([\\-](\\s)+.*$";

  /** Checks for invalid pattern e.g. ' foo - ' or ' - foo' */
  private static final String INVALID_PATTERN_PART2 = "([\\s][a-z]*[\\s][\\-](\\s)*.*$)";

  /** Checks for invalid pattern e.g. 'get-version foobar - foobar' */
  private static final String INVALID_PATTERN_PART3 = "([a-z][\\-][a-z]*[\\s][\\-](\\s)*.*$))";

  /** Checks for invalid pattern e.g. 'foo-bar - foobar' */
  private static final String INVALID_PATTERN_PART4 = "(^[a-z]*(\\s)[\\-](\\s)*.*$)";

  /** Pattern which should stop autocompletion e.g. 'get-version mvn -' (prevents options after tool commands) */
  private static final String INVALID_PATTERN = INVALID_PATTERN_PART1 + "|" + INVALID_PATTERN_PART2 + "|"
      + INVALID_PATTERN_PART3 + "|" + INVALID_PATTERN_PART4;

  /** Pre-compiled pattern for better usage */
  private static final Pattern patternCompiled = Pattern.compile(INVALID_PATTERN, Pattern.MULTILINE);

  private final IdeContext context;

  private final Set<String> commandlets = new HashSet<>();

  private final Set<String> toolCommandlets = new HashSet<>();

  private final Set<Property<?>> commandletOptions = new HashSet<>();

  public IdeCompleter(IdeContext context) {

    super(NullCompleter.INSTANCE);
    this.context = context;

    Collection<Commandlet> commandletCollection = context.getCommandletManager().getCommandlets();

    for (Commandlet commandlet : commandletCollection) {
      // TODO: add more logic to remove unused keyword, see: https://github.com/devonfw/IDEasy/issues/167
      this.commandlets.add(commandlet.getName());
      this.commandlets.add(commandlet.getKeyword());
    }

    for (Commandlet commandlet : commandletCollection) {
      if (commandlet instanceof ToolCommandlet) {
        // TODO: add more logic to remove unused keyword, see: https://github.com/devonfw/IDEasy/issues/167
        this.toolCommandlets.add(commandlet.getName());
        this.toolCommandlets.add(commandlet.getKeyword());
      }

    }

  }

  @Override
  public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {

    assert commandLine != null;
    assert candidates != null;

    // extra check to prevent options after tool commands
    if (checkInvalidPattern(commandLine.line())) {
      return;
    }

    String word = commandLine.word();
    List<String> words = commandLine.words();
    // options
    List<Property<?>> optionList = new ArrayList<>();
    for (String singleWord : words) {
      if (singleWord.startsWith("-")) {
        // Property<?> commandletOption = this.cmd.getOption(singleWord);
        // if (commandletOption != null) {
        // optionList.add(commandletOption);
        // }
      }
    }
    // cleanup options
    Set<String> cleanedOptions = new HashSet<>();

    // adds non options to list
    List<String> wordsWithoutOptions = new ArrayList<>();
    for (String singleWord : words) {
      if (!singleWord.startsWith("-")) {
        wordsWithoutOptions.add(singleWord);
      }
    }

    if (word.startsWith("-") && wordsWithoutOptions.isEmpty()) {
      addCandidates(candidates, cleanedOptions); // adds rest of options without used option
    }

    if (wordsWithoutOptions.size() == 1) {
      addCandidates(candidates, this.commandlets); // adds all commandlets
    } else if (wordsWithoutOptions.size() == 2) {
      // 2nd layer..

      Commandlet commandlet = this.context.getCommandletManager().getCommandlet(wordsWithoutOptions.get(0));
      if (commandlet != null) {
        List<Property<?>> properties = commandlet.getProperties();
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
      Commandlet commandlet = this.context.getCommandletManager().getCommandlet(wordsWithoutOptions.get(0));
      if (commandlet != null) {
        List<Property<?>> properties = commandlet.getProperties();
        for (Property<?> property : properties) {
          if (property instanceof VersionProperty) { // add version numbers
            Commandlet subCommandlet = this.context.getCommandletManager().getCommandlet(wordsWithoutOptions.get(1));
            if (subCommandlet != null) {
              String toolEdition = this.context.getVariables().getToolEdition(subCommandlet.getName());
              List<VersionIdentifier> versions = this.context.getUrls().getSortedVersions(subCommandlet.getName(),
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

  private boolean checkInvalidPattern(String commandLine) {

    Matcher matcher = patternCompiled.matcher(commandLine);

    return matcher.find();
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

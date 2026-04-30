package com.devonfw.tools.ide.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.property.EnumProperty;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.step.StepImpl;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.validation.ValidationState;

/**
 * Helper class to suggest corrections for mistyped CLI options, commands, and tools using Levenshtein distance.
 */
public class CliSuggester {

  private static final Logger LOG = LoggerFactory.getLogger(CliSuggester.class);

  private final IdeContext context;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CliSuggester(IdeContext context) {
    this.context = context;
  }


  /**
   * Handles the case where a commandlet requires an IDEasy project context (IDE_ROOT/IDE_HOME) but the user is not inside one.
   *
   * @param commandlet the {@link Commandlet} that the user tried to run.
   * @param step the current {@link StepImpl} for error reporting.
   * @return {@code true} if handled (message printed), {@code false} otherwise.
   */
  public boolean handleMissingProjectContext(Commandlet commandlet, StepImpl step) {

    if (commandlet == null) {
      return false;
    }

    boolean missingIdeRoot = commandlet.isIdeRootRequired() && (this.context.getIdeRoot() == null);
    boolean missingIdeHome = commandlet.isIdeHomeRequired() && (this.context.getIdeHome() == null);

    if (!(missingIdeRoot || missingIdeHome)) {
      return false;
    }

    String name = commandlet.getName();

    // Match your expected output wording (project, not project root)
    step.error("The {} commandlet requires to be an IDEasy project to work.", name);
    IdeLogLevel.INTERACTION.log(LOG, "Please run \"icd <project-name>\" before calling \"ide {}\".", name);
    IdeLogLevel.INTERACTION.log(LOG, "Call \"ide help\" for additional details.");

    return true;
  }


  /**
   * Handles invalid option errors and suggests corrections.
   *
   * @param result the {@link com.devonfw.tools.ide.validation.ValidationResult} from option parsing.
   * @param commandlet the {@link Commandlet} that was being executed.
   * @param step the current {@link StepImpl} for error reporting.
   * @return {@code true} if handled (suggestion provided), {@code false} otherwise.
   */
  public boolean handleInvalidOption(ValidationState result, Commandlet commandlet, StepImpl step) {

    if ((result == null) || (commandlet == null)) {
      return false;
    }
    String message = result.getErrorMessage();
    if ((message == null) || !message.contains("Invalid option \"") || result.getInvalidOption() == null) {
      return false;
    }
    String invalidOption = result.getInvalidOption();
    if (invalidOption == null) {
      return false;
    }
    List<String> options = getAllOptionNames(commandlet);
    String suggestion = bestSuggestion(invalidOption, options);

    step.error("Option \"{}\" not found for commandlet \"{}\"", invalidOption, commandlet.getName());
    if (suggestion != null) {
      IdeLogLevel.INTERACTION.log(LOG, "Did you mean \"{}\"?", suggestion);
    }
    IdeLogLevel.INTERACTION.log(LOG, "Available options are: {}.", String.join(", ", options));
    IdeLogLevel.INTERACTION.log(LOG, "Call \"ide help {}\" for additional details.", commandlet.getName());
    return true;
  }


  /**
   * Handles missing commandlet errors and suggests corrections.
   *
   * @param commandKey the command name that was not found.
   * @param step the current {@link StepImpl} for error reporting.
   * @return {@code true} if handled (suggestion provided), {@code false} otherwise.
   */
  public boolean handleMissingCommandlet(String commandKey, StepImpl step) {

    // Try to find a suggestion among commandlets
    List<String> commandletNames = getAllCommandletNames();
    String commandletSuggestion = bestSuggestion(commandKey, commandletNames);

    // Try to find a suggestion among tools (if not found in commandlets)
    if (commandletSuggestion == null) {
      List<String> toolNames = getAllToolNames();
      commandletSuggestion = bestSuggestion(commandKey, toolNames);
    }

    if (commandletSuggestion != null) {
      step.error("Unknown command \"{}\".", commandKey);
      IdeLogLevel.INTERACTION.log(LOG, "Did you mean \"{}\"?", commandletSuggestion);
      IdeLogLevel.INTERACTION.log(LOG, "Call \"ide help\" for additional details.");
      return true;
    }

    return false;
  }


  /**
   * Handles invalid argument value errors for properties and suggests corrections.
   *
   * @param result the {@link com.devonfw.tools.ide.validation.ValidationResult} from argument parsing.
   * @param commandlet the {@link Commandlet} that was being executed.
   * @return {@code true} if handled (suggestion provided), {@code false} otherwise.
   */
  public boolean handleInvalidArgument(ValidationState result, Commandlet commandlet) {

    if ((result == null) || (commandlet == null)) {
      return false;
    }
    String invalidValue = result.getInvalidArgument();
    String invalidProperty = result.getInvalidArgumentProperty();
    if (invalidValue == null || invalidProperty == null) {
      return false;
    }
    // Find the property in the commandlet
    Property<?> property = null;
    for (Property<?> prop : commandlet.getProperties()) {
      if (prop.getName().equals(invalidProperty) || (prop.getAlias() != null && prop.getAlias().equals(invalidProperty))) {
        property = prop;
        break;
      }
    }
    if (property == null) {
      return false;
    }
    // Get valid values for the property
    List<String> validValues = getValidValuesForProperty(property);
    if (validValues == null || validValues.isEmpty()) {
      return false;
    }

    String suggestion = bestSuggestion(invalidValue, validValues);

    if (suggestion != null) {
      IdeLogLevel.INTERACTION.log(LOG, "Did you mean \"{}={}\"?", property.getName(), suggestion);
    }
    IdeLogLevel.INTERACTION.log(LOG, "Valid values for '{}' are: {}.", invalidProperty, String.join(", ", validValues));
    IdeLogLevel.INTERACTION.log(LOG, "Call \"ide help {}\" for additional details.", commandlet.getName());
    return true;
  }

  //------------------------- Helper methods-------------------------

  /**
   * Gets valid values for a property (especially for Enum properties).
   *
   * @param property the {@link Property}.
   * @return a {@link List} of valid values, or {@code null} if the property doesn't have a limited set of valid values.
   */
  private List<String> getValidValuesForProperty(Property<?> property) {

    List<String> validValues = new ArrayList<>();

    // Check if the property is an EnumProperty
    if (property instanceof EnumProperty<?> enumProperty) {
      Enum<?>[] enumConstants = enumProperty.getValueType().getEnumConstants();
      if (enumConstants != null) {
        for (Enum<?> enumConstant : enumConstants) {
          validValues.add(enumConstant.name().toLowerCase(Locale.ROOT));
        }
      }
    }

    return validValues.isEmpty() ? null : validValues;
  }

  /**
   * Gets all option names for a commandlet.
   *
   * @param cmd the {@link Commandlet}.
   * @return {@link List} of all option names and aliases.
   */
  private List<String> getAllOptionNames(Commandlet cmd) {

    List<String> opts = new ArrayList<>();
    for (Property<?> p : cmd.getProperties()) {
      if (p.isOption()) {
        if (!opts.contains(p.getName())) {
          opts.add(p.getName());
        }
        if (p.getAlias() != null) {
          if (!opts.contains(p.getAlias())) {
            opts.add(p.getAlias());
          }
        }
      }
    }
    return opts;
  }

  /**
   * Gets all available commandlet names.
   *
   * @return {@link List} of commandlet names.
   */
  private List<String> getAllCommandletNames() {

    List<String> names = new ArrayList<>();
    for (Commandlet cmd : this.context.getCommandletManager().getCommandlets()) {
      names.add(cmd.getName());
    }
    return names;
  }

  /**
   * Gets all available tool names.
   *
   * @return {@link List} of tool names.
   */
  private List<String> getAllToolNames() {

    List<String> names = new ArrayList<>();
    for (Commandlet cmd : this.context.getCommandletManager().getCommandlets()) {
      if (cmd instanceof ToolCommandlet) {
        names.add(cmd.getName());
      }
    }
    return names;
  }

  /**
   * Finds the best matching suggestion from candidates using Levenshtein distance.
   *
   * @param input the input string to match.
   * @param candidates the list of candidate strings.
   * @return the best matching candidate, or {@code null} if no suitable match is found.
   */
  public String bestSuggestion(String input, List<String> candidates) {

    String best = null;
    int bestDist = Integer.MAX_VALUE;
    for (String c : candidates) {
      int d = levenshtein(input, c);
      if (d < bestDist) {
        bestDist = d;
        best = c;
      }
    }
    if (best != null) {
      int threshold = Math.max(3, input.length() / 2);
      if (bestDist <= threshold) {
        return best;
      }
    }
    return null;
  }

  /**
   * Calculates the Levenshtein distance between two strings.
   *
   * @param a the first string.
   * @param b the second string.
   * @return the edit distance between the strings.
   */
  private int levenshtein(String a, String b) {
    if (a == null) {
      a = "";
    }
    if (b == null) {
      b = "";
    }
    int[] costs = new int[b.length() + 1];
    for (int j = 0; j < costs.length; j++) {
      costs[j] = j;
    }
    for (int i = 1; i <= a.length(); i++) {
      costs[0] = i;
      int nw = i - 1;
      for (int j = 1; j <= b.length(); j++) {
        int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
            a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
        nw = costs[j];
        costs[j] = cj;
      }
    }
    return costs[b.length()];
  }

}


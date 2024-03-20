package com.devonfw.tools.ide.environment;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.variable.VariableDefinition;

/**
 * Abstract base implementation of {@link EnvironmentVariables}.
 */
public abstract class AbstractEnvironmentVariables implements EnvironmentVariables {

  /**
   * When we replace variable expressions with their value the resulting {@link String} can change in size (shrink or
   * grow). By adding a bit of extra capacity we reduce the chance that the capacity is too small and a new buffer array
   * has to be allocated and array-copy has to be performed.
   */
  private static final int EXTRA_CAPACITY = 8;

  // Variable surrounded with "${" and "}" such as "${JAVA_HOME}" 1......2........
  private static final Pattern VARIABLE_SYNTAX = Pattern.compile("(\\$\\{([^}]+)})");

  private static final String SELF_REFERENCING_NOT_FOUND = "";

  private static final int MAX_RECURSION = 9;

  private static final String VARIABLE_PREFIX = "${";

  private static final String VARIABLE_SUFFIX = "}";

  /** @see #getParent() */
  protected final AbstractEnvironmentVariables parent;

  /** The {@link IdeContext} instance. */
  protected final IdeContext context;

  private String source;

  /**
   * The constructor.
   *
   * @param parent the parent {@link EnvironmentVariables} to inherit from.
   * @param context the {@link IdeContext}.
   */
  public AbstractEnvironmentVariables(AbstractEnvironmentVariables parent, IdeContext context) {

    super();
    this.parent = parent;
    if (context == null) {
      if (parent == null) {
        throw new IllegalArgumentException("parent and logger must not both be null!");
      }
      this.context = parent.context;
    } else {
      this.context = context;
    }
  }

  @Override
  public EnvironmentVariables getParent() {

    return this.parent;
  }

  @Override
  public Path getPropertiesFilePath() {

    return null;
  }

  @Override
  public String getSource() {

    if (this.source == null) {
      this.source = getType().toString();
      Path propertiesPath = getPropertiesFilePath();
      if (propertiesPath != null) {
        this.source = this.source + "@" + propertiesPath;
      }
    }
    return this.source;
  }

  /**
   * @param name the name of the variable to check.
   * @return {@code true} if the variable shall be exported, {@code false} otherwise.
   */
  protected boolean isExported(String name) {

    if (this.parent != null) {
      if (this.parent.isExported(name)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public final Collection<VariableLine> collectVariables() {

    return collectVariables(false);
  }

  @Override
  public final Collection<VariableLine> collectExportedVariables() {

    return collectVariables(true);
  }

  private final Collection<VariableLine> collectVariables(boolean onlyExported) {

    Set<String> variableNames = new HashSet<>();
    collectVariables(variableNames);
    List<VariableLine> variables = new ArrayList<>(variableNames.size());
    for (String name : variableNames) {
      boolean export = isExported(name);
      if (!onlyExported || export) {
        String value = get(name);
        variables.add(VariableLine.of(export, name, value));
      }
    }
    return variables;
  }

  /**
   * @param variables the {@link Set} where to add the names of the variables defined here.
   */
  protected void collectVariables(Set<String> variables) {

    if (this.parent != null) {
      this.parent.collectVariables(variables);
    }
  }

  /**
   * @param propertiesFilePath the {@link #getPropertiesFilePath() propertiesFilePath} of the child
   *        {@link EnvironmentVariables}.
   * @param type the {@link #getType() type}.
   * @return the new {@link EnvironmentVariables}.
   */
  public AbstractEnvironmentVariables extend(Path propertiesFilePath, EnvironmentVariablesType type) {

    return new EnvironmentVariablesPropertiesFile(this, type, propertiesFilePath, this.context);
  }

  /**
   * @return a new child {@link EnvironmentVariables} that will resolve variables recursively or this instance itself if
   *         already satisfied.
   */
  public EnvironmentVariables resolved() {

    return new EnvironmentVariablesResolved(this);
  }

  @Override
  public String resolve(String string, Object src) {

    return resolve(string, src, 0, src, string, this);
  }

  /**
   * This method is called recursively. This allows you to resolve variables that are defined by other variables.
   *
   * @param value the {@link String} that potentially contains variables in the syntax "${«variable«}". Those will be
   *        resolved by this method and replaced with their {@link #get(String) value}.
   * @param src the source where the {@link String} to resolve originates from. Should have a reasonable
   *        {@link Object#toString() string representation} that will be used in error or log messages if a variable
   *        could not be resolved.
   * @param recursion the current recursion level. This is used to interrupt endless recursion.
   * @param rootSrc the root source where the {@link String} to resolve originates from.
   * @param rootValue the root value to resolve.
   * @param resolvedVars this is a reference to an object of {@link EnvironmentVariablesResolved} being the lowest level
   *        in the {@link EnvironmentVariablesType hierarchy} of variables. In case of a self-referencing variable
   *        {@code x} the resolving has to continue one level higher in the {@link EnvironmentVariablesType hierarchy}
   *        to avoid endless recursion. The {@link EnvironmentVariablesResolved} is then used if another variable
   *        {@code y} must be resolved, since resolving this variable has to again start at the lowest level. For
   *        example: For levels {@code l1, l2} with {@code l1 < l2} and {@code x=${x} foo} and {@code y=bar} defined at
   *        level {@code l1} and {@code x=test ${y}} defined at level {@code l2}, {@code x} is first resolved at level
   *        {@code l1} and then up the {@link EnvironmentVariablesType hierarchy} at {@code l2} to avoid endless
   *        recursion. However, {@code y} must be resolved starting from the lowest level in the
   *        {@link EnvironmentVariablesType hierarchy} and therefore {@link EnvironmentVariablesResolved} is used.
   * @return the given {@link String} with the variables resolved.
   */
  private String resolve(String value, Object src, int recursion, Object rootSrc, String rootValue,
      AbstractEnvironmentVariables resolvedVars) {

    if (value == null) {
      return null;
    }
    if (recursion > MAX_RECURSION) {
      throw new IllegalStateException("Reached maximum recursion resolving " + value + " for root variable " + rootSrc
          + " with value '" + rootValue + "'.");
    }
    recursion++;

    Matcher matcher = VARIABLE_SYNTAX.matcher(value);
    if (!matcher.find()) {
      return value;
    }
    StringBuilder sb = new StringBuilder(value.length() + EXTRA_CAPACITY);
    do {
      String variableName = matcher.group(2);
      String variableValue = resolvedVars.getValue(variableName);
      if (variableValue == null) {
        this.context.warning("Undefined variable {} in '{}={}' for root '{}={}'", variableName, src, value, rootSrc,
            rootValue);
        continue;
      }
      EnvironmentVariables lowestFound = findVariable(variableName);
      boolean isNotSelfReferencing = lowestFound == null || !lowestFound.getFlat(variableName).equals(value);

      if (isNotSelfReferencing) {
        // looking for "variableName" starting from resolved upwards the hierarchy
        String replacement = resolvedVars.resolve(variableValue, variableName, recursion, rootSrc, rootValue,
            resolvedVars);
        matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
      } else { // is self referencing
        // finding next occurrence of "variableName" up the hierarchy of EnvironmentVariablesType
        EnvironmentVariables next = lowestFound.getParent();
        while (next != null) {
          if (next.getFlat(variableName) != null) {
            break;
          }
          next = next.getParent();
        }
        if (next == null) {
          matcher.appendReplacement(sb, Matcher.quoteReplacement(SELF_REFERENCING_NOT_FOUND));
          continue;
        }
        // resolving a self referencing variable one level up the hierarchy of EnvironmentVariablesType, i.e. at "next",
        // to avoid endless recursion
        String replacement = ((AbstractEnvironmentVariables) next).resolve(next.getFlat(variableName), variableName,
            recursion, rootSrc, rootValue, resolvedVars);
        matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));

      }
    } while (matcher.find());
    matcher.appendTail(sb);

    String resolved = sb.toString();
    return resolved;
  }

  /**
   * Like {@link #get(String)} but with higher-level features including to resolve {@link IdeVariables} with their
   * default values.
   *
   * @param name the name of the variable to get.
   * @return the value of the variable.
   */
  protected String getValue(String name) {

    VariableDefinition<?> var = IdeVariables.get(name);
    String value;
    if ((var != null) && var.isForceDefaultValue()) {
      value = var.getDefaultValueAsString(this.context);
    } else {
      value = this.parent.get(name);
    }
    if ((value == null) && (var != null)) {
      String key = var.getName();
      if (!name.equals(key)) {
        value = this.parent.get(key);
      }
      if (value != null) {
        value = var.getDefaultValueAsString(this.context);
      }
    }
    if ((value != null) && (value.startsWith("~/"))) {
      value = this.context.getUserHome() + value.substring(1);
    }
    return value;
  }

  @Override
  public String inverseResolve(String string, Object src) {

    String result = string;
    // TODO add more variables to IdeVariables like JAVA_HOME
    for (VariableDefinition<?> variable : IdeVariables.VARIABLES) {
      if (variable != IdeVariables.PATH) {
        String name = variable.getName();
        String value = get(name);
        if (value == null) {
          value = variable.getDefaultValueAsString(this.context);
        }
        if (value != null) {
          result = result.replace(value, VARIABLE_PREFIX + name + VARIABLE_SUFFIX);
        }
      }
    }
    if (!result.equals(string)) {
      this.context.trace("Inverse resolved '{}' to '{}' from {}.", string, result, src);
    }
    return result;
  }

  @Override
  public String toString() {

    return getSource();
  }

}

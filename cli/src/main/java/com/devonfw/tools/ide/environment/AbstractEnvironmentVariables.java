package com.devonfw.tools.ide.environment;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.variable.VariableDefinition;
import com.devonfw.tools.ide.variable.VariableSyntax;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Abstract base implementation of {@link EnvironmentVariables}.
 */
public abstract class AbstractEnvironmentVariables implements EnvironmentVariables {

  /**
   * When we replace variable expressions with their value the resulting {@link String} can change in size (shrink or grow). By adding a bit of extra capacity
   * we reduce the chance that the capacity is too small and a new buffer array has to be allocated and array-copy has to be performed.
   */
  private static final int EXTRA_CAPACITY = 8;

  private static final String SELF_REFERENCING_NOT_FOUND = "";

  private static final int MAX_RECURSION = 9;

  /**
   * @see #getParent()
   */
  protected final AbstractEnvironmentVariables parent;

  /**
   * The {@link IdeContext} instance.
   */
  protected final IdeContext context;

  private VariableSource source;

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
  public Path getLegacyPropertiesFilePath() {

    return null;
  }

  @Override
  public VariableSource getSource() {

    if (this.source == null) {
      this.source = new VariableSource(getType(), getPropertiesFilePath());
    }
    return this.source;
  }

  /**
   * @param name the name of the variable to check.
   * @return {@code true} if the variable shall be exported, {@code false} otherwise.
   */
  protected boolean isExported(String name) {

    if (this.parent != null) {
      return this.parent.isExported(name);
    }
    return false;
  }

  @Override
  public final List<VariableLine> collectVariables() {

    return collectVariables(false);
  }

  @Override
  public final List<VariableLine> collectExportedVariables() {

    return collectVariables(true);
  }

  private final List<VariableLine> collectVariables(boolean onlyExported) {

    Map<String, VariableLine> variables = new HashMap<>();
    collectVariables(variables, onlyExported, this);
    return new ArrayList<>(variables.values());
  }

  /**
   * @param variables the {@link Map} where to add the names of the variables defined here as keys, and their corresponding source as value.
   */
  protected void collectVariables(Map<String, VariableLine> variables, boolean onlyExported, AbstractEnvironmentVariables resolver) {

    if (this.parent != null) {
      this.parent.collectVariables(variables, onlyExported, resolver);
    }
  }

  protected VariableLine createVariableLine(String name, boolean onlyExported, AbstractEnvironmentVariables resolver) {

    boolean export = resolver.isExported(name);
    if (!onlyExported || export) {
      String value = resolver.get(name, false);
      if (value != null) {
        return VariableLine.of(export, name, value, getSource());
      }
    }
    return null;
  }

  /**
   * @param propertiesFolderPath the {@link Path} to the folder containing the {@link #getPropertiesFilePath() properties file} of the child
   *     {@link EnvironmentVariables}.
   * @param type the {@link #getType() type}.
   * @return the new {@link EnvironmentVariables}.
   */
  public AbstractEnvironmentVariables extend(Path propertiesFolderPath, EnvironmentVariablesType type) {

    return new EnvironmentVariablesPropertiesFile(this, type, propertiesFolderPath, null, this.context);
  }

  /**
   * @return a new child {@link EnvironmentVariables} that will resolve variables recursively or this instance itself if already satisfied.
   */
  public EnvironmentVariables resolved() {

    return new EnvironmentVariablesResolved(this);
  }

  @Override
  public String resolve(String string, Object source) {
    return resolveRecursive(string, source, 0, this, new ResolveContext(source, string, false, VariableSyntax.CURLY));
  }

  @Override
  public String resolve(String string, Object source, boolean legacySupport) {

    return resolveRecursive(string, source, 0, this, new ResolveContext(source, string, legacySupport, null));
  }

  /**
   * This method is called recursively. This allows you to resolve variables that are defined by other variables.
   *
   * @param value the {@link String} that potentially contains variables in the syntax "${«variable«}". Those will be resolved by this method and replaced
   *     with their {@link #get(String) value}.
   * @param source the source where the {@link String} to resolve originates from. Should have a reasonable {@link Object#toString() string representation}
   *     that will be used in error or log messages if a variable could not be resolved.
   * @param recursion the current recursion level. This is used to interrupt endless recursion.
   * @param resolvedVars this is a reference to an object of {@link EnvironmentVariablesResolved} being the lowest level in the
   *     {@link EnvironmentVariablesType hierarchy} of variables. In case of a self-referencing variable {@code x} the resolving has to continue one level
   *     higher in the {@link EnvironmentVariablesType hierarchy} to avoid endless recursion. The {@link EnvironmentVariablesResolved} is then used if another
   *     variable {@code y} must be resolved, since resolving this variable has to again start at the lowest level. For example: For levels {@code l1, l2} with
   *     {@code l1 < l2} and {@code x=${x} foo} and {@code y=bar} defined at level {@code l1} and {@code x=test ${y}} defined at level {@code l2}, {@code x} is
   *     first resolved at level {@code l1} and then up the {@link EnvironmentVariablesType hierarchy} at {@code l2} to avoid endless recursion. However,
   *     {@code y} must be resolved starting from the lowest level in the {@link EnvironmentVariablesType hierarchy} and therefore
   *     {@link EnvironmentVariablesResolved} is used.
   * @param context the {@link ResolveContext}.
   * @return the given {@link String} with the variables resolved.
   */
  private String resolveRecursive(String value, Object source, int recursion, AbstractEnvironmentVariables resolvedVars, ResolveContext context) {

    if (value == null) {
      return null;
    }
    if (recursion > MAX_RECURSION) {
      throw new IllegalStateException(
          "Reached maximum recursion resolving " + value + " for root variable " + context.rootSrc + " with value '" + context.rootValue + "'.");
    }
    recursion++;

    String resolved;
    if (context.syntax == null) {
      resolved = resolveWithSyntax(value, source, recursion, resolvedVars, context, VariableSyntax.SQUARE);
      if (context.legacySupport) {
        resolved = resolveWithSyntax(resolved, source, recursion, resolvedVars, context, VariableSyntax.CURLY);
      }
    } else {
      resolved = resolveWithSyntax(value, source, recursion, resolvedVars, context, context.syntax);
    }
    return resolved;
  }

  private String resolveWithSyntax(final String value, final Object src, final int recursion, final AbstractEnvironmentVariables resolvedVars,
      final ResolveContext context, final VariableSyntax syntax) {

    Matcher matcher = syntax.getPattern().matcher(value);
    if (!matcher.find()) {
      return value;
    }
    StringBuilder sb = new StringBuilder(value.length() + EXTRA_CAPACITY);
    do {
      String variableName = syntax.getVariable(matcher);
      String variableValue = resolvedVars.getValue(variableName, false);
      if (variableValue == null) {
        IdeLogLevel logLevel = IdeLogLevel.WARNING;
        if (context.legacySupport && (syntax == VariableSyntax.CURLY)) {
          logLevel = IdeLogLevel.INFO;
        }
        String var = matcher.group();
        if (recursion > 1) {
          this.context.level(logLevel).log("Undefined variable {} in '{}' at '{}={}'", var, context.rootSrc, src, value);
        } else {
          this.context.level(logLevel).log("Undefined variable {} in '{}'", var, src);
        }
        continue;
      }
      EnvironmentVariables lowestFound = findVariable(variableName);
      if ((lowestFound == null) || !lowestFound.getFlat(variableName).equals(value)) {
        // looking for "variableName" starting from resolved upwards the hierarchy
        String replacement = resolvedVars.resolveRecursive(variableValue, variableName, recursion, resolvedVars, context);
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
        String replacement = ((AbstractEnvironmentVariables) next).resolveRecursive(next.getFlat(variableName), variableName, recursion, resolvedVars, context
        );
        matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));

      }
    } while (matcher.find());
    matcher.appendTail(sb);

    String resolved = sb.toString();
    return resolved;
  }

  /**
   * Like {@link #get(String)} but with higher-level features including to resolve {@link IdeVariables} with their default values.
   *
   * @param name the name of the variable to get.
   * @param ignoreDefaultValue - {@code true} if the {@link VariableDefinition#getDefaultValue(IdeContext) default value} of a potential
   *     {@link VariableDefinition} shall be ignored, {@code false} to return default instead of {@code null}.
   * @return the value of the variable.
   */
  protected String getValue(String name, boolean ignoreDefaultValue) {

    VariableDefinition<?> var = IdeVariables.get(name);
    String value;
    if ((var != null) && var.isForceDefaultValue()) {
      value = var.getDefaultValueAsString(this.context);
    } else {
      value = this.parent.get(name, false);
    }
    if ((value == null) && (var != null)) {
      String key = var.getName();
      if (!name.equals(key)) {
        // try new name (e.g. IDE_TOOLS or IDE_HOME) if no value could be found by given legacy name (e.g.
        // DEVON_IDE_TOOLS or DEVON_IDE_HOME)
        value = this.parent.get(key, false);
      }
      if ((value == null) && !ignoreDefaultValue) {
        value = var.getDefaultValueAsString(this.context);
      }
    }
    if ((value != null) && (value.startsWith("~/"))) {
      value = this.context.getUserHome() + value.substring(1);
    }
    return value;
  }

  @Override
  public String inverseResolve(String string, Object src, VariableSyntax syntax) {

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
          result = result.replace(value, syntax.create(name));
        }
      }
    }
    if (!result.equals(string)) {
      this.context.trace("Inverse resolved '{}' to '{}' from {}.", string, result, src);
    }
    return result;
  }

  @Override
  public VersionIdentifier getToolVersion(String tool) {

    String variable = EnvironmentVariables.getToolVersionVariable(tool);
    String value = get(variable);
    if (value == null) {
      return VersionIdentifier.LATEST;
    } else if (value.isEmpty()) {
      this.context.warning("Variable {} is configured with empty value, please fix your configuration.", variable);
      return VersionIdentifier.LATEST;
    }
    VersionIdentifier version = VersionIdentifier.of(value);
    if (version == null) {
      // can actually never happen, but for robustness
      version = VersionIdentifier.LATEST;
    }
    return version;
  }

  @Override
  public String toString() {

    return getSource().toString();
  }

  /**
   * Simple record for the immutable arguments of recursive resolve methods.
   *
   * @param rootSrc the root source where the {@link String} to resolve originates from.
   * @param rootValue the root value to resolve.
   * @param legacySupport flag for legacy support (see {@link #resolve(String, Object, boolean)}). Only considered if {@link #syntax()} is {@code null}.
   * @param syntax the explicit {@link VariableSyntax} to use.
   */
  private static record ResolveContext(Object rootSrc, String rootValue, boolean legacySupport, VariableSyntax syntax) {

  }

}

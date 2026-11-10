package com.devonfw.tools.ide.environment;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.CharFilter;
import com.devonfw.tools.ide.common.CharReader;
import com.devonfw.tools.ide.common.ListCharFilter;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.expression.ExpressionContext;
import com.devonfw.tools.ide.expression.ExpressionFunction;
import com.devonfw.tools.ide.expression.ExpressionFunctionManager;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.variable.VariableDefinition;
import com.devonfw.tools.ide.variable.VariableSyntax;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Abstract base implementation of {@link EnvironmentVariables}.
 */
public abstract class AbstractEnvironmentVariables implements EnvironmentVariables {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractEnvironmentVariables.class);

  private static final char CHAR_NONE = '\0';

  private static final CharFilter VARIABLE_NAME_FILTER = ch -> (CharReader.isLetter(ch) || CharReader.isDigit(ch) || ch == '_' || ch == '-');

  private static final CharFilter FUNCTION_NAME_FILTER = ch -> (CharReader.isLetterLowercase(ch) || CharReader.isDigit(ch) || ch == '-');

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
    return resolveRecursive(new CharReader(value), CHAR_NONE, source, recursion, resolvedVars, context);
  }

  /**
   * This method is called recursively. This allows you to resolve variables that are defined by other variables.
   *
   * @param reader the {@link CharReader} that with the text to resolve. potentially contains variables in the syntax "${«variable«}" or functions using the
   *     syntax "@«name»(«parameters»)". Those will be resolved by this method and replaced with their evaluated value.
   * @param stop the stop character where to end reading from the given {@link CharReader} (e.g. quote character).
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
  private String resolveRecursive(CharReader reader, char stop, Object source, int recursion, AbstractEnvironmentVariables resolvedVars,
      ResolveContext context) {

    if (recursion > MAX_RECURSION) {
      // a circular variable reference is a configuration error of the user and therefore reported as CliException and not as IllegalStateException that
      // would indicate a programming error and reach the end-user as a stacktrace
      throw new CliException(
          "Reached maximum recursion resolving " + reader.getRest() + " for root variable " + context.rootSrc + " with value '" + context.rootValue + "'.");
    }
    recursion++;

    StringBuilder sb = new StringBuilder(reader.getRemaining() + EXTRA_CAPACITY);
    CharFilter stopFilter;
    if (stop == CHAR_NONE) {
      stopFilter = new ListCharFilter('$', '@');
    } else {
      stopFilter = new ListCharFilter('$', '@', stop);
    }
    char c;
    do {
      String fragment = reader.readUntil(stopFilter);
      sb.append(fragment); // append regular text as is
      int pos = reader.getIndex();
      c = reader.next();
      Boolean parsed = null;
      if (c == '$') {
        parsed = parseVariable(reader, source, recursion, resolvedVars, context, sb);
      } else if (c == '@') {
        parsed = parseExpressionFunction(reader, source, recursion, resolvedVars, context, sb);
      }
      if (Boolean.FALSE.equals(parsed)) {
        // parsing failed, copy parsed text as-is
        String nonsense = reader.getString().substring(pos, reader.getIndex());
        sb.append(nonsense);
      }
    } while (c != stop);
    return sb.toString();
  }

  private boolean parseExpressionFunction(CharReader reader, Object source, int recursion, AbstractEnvironmentVariables resolvedVars, ResolveContext context,
      StringBuilder sb) {

    String functionName = reader.readWhile(FUNCTION_NAME_FILTER);
    ExpressionFunction function = ExpressionFunctionManager.get().getFunction(functionName);
    if ((function == null) || !reader.expect('(')) {
      LOG.trace("Ignoring '@{}(' in '{}' as it is no registered expression function.", functionName, reader.getString());
      return false;
    }
    List<Object> args = parseArguments(reader, source, recursion, resolvedVars, context, sb);
    if (args == null) {
      // syntax error, tolerant handling already logged and appended in parseArguments
      return false;
    }
    String result = applyExpressionFunction(function, args, new EnvironmentExpressionContext(resolvedVars));
    sb.append(result);
    return true;
  }

  private String applyExpressionFunction(ExpressionFunction function, List<Object> args, ExpressionContext context) {

    function.reset();
    String result = function.apply(args, context);
    return (result == null) ? "" : result;
  }

  /**
   * Splits the argument list at top-level commas, then trims, unquotes and resolves each argument.
   *
   * @param context the {@link ExpressionContext}.
   * @return the {@link List} of arguments.
   */
  private List<Object> parseArguments(CharReader reader, Object source, int recursion, AbstractEnvironmentVariables resolvedVars, ResolveContext context,
      StringBuilder sb) {

    List<Object> args = new ArrayList<>();
    while (reader.hasNext()) {
      reader.skipSpaces();
      boolean added = parseArgument(reader, source, recursion, resolvedVars, context, args);
      if (added) {
        reader.skipSpaces();
        if (reader.expect(',')) {
          continue;
        }
      }
      if (reader.expect(')')) {
        break;
      }
      LOG.error("Illegal syntax found in expression arguments: {}", reader.getRest());
      return null;
    }
    return args;
  }

  private boolean parseArgument(CharReader reader, Object source, int recursion, AbstractEnvironmentVariables resolvedVars, ResolveContext context,
      List<Object> args) {

    if (reader.expect("null")) {
      args.add(null);
      return true;
    } else if (reader.expect("true")) {
      args.add(Boolean.TRUE);
      return true;
    } else if (reader.expect("false")) {
      args.add(Boolean.FALSE);
      return true;
    }
    char c = reader.peek();
    if ((c == '"') || (c == '\'')) {
      reader.next();
      String arg = resolveRecursive(reader, c, source, recursion, resolvedVars, context);
      args.add(arg);
      return true;
    }
    return false;
  }

  private boolean parseVariable(CharReader reader, Object source, int recursion, AbstractEnvironmentVariables resolvedVars, ResolveContext context,
      StringBuilder sb) {

    char c2 = reader.next();
    VariableSyntax syntax = null;
    if ((c2 == '[') && context.isSupportSquareSyntax()) {
      syntax = VariableSyntax.SQUARE;
    } else if ((c2 == '{') && (context.isSupportCurlySyntax())) {
      syntax = VariableSyntax.CURLY;
    }
    if (syntax == null) {
      return false;
    }
    String variableName = reader.readWhile(VARIABLE_NAME_FILTER);
    if (!variableName.isEmpty() && reader.expect(syntax.getClose())) {
      String variableValue = resolvedVars.getValue(variableName, false);
      if (variableValue == null) {
        Level logLevel = Level.WARN;
        if (context.legacySupport && (syntax == VariableSyntax.CURLY)) {
          logLevel = Level.INFO;
        }
        if (recursion > 1) {
          LOG.atLevel(logLevel).log("Undefined variable {} in '{}' at '{}={}'", variableName, context.rootSrc, source, reader.getString());
        } else {
          LOG.atLevel(logLevel).log("Undefined variable {} in '{}'", variableName, source);
        }
        return false;
      } else {
        EnvironmentVariables lowestFound = findVariable(variableName);
        String replacement = "";
        if ((lowestFound == null) || !reader.getString().equals(lowestFound.getFlat(variableName))) {
          // looking for "variableName" starting from resolved upwards the hierarchy
          replacement = resolvedVars.resolveRecursive(variableValue, variableName, recursion, resolvedVars, context);
        } else { // is self referencing
          // finding next occurrence of "variableName" up the hierarchy of EnvironmentVariablesType
          EnvironmentVariables next = lowestFound.getParent();
          while (next != null) {
            String variableValueFlat = next.getFlat(variableName);
            if (variableValueFlat != null) {
              // resolving a self referencing variable one level up the hierarchy of EnvironmentVariablesType, i.e. at "next", to avoid endless recursion
              replacement = ((AbstractEnvironmentVariables) next).resolveRecursive(variableValueFlat, variableName, recursion, resolvedVars, context);
              break;
            }
            next = next.getParent();
          }
        }
        sb.append(replacement);
      }
      return true;
    }
    return false;
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
    } else if ((value != null) && (var != null) && var.isDefaultValueAppended()) {
      // if user has set a value, append IDEasy's default to it instead of replacing it
      value = mergeWithDefault(value, var.getDefaultValueAsString(this.context));
    }
    if ((value != null) && (value.startsWith("~/"))) {
      value = this.context.getUserHome() + value.substring(1);
    }
    return value;
  }

  @Override
  public String inverseResolve(String string, Object src, VariableSyntax syntax) {

    String result = string;
    for (VariableDefinition<?> variable : IdeVariables.VARIABLES) {
      if (variable != IdeVariables.PATH) {
        String name = variable.getName();
        String value = get(name);
        if (value == null) {
          value = variable.getDefaultValueAsString(this.context);
        }
        // skip empty values: they carry nothing to inverse-resolve and String.replace("", token) would
        // insert the variable expression between every character, corrupting the value
        if ((value != null) && !value.isEmpty()) {
          result = result.replace(value, syntax.create(name));
        }
      }
    }
    if (!result.equals(string)) {
      LOG.trace("Inverse resolved '{}' to '{}' from {}.", string, result, src);
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
      LOG.warn("Variable {} is configured with empty value, please fix your configuration.", variable);
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
   * Implementation of {@link ExpressionContext} that connects an {@link com.devonfw.tools.ide.expression.ExpressionFunction} with this
   * {@link EnvironmentVariables} hierarchy.
   */
  private final class EnvironmentExpressionContext implements ExpressionContext {

    private final AbstractEnvironmentVariables resolvedVars;

    private EnvironmentExpressionContext(AbstractEnvironmentVariables resolvedVars) {

      super();
      this.resolvedVars = resolvedVars;
    }

    @Override
    public IdeContext getIdeContext() {

      return AbstractEnvironmentVariables.this.context;
    }

    @Override
    public String getVariable(String name) {

      return this.resolvedVars.get(name, false);
    }

    @Override
    public void setVariable(String name, String value, EnvironmentVariablesType type) {

      EnvironmentVariables variables = getByType(type);
      if (variables instanceof EnvironmentVariablesPropertiesFile propertiesFile) {
        propertiesFile.set(name, value);
        propertiesFile.save();
      } else {
        LOG.warn("Cannot persist variable {} since no configuration file is available for {}.", name, type);
      }
    }
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

    boolean isSupportSquareSyntax() {
      return (this.syntax == VariableSyntax.SQUARE) || (this.syntax == null);
    }

    boolean isSupportCurlySyntax() {
      return (this.syntax == VariableSyntax.CURLY) || this.legacySupport;
    }

  }

  /**
   * Appends IDEasy's default to the user's input value, stripping {@code -s <path>} and {@code -Dsettings.security=<path>}.
   *
   * @param value the user-defined value.
   * @param defaultValue IDEasy's default value.
   * @return the merged value.
   */
  static String mergeWithDefault(String value, String defaultValue) {
    if (defaultValue == null || defaultValue.isEmpty()) {
      return value;
    }
    StringBuilder merged = new StringBuilder();
    String[] tokens = value.trim().split("\\s+");
    for (int i = 0; i < tokens.length; i++) {
      String token = tokens[i];
      if (token.equals("-s")) {
        i++; // skip the path argument that follows
      } else if (!token.startsWith("-Dsettings.security=")) {
        merged.append(token).append(' ');
      }
    }
    return merged.append(defaultValue).toString();
  }

}

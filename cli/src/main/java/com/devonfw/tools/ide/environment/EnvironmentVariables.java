package com.devonfw.tools.ide.environment;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.variable.VariableDefinition;
import com.devonfw.tools.ide.variable.VariableSyntax;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Interface for the environment with the variables.
 */
public interface EnvironmentVariables {

  /** Filename of the default variable configuration file. {@value} */
  String DEFAULT_PROPERTIES = "ide.properties";

  /** Filename of the legacy variable configuration file. {@value} */
  String LEGACY_PROPERTIES = "devon.properties";

  /**
   * @param name the name of the environment variable to get.
   * @return the value of the variable with the given {@code name}. Will be {@code null} if no such variable is defined.
   */
  default String get(String name) {

    return get(name, false);
  }

  /**
   * @param name the name of the environment variable to get.
   * @param ignoreDefaultValue - {@code true} if the {@link VariableDefinition#getDefaultValue(IdeContext) default value} of a potential
   *     {@link VariableDefinition} shall be ignored, {@code false} to return default instead of {@code null}.
   * @return the value of the variable with the given {@code name}. Will be {@code null} if no such variable is defined.
   */
  default String get(String name, boolean ignoreDefaultValue) {

    String value = getFlat(name);
    if (value == null) {
      EnvironmentVariables parent = getParent();
      if (parent != null) {
        value = parent.get(name);
      }
    }
    return value;
  }

  /**
   * @param name the name of the environment variable to get.
   * @return the value of the variable with the given {@code name} as {@link Path}. Will be {@code null} if no such variable is defined.
   */
  default Path getPath(String name) {

    String value = get(name);
    if (value == null) {
      return null;
    }
    return Path.of(value);
  }

  /**
   * @param name the name of the environment variable to get.
   * @return the value of the variable with the given {@code name} without {@link #getParent() inheritance from parent}. Will be {@code null} if no such
   *     variable is defined.
   */
  String getFlat(String name);

  /**
   * @param tool the name of the tool (e.g. "java").
   * @return the edition of the tool to use.
   */
  default String getToolEdition(String tool) {

    String variable = getToolEditionVariable(tool);
    String value = get(variable);
    if (value == null) {
      value = tool;
    }
    return value;
  }

  /**
   * @param tool the name of the tool (e.g. "java").
   * @return the {@link VersionIdentifier} with the version of the tool to use. May also be a {@link VersionIdentifier#isPattern() version pattern}. Will be
   *     {@link VersionIdentifier#LATEST} if undefined.
   */
  VersionIdentifier getToolVersion(String tool);

  /**
   * @return the {@link EnvironmentVariablesType type} of this {@link EnvironmentVariables}.
   */
  EnvironmentVariablesType getType();

  /**
   * @param type the {@link #getType() type} of the requested {@link EnvironmentVariables}.
   * @return the {@link EnvironmentVariables} with the given {@link #getType() type} from this {@link EnvironmentVariables} along the
   *     {@link #getParent() parent} hierarchy or {@code null} if not found.
   */
  default EnvironmentVariables getByType(EnvironmentVariablesType type) {

    if (type == getType()) {
      return this;
    }
    EnvironmentVariables parent = getParent();
    if (parent == null) {
      return null;
    } else {
      return parent.getByType(type);
    }
  }

  /**
   * @return the {@link Path} to the underlying properties file or {@code null} if not based on such file (e.g. for EVS or
   *     {@link EnvironmentVariablesResolved}).
   */
  Path getPropertiesFilePath();

  /**
   * @return the {@link Path} to the {@link #LEGACY_PROPERTIES} if they exist for this {@link EnvironmentVariables} or {@code null} otherwise (does not exist).
   */
  Path getLegacyPropertiesFilePath();

  /**
   * @return the {@link VariableSource} of this {@link EnvironmentVariables}.
   */
  VariableSource getSource();

  /**
   * @return the parent {@link EnvironmentVariables} to inherit from or {@code null} if this is the {@link EnvironmentVariablesType#SYSTEM root}
   *     {@link EnvironmentVariables} instance.
   */
  default EnvironmentVariables getParent() {

    return null;
  }

  /**
   * @param name the {@link com.devonfw.tools.ide.variable.VariableDefinition#getName() name} of the variable to set.
   * @param value the new {@link #get(String) value} of the variable to set. May be {@code null} to unset the variable.
   * @return the old variable value.
   */
  default String set(String name, String value) {

    throw new UnsupportedOperationException();
  }

  /**
   * @param name the {@link com.devonfw.tools.ide.variable.VariableDefinition#getName() name} of the variable to set.
   * @param value the new {@link #get(String) value} of the variable to set. May be {@code null} to unset the variable.
   * @param export - {@code true} if the variable needs to be exported, {@code false} otherwise.
   * @return the old variable value.
   */
  default String set(String name, String value, boolean export) {

    throw new UnsupportedOperationException();
  }

  /**
   * Saves any potential {@link #set(String, String, boolean) changes} of this {@link EnvironmentVariables}.
   */
  default void save() {

    throw new UnsupportedOperationException("Not yet implemented!");
  }

  /**
   * @param name the {@link com.devonfw.tools.ide.variable.VariableDefinition#getName() name} of the variable to search for.
   * @return the closest {@link EnvironmentVariables} instance that defines the variable with the given {@code name} or {@code null} if the variable is not
   *     defined.
   */
  default EnvironmentVariables findVariable(String name) {

    String value = getFlat(name);
    if (value != null) {
      return this;
    }
    EnvironmentVariables parent = getParent();
    if (parent == null) {
      return null;
    } else {
      return parent.findVariable(name);
    }
  }

  /**
   * @return the {@link Collection} of the {@link VariableLine}s defined by this {@link EnvironmentVariables} including inheritance.
   */
  List<VariableLine> collectVariables();

  /**
   * @return the {@link Collection} of the {@link VariableLine#isExport() exported} {@link VariableLine}s defined by this {@link EnvironmentVariables} including
   *     inheritance.
   */
  List<VariableLine> collectExportedVariables();

  /**
   * @param string the {@link String} that potentially contains variables in {@link VariableSyntax#CURLY} ("${«variable«}"). Those will be resolved by this
   *     method and replaced with their {@link #get(String) value}.
   * @param source the source where the {@link String} to resolve originates from. Should have a reasonable {@link Object#toString() string representation}
   *     that will be used in error or log messages if a variable could not be resolved.
   * @return the given {@link String} with the variables resolved.
   * @see com.devonfw.tools.ide.tool.ide.IdeToolCommandlet
   */
  String resolve(String string, Object source);

  /**
   * @param string the {@link String} that potentially contains variables in {@link VariableSyntax}. Those will be resolved by this method and replaced with
   *     their {@link #get(String) value}.
   * @param source the source where the {@link String} to resolve originates from. Should have a reasonable {@link Object#toString() string representation}
   *     that will be used in error or log messages if a variable could not be resolved.
   * @param legacySupport - {@code true} for legacy support with {@link VariableSyntax#CURLY} as fallback, {@code false} otherwise.
   * @return the given {@link String} with the variables resolved.
   * @see com.devonfw.tools.ide.tool.ide.IdeToolCommandlet
   */
  String resolve(String string, Object source, boolean legacySupport);

  /**
   * The inverse operation of {@link #resolve(String, Object, boolean)}. Please note that the {@link #resolve(String, Object, boolean) resolve} operation is not
   * fully bijective. There may be multiple variables holding the same {@link #get(String) value} or there may be static text that can be equal to a
   * {@link #get(String) variable value}. This method does its best to implement the inverse resolution based on some heuristics.
   *
   * @param string the {@link String} where to find {@link #get(String) variable values} and replace them with according
   *     {@link com.devonfw.tools.ide.variable.VariableSyntax} expressions.
   * @param source the source where the {@link String} to inverse resolve originates from. Should have a reasonable
   *     {@link Object#toString() string representation} that will be used in error or log messages if the inverse resolving was not working as expected.
   * @return the given {@link String} with {@link #get(String) variable values} replaced with according {@link com.devonfw.tools.ide.variable.VariableSyntax}
   *     expressions.
   * @see com.devonfw.tools.ide.tool.ide.IdeToolCommandlet
   */
  default String inverseResolve(String string, Object source) {

    return inverseResolve(string, source, VariableSyntax.SQUARE);
  }

  /**
   * @param string the {@link String} where to find {@link #get(String) variable values} and replace them with according
   *     {@link com.devonfw.tools.ide.variable.VariableSyntax} expressions.
   * @param source the source where the {@link String} to inverse resolve originates from. Should have a reasonable
   *     {@link Object#toString() string representation} that will be used in error or log messages if the inverse resolving was not working as expected.
   * @param syntax the explicit {@link VariableSyntax} to use.
   * @return the given {@link String} with {@link #get(String) variable values} replaced with according {@link com.devonfw.tools.ide.variable.VariableSyntax}
   *     expressions.
   * @see #inverseResolve(String, Object)
   */
  String inverseResolve(String string, Object source, VariableSyntax syntax);

  /**
   * @param context the {@link IdeContext}.
   * @return the system {@link EnvironmentVariables} building the root of the {@link EnvironmentVariables} hierarchy.
   */
  static AbstractEnvironmentVariables ofSystem(IdeContext context) {

    return EnvironmentVariablesSystem.of(context);
  }

  /**
   * @param tool the name of the tool.
   * @return the name of the version variable.
   */
  static String getToolVersionVariable(String tool) {

    return getToolVariablePrefix(tool) + "_VERSION";
  }

  /**
   * @param tool the name of the tool.
   * @return the name of the edition variable.
   */
  static String getToolEditionVariable(String tool) {

    return getToolVariablePrefix(tool) + "_EDITION";
  }

  /**
   * @param tool the name of the tool.
   * @return the given {@code tool} name in UPPER_CASE without hyphen characters.
   */
  static String getToolVariablePrefix(String tool) {

    return tool.toUpperCase(Locale.ROOT).replace('-', '_');
  }
}

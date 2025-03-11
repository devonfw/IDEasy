package com.devonfw.tools.ide.environment;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Implementation of {@link EnvironmentVariables} for testing.
 */
public final class EnvironmentVariablesPropertiesMock extends EnvironmentVariablesMap {

  private static final Path PROPERTIES_FILE_PATH = Path.of(DEFAULT_PROPERTIES);

  private static final Path LEGACY_PROPERTIES_FILE_PATH = Path.of(LEGACY_PROPERTIES);

  private final EnvironmentVariablesType type;

  private final Map<String, String> variables;

  private final Set<String> exportedVariables;

  private final Set<String> modifiedVariables;

  /**
   * The constructor.
   *
   * @param parent the parent {@link EnvironmentVariables} to inherit from.
   * @param type the {@link #getType() type}.
   * @param context the {@link IdeContext}.
   */
  public EnvironmentVariablesPropertiesMock(AbstractEnvironmentVariables parent, EnvironmentVariablesType type,
      IdeContext context) {

    super(parent, context);
    Objects.requireNonNull(type);
    assert (type != EnvironmentVariablesType.RESOLVED);
    this.type = type;
    this.variables = new HashMap<>();
    this.exportedVariables = new HashSet<>();
    this.modifiedVariables = new HashSet<>();
  }

  @Override
  public void save() {

    if (this.modifiedVariables.isEmpty()) {
      this.context.trace("No changes to save in properties file {}", getPropertiesFilePath());
      return;
    }
    this.modifiedVariables.clear();
  }

  @Override
  protected Map<String, String> getVariables() {

    return this.variables;
  }

  @Override
  protected void collectVariables(Map<String, VariableLine> variables, boolean onlyExported, AbstractEnvironmentVariables resolver) {

    for (String key : this.variables.keySet()) {
      variables.computeIfAbsent(key, k -> createVariableLine(key, onlyExported, resolver));
    }
    super.collectVariables(variables, onlyExported, resolver);
  }

  @Override
  protected boolean isExported(String name) {

    if (this.exportedVariables.contains(name)) {
      return true;
    }
    return super.isExported(name);
  }

  @Override
  public EnvironmentVariablesType getType() {

    return this.type;
  }

  @Override
  public Path getPropertiesFilePath() {

    return PROPERTIES_FILE_PATH;
  }

  @Override
  public Path getLegacyPropertiesFilePath() {

    return LEGACY_PROPERTIES_FILE_PATH;
  }

  @Override
  public String set(String name, String value, boolean export) {

    String oldValue = this.variables.put(name, value);
    boolean flagChanged = export != this.exportedVariables.contains(name);
    if (Objects.equals(value, oldValue) && !flagChanged) {
      this.context.trace("Set variable '{}={}' caused no change in {}", name, value, getPropertiesFilePath());
    } else {
      this.context.debug("Set variable '{}={}' in {}", name, value, getPropertiesFilePath());
      this.modifiedVariables.add(name);
      if (export && (value != null)) {
        this.exportedVariables.add(name);
      } else {
        this.exportedVariables.remove(name);
      }
    }
    return oldValue;
  }

}

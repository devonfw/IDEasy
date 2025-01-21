package com.devonfw.tools.ide.environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.variable.VariableDefinition;

/**
 * Implementation of {@link EnvironmentVariables}.
 */
public final class EnvironmentVariablesPropertiesFile extends EnvironmentVariablesMap {

  private static final String NEWLINE = "\n";

  private final EnvironmentVariablesType type;

  private final Path propertiesFilePath;

  private final Path legacyPropertiesFilePath;

  private final Map<String, String> variables;

  private final Set<String> exportedVariables;

  private final Set<String> modifiedVariables;

  private Boolean legacyConfiguration;

  /**
   * The constructor.
   *
   * @param parent the parent {@link EnvironmentVariables} to inherit from.
   * @param type the {@link #getType() type}.
   * @param propertiesFilePath the {@link #getSource() source}.
   * @param context the {@link IdeContext}.
   */
  public EnvironmentVariablesPropertiesFile(AbstractEnvironmentVariables parent, EnvironmentVariablesType type,
      Path propertiesFilePath, IdeContext context) {

    this(parent, type, getParent(propertiesFilePath), propertiesFilePath, context);
  }

  /**
   * The constructor.
   *
   * @param parent the parent {@link EnvironmentVariables} to inherit from.
   * @param type the {@link #getType() type}.
   * @param propertiesFolderPath the {@link Path} to the folder where the properties file is expected.
   * @param propertiesFilePath the {@link #getSource() source}.
   * @param context the {@link IdeContext}.
   */
  public EnvironmentVariablesPropertiesFile(AbstractEnvironmentVariables parent, EnvironmentVariablesType type, Path propertiesFolderPath,
      Path propertiesFilePath, IdeContext context) {

    super(parent, context);
    Objects.requireNonNull(type);
    assert (type != EnvironmentVariablesType.RESOLVED);
    this.type = type;
    if (propertiesFolderPath == null) {
      this.propertiesFilePath = null;
      this.legacyPropertiesFilePath = null;
    } else {
      if (propertiesFilePath == null) {
        this.propertiesFilePath = propertiesFolderPath.resolve(DEFAULT_PROPERTIES);
      } else {
        this.propertiesFilePath = propertiesFilePath;
        assert (propertiesFilePath.getParent().equals(propertiesFolderPath));
      }
      Path legacyPropertiesFolderPath = propertiesFolderPath;
      if (type == EnvironmentVariablesType.USER) {
        // ~/devon.properties vs. ~/.ide/ide.properties
        legacyPropertiesFolderPath = propertiesFolderPath.getParent();
      }
      this.legacyPropertiesFilePath = legacyPropertiesFolderPath.resolve(LEGACY_PROPERTIES);
    }
    this.variables = new HashMap<>();
    this.exportedVariables = new HashSet<>();
    this.modifiedVariables = new HashSet<>();
    load();
  }

  private static Path getParent(Path path) {

    if (path == null) {
      return null;
    }
    return path.getParent();
  }

  private void load() {

    boolean success = load(this.propertiesFilePath);
    if (success) {
      this.legacyConfiguration = Boolean.FALSE;
    } else {
      success = load(this.legacyPropertiesFilePath);
      if (success) {
        this.legacyConfiguration = Boolean.TRUE;
      }
    }
  }

  private boolean load(Path file) {
    if (file == null) {
      return false;
    }
    if (!Files.exists(file)) {
      this.context.trace("Properties not found at {}", file);
      return false;
    }
    this.context.trace("Loading properties from {}", file);
    boolean legacyProperties = file.getFileName().toString().equals(LEGACY_PROPERTIES);
    try (BufferedReader reader = Files.newBufferedReader(file)) {
      String line;
      do {
        line = reader.readLine();
        if (line != null) {
          VariableLine variableLine = VariableLine.of(line, this.context, getSource());
          String name = variableLine.getName();
          if (name != null) {
            VariableLine migratedVariableLine = migrateLine(variableLine, false);
            if (migratedVariableLine == null) {
              this.context.warning("Illegal variable definition: {}", variableLine);
              continue;
            }
            String migratedName = migratedVariableLine.getName();
            String migratedValue = migratedVariableLine.getValue();
            boolean legacyVariable = IdeVariables.isLegacyVariable(name);
            if (legacyVariable && !legacyProperties) {
              this.context.warning("Legacy variable name is used to define variable {} in {} - please cleanup your configuration.", variableLine,
                  file);
            }
            String oldValue = this.variables.get(migratedName);
            if (oldValue != null) {
              VariableDefinition<?> variableDefinition = IdeVariables.get(name);
              if (legacyVariable) {
                // if the legacy name was configured we do not want to override the official variable!
                this.context.warning("Both legacy variable {} and official variable {} are configured in {} - ignoring legacy variable declaration!",
                    variableDefinition.getLegacyName(), variableDefinition.getName(), file);
              } else {
                this.context.warning("Duplicate variable definition {} with old value '{}' and new value '{}' in {}", name, oldValue, migratedValue,
                    file);
                this.variables.put(migratedName, migratedValue);
              }
            } else {
              this.variables.put(migratedName, migratedValue);
            }
            if (variableLine.isExport()) {
              this.exportedVariables.add(migratedName);
            }
          }
        }
      } while (line != null);
      return true;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load properties from " + file, e);
    }
  }

  @Override
  public void save() {

    boolean isLegacy = Boolean.TRUE.equals(this.legacyConfiguration);
    if (this.modifiedVariables.isEmpty() && !isLegacy) {
      this.context.trace("No changes to save in properties file {}", this.propertiesFilePath);
      return;
    }

    Path file = this.propertiesFilePath;
    if (isLegacy) {
      this.context.info("Converting legacy properties to {}", this.propertiesFilePath);
      file = this.legacyPropertiesFilePath;
    }

    List<VariableLine> lines = loadVariableLines(file);

    this.context.getFileAccess().mkdirs(this.propertiesFilePath.getParent());
    try (BufferedWriter writer = Files.newBufferedWriter(this.propertiesFilePath)) {
      // copy and modify original lines from properties file
      for (VariableLine line : lines) {
        VariableLine newLine = migrateLine(line, true);
        if (newLine == null) {
          this.context.debug("Removed variable line '{}' from {}", line, this.propertiesFilePath);
        } else {
          if (newLine != line) {
            this.context.debug("Changed variable line from '{}' to '{}' in {}", line, newLine, this.propertiesFilePath);
          }
          writer.append(newLine.toString());
          writer.append(NEWLINE);
          String name = line.getName();
          if (name != null) {
            this.modifiedVariables.remove(name);
          }
        }
      }
      // append variables that have been newly added
      for (String name : this.modifiedVariables) {
        String value = this.variables.get(name);
        if (value == null) {
          this.context.trace("Internal error: removed variable {} was not found in {}", name, this.propertiesFilePath);
        } else {
          boolean export = this.exportedVariables.contains(name);
          VariableLine line = VariableLine.of(export, name, value);
          writer.append(line.toString());
          writer.append(NEWLINE);
        }
      }
      this.modifiedVariables.clear();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to save properties to " + this.propertiesFilePath, e);
    }
    this.legacyConfiguration = Boolean.FALSE;
  }

  private List<VariableLine> loadVariableLines(Path file) {
    List<VariableLine> lines = new ArrayList<>();
    if (!Files.exists(file)) {
      // Skip reading if the file does not exist
      this.context.debug("Properties file {} does not exist, skipping read.", file);
      return lines;
    }
    try (BufferedReader reader = Files.newBufferedReader(file)) {
      String line;
      do {
        line = reader.readLine();
        if (line != null) {
          VariableLine variableLine = VariableLine.of(line, this.context, getSource());
          lines.add(variableLine);
        }
      } while (line != null);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load existing properties from " + file, e);
    }
    return lines;
  }

  private VariableLine migrateLine(VariableLine line, boolean saveNotLoad) {

    String name = line.getName();
    if (name != null) {
      VariableDefinition<?> variableDefinition = IdeVariables.get(name);
      if (variableDefinition != null) {
        line = variableDefinition.migrateLine(line);
      }
      if (saveNotLoad) {
        name = line.getName();
        if (this.modifiedVariables.contains(name)) {
          String value = this.variables.get(name);
          if (value == null) {
            return null;
          } else {
            line = line.withValue(value);
          }
        }
        boolean newExport = this.exportedVariables.contains(name);
        if (line.isExport() != newExport) {
          line = line.withExport(newExport);
        }
      }
    }
    return line;
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
  public boolean isExported(String name) {

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

    return this.propertiesFilePath;
  }

  @Override
  public Path getLegacyPropertiesFilePath() {

    return this.legacyPropertiesFilePath;
  }

  /**
   * @return {@code Boolean#TRUE} if the current variable state comes from {@link #getLegacyPropertiesFilePath()}, {@code Boolean#FALSE} if state comes from
   *     {@link #getPropertiesFilePath()}), and {@code null} if neither of these files existed (nothing was loaded).
   */
  public Boolean getLegacyConfiguration() {

    return this.legacyConfiguration;
  }

  @Override
  public String set(String name, String value) {

    return set(name, value, this.exportedVariables.contains(name));
  }

  @Override
  public String set(String name, String value, boolean export) {

    String oldValue = this.variables.put(name, value);
    boolean flagChanged = export != this.exportedVariables.contains(name);
    if (Objects.equals(value, oldValue) && !flagChanged) {
      this.context.trace("Set variable '{}={}' caused no change in {}", name, value, this.propertiesFilePath);
    } else {
      this.context.debug("Set variable '{}={}' in {}", name, value, this.propertiesFilePath);
      this.modifiedVariables.add(name);
      if (export && (value != null)) {
        this.exportedVariables.add(name);
      } else {
        this.exportedVariables.remove(name);
      }
    }
    return oldValue;
  }

  /**
   * Removes a property.
   *
   * @param name name of the property to remove.
   */
  public void remove(String name) {
    String oldValue = this.variables.remove(name);
    if (oldValue != null) {
      this.modifiedVariables.add(name);
      this.exportedVariables.remove(name);
      this.context.debug("Removed variable name of '{}' in {}", name, this.propertiesFilePath);
    }
  }

}

package com.devonfw.tools.ide.environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
final class EnvironmentVariablesPropertiesFile extends EnvironmentVariablesMap {

  private static final String NEWLINE = "\n";

  private final EnvironmentVariablesType type;

  private Path propertiesFilePath;

  private final Map<String, String> variables;

  private final Set<String> exportedVariables;

  private final Set<String> modifiedVariables;

  /**
   * The constructor.
   *
   * @param parent the parent {@link EnvironmentVariables} to inherit from.
   * @param type the {@link #getType() type}.
   * @param source the {@link #getSource() source}.
   * @param context the {@link IdeContext}.
   * @param variables the underlying variables.
   */
  EnvironmentVariablesPropertiesFile(AbstractEnvironmentVariables parent, EnvironmentVariablesType type,
      Path propertiesFilePath, IdeContext context) {

    super(parent, context);
    Objects.requireNonNull(type);
    assert (type != EnvironmentVariablesType.RESOLVED);
    this.type = type;
    this.propertiesFilePath = propertiesFilePath;
    this.variables = new HashMap<>();
    this.exportedVariables = new HashSet<>();
    this.modifiedVariables = new HashSet<>();
    load();
  }

  private void load() {

    if (this.propertiesFilePath == null) {
      return;
    }
    if (!Files.exists(this.propertiesFilePath)) {
      this.context.trace("Properties not found at {}", this.propertiesFilePath);
      return;
    }
    this.context.trace("Loading properties from {}", this.propertiesFilePath);
    try (BufferedReader reader = Files.newBufferedReader(this.propertiesFilePath)) {
      String line;
      do {
        line = reader.readLine();
        if (line != null) {
          VariableLine variableLine = VariableLine.of(line, this.context, this.propertiesFilePath);
          String name = variableLine.getName();
          if (name != null) {
            variableLine = migrateLine(variableLine, false);
            name = variableLine.getName();
            String value = variableLine.getValue();
            this.variables.put(name, value);
            if (variableLine.isExport()) {
              this.exportedVariables.add(name);
            }
          }
        }
      } while (line != null);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load properties from " + this.propertiesFilePath, e);
    }
  }

  @Override
  public void save() {

    if (this.modifiedVariables.isEmpty()) {
      this.context.trace("No changes to save in properties file {}", this.propertiesFilePath);
      return;
    }
    Path newPropertiesFilePath = this.propertiesFilePath;
    String propertiesFileName = this.propertiesFilePath.getFileName().toString();
    if (LEGACY_PROPERTIES.equals(propertiesFileName)) {
      newPropertiesFilePath = this.propertiesFilePath.getParent().resolve(DEFAULT_PROPERTIES);
      this.context.info("Converting legacy properties to {}", newPropertiesFilePath);
    }
    List<VariableLine> lines = new ArrayList<>();
    try (BufferedReader reader = Files.newBufferedReader(this.propertiesFilePath)) {
      String line;
      do {
        line = reader.readLine();
        if (line != null) {
          VariableLine variableLine = VariableLine.of(line, this.context, reader);
          lines.add(variableLine);
        }
      } while (line != null);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load existing properties from " + this.propertiesFilePath, e);
    }
    try (BufferedWriter writer = Files.newBufferedWriter(newPropertiesFilePath, StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING)) {
      // copy and modify original lines from properties file
      for (VariableLine line : lines) {
        VariableLine newLine = migrateLine(line, true);
        if (newLine == null) {
          this.context.debug("Removed variable line '{}' from {}", line, newPropertiesFilePath);
        } else {
          if (newLine != line) {
            this.context.debug("Changed variable line from '{}' to '{}' in {}", line, newLine, newPropertiesFilePath);
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
      throw new IllegalStateException("Failed to save properties to " + newPropertiesFilePath, e);
    }
    this.propertiesFilePath = newPropertiesFilePath;
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
  protected void collectVariables(Set<String> variableNames) {

    variableNames.addAll(this.variables.keySet());
    super.collectVariables(variableNames);
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

    return this.propertiesFilePath;
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

}

package com.devonfw.tools.ide.environment;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.log.IdeLogger;

/**
 * Abstract base implementation of {@link EnvironmentVariables}.
 */
public abstract class AbstractEnvironmentVariables implements EnvironmentVariables {

  /** @see #getParent() */
  protected final AbstractEnvironmentVariables parent;

  /** The {@link IdeLogger} instance. */
  protected final IdeLogger logger;

  private String source;

  /**
   * The constructor.
   *
   * @param parent the parent {@link EnvironmentVariables} to inherit from.
   * @param logger the {@link IdeLogger}.
   */
  public AbstractEnvironmentVariables(AbstractEnvironmentVariables parent, IdeLogger logger) {

    super();
    this.parent = parent;
    if (logger == null) {
      if (parent == null) {
        throw new IllegalArgumentException("parent and logger must not both be null!");
      }
      this.logger = parent.logger;
    } else {
      this.logger = logger;
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

    Set<String> variableNames = new HashSet<>();
    collectVariables(variableNames);
    List<VariableLine> variables = new ArrayList<>(variableNames.size());
    for (String name : variableNames) {
      boolean export = isExported(name);
      String value = get(name);
      variables.add(VariableLine.of(export, name, value));
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

    return new EnvironmentVariablesPropertiesFile(this, type, propertiesFilePath, this.logger);
  }

  /**
   * @return a new child {@link EnvironmentVariables} that will resolve variables recursively or this instance itself if
   *         already satisfied.
   */
  public EnvironmentVariables resolved() {

    return new EnvironmentVariablesResolved(this);
  }

  @Override
  public String toString() {

    return getSource();
  }

}

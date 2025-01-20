package com.devonfw.tools.ide.property;

import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.validation.PropertyValidator;

/**
 * Extends {@link FileProperty} for repository properties config file with auto-completion.
 */
public class RepositoryProperty extends FileProperty {

  public static final String EXTENSION_PROPERTIES = ".properties";

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public RepositoryProperty(String name, boolean required, String alias) {

    super(name, required, alias, true);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   * @param validator the {@link PropertyValidator} used to {@link #validate() validate} the {@link #getValue() value}.
   */
  public RepositoryProperty(String name, boolean required, String alias, PropertyValidator<Path> validator) {

    super(name, required, alias, true, validator);
  }

  @Override
  public Path parse(String valueAsString, IdeContext context) {

    if (valueAsString == null) {
      return null;
    }
    Path repositoriesPath = null;
    Path repositoryFile = Path.of(valueAsString);
    if (!Files.exists(repositoryFile)) {
      repositoryFile = null;
      repositoriesPath = context.getRepositoriesPath();
      if (repositoriesPath != null) {
        String propertiesFileName = valueAsString;
        if (!valueAsString.endsWith(EXTENSION_PROPERTIES)) {
          propertiesFileName += EXTENSION_PROPERTIES;
        }
        Path resolvedRepositoriesFile = repositoriesPath.resolve(propertiesFileName);
        if (Files.exists(resolvedRepositoriesFile)) {
          repositoryFile = resolvedRepositoriesFile;
        }
      }
    }
    if (repositoryFile == null) {
      throw new IllegalStateException("Could not find properties file: " + valueAsString + " in " + repositoriesPath);
    }
    return repositoryFile;
  }

  @Override
  protected void completeValue(String arg, IdeContext context, Commandlet commandlet, CompletionCandidateCollector collector) {

    Path repositoriesPath = context.getRepositoriesPath();
    if (repositoriesPath != null) {
      completeValuesFromFolder(repositoriesPath, arg, context, commandlet, collector);
    }
  }

  @Override
  protected String getPathForCompletion(Path path, IdeContext context, Commandlet commandlet) {

    String filename = path.getFileName().toString();
    if (filename.endsWith(EXTENSION_PROPERTIES)) {
      filename = filename.substring(0, filename.length() - EXTENSION_PROPERTIES.length());
    }
    return filename;
  }

}

package com.devonfw.tools.ide.property;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Consumer;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Extends {@link FileProperty} for repository properties config file with auto-completion.
 */
public class RepositoryProperty extends FileProperty {

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
   * @param validator the {@link Consumer} used to {@link #validate() validate} the {@link #getValue() value}.
   */
  public RepositoryProperty(String name, boolean required, String alias, Consumer<Path> validator) {

    super(name, required, alias, true, validator);
  }

  @Override
  public Path parse(String valueAsString, IdeContext context) {

    if (valueAsString == null) {
      return null;
    }

    Path repositoryFile = Path.of(valueAsString);
    if (!Files.exists(repositoryFile)) {
      Path repositoriesPath = context.getSettingsPath().resolve(IdeContext.FOLDER_REPOSITORIES);
      Path legacyRepositoriesPath = context.getSettingsPath().resolve(IdeContext.FOLDER_LEGACY_REPOSITORIES);
      String propertiesFileName = valueAsString;
      if (!valueAsString.endsWith(".properties")) {
        propertiesFileName += ".properties";
      }
      repositoryFile = context.getFileAccess().findExistingFile(propertiesFileName,
          Arrays.asList(repositoriesPath, legacyRepositoriesPath));
    }
    if (repositoryFile == null) {
      throw new IllegalStateException("Could not find properties file: " + valueAsString);
    }
    return repositoryFile;
  }

}

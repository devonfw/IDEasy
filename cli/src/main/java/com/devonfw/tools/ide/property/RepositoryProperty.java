package com.devonfw.tools.ide.property;

import com.devonfw.tools.ide.context.IdeContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Consumer;

public class RepositoryProperty extends Property<String> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public RepositoryProperty(String name, boolean required, String alias) {

    this(name, required, alias, null);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   * @param validator the {@link Consumer} used to {@link #validate() validate} the {@link #getValue() value}.
   */
  public RepositoryProperty(String name, boolean required, String alias, Consumer<String> validator) {

    super(name, required, alias, validator);
  }

  @Override
  public Class<String> getValueType() {

    return String.class;
  }

  @Override
  public String parse(String valueAsString, IdeContext context) {

    return null;
  }

  public Path getValueAsPath(IdeContext context) {

    String value = getValue();
    if (value == null) {
      return null;
    }

    Path repositoryFile = Path.of(value);
    if (!Files.exists(repositoryFile)) {
      Path repositoriesPath = context.getSettingsPath().resolve(IdeContext.FOLDER_REPOSITORIES);
      Path legacyRepositoriesPath = context.getSettingsPath().resolve(IdeContext.FOLDER_LEGACY_REPOSITORIES);
      repositoryFile = context.getFileAccess().findExistingFile(value + ".properties",
          Arrays.asList(repositoriesPath, legacyRepositoriesPath));
    }
    if (repositoryFile == null) {
      throw new IllegalStateException("Could not find " + value);
    }
    return repositoryFile;
  }

}

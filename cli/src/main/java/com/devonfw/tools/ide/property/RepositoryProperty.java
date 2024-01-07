package com.devonfw.tools.ide.property;

import com.devonfw.tools.ide.context.IdeContext;

import java.nio.file.Files;
import java.nio.file.Path;
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
  public String parse(String valueAsString) {

    return valueAsString;
  }

  public Path getValueAsPath(IdeContext context) {

    Path repositoriesPath = context.getSettingsPath().resolve(IdeContext.FOLDER_REPOSITORIES);
    Path legacyRepositoriesPath = context.getSettingsPath().resolve(IdeContext.FOLDER_LEGACY_REPOSITORIES);

    String value = getValue();
    if (value == null) {
      return null;
    }
    Path repositoryFile = Path.of(value);
    
    if (!Files.exists(repositoryFile)) {
      repositoryFile = repositoriesPath.resolve(value + ".properties");
    }
    if (!Files.exists(repositoryFile)) {
      Path legacyRepositoryFile = legacyRepositoriesPath.resolve(repositoryFile.getFileName().toString());
      if (Files.exists(legacyRepositoryFile)) {
        repositoryFile = legacyRepositoryFile;
      } else {
        throw new IllegalStateException("Could not find " + repositoryFile);
      }
    }
    return repositoryFile;
  }


}

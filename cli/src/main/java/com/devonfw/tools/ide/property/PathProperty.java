package com.devonfw.tools.ide.property;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * {@link Property} with {@link Path} as {@link #getValueType() value type}.
 */
public class PathProperty extends Property<Path> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public PathProperty(String name, boolean required, String alias) {

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
  public PathProperty(String name, boolean required, String alias, Consumer<Path> validator) {

    super(name, required, alias, validator);
  }

  @Override
  public Class<Path> getValueType() {

    return Path.class;
  }

  @Override
  public Path parse(String valueAsString) {

    return Paths.get(valueAsString);
  }

}

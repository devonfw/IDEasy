package com.devonfw.tools.ide.property;

import java.util.function.Consumer;

import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link Property} for {@link VersionIdentifier} as {@link #getValueType() value type}.
 */
public class VersionProperty extends Property<VersionIdentifier> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public VersionProperty(String name, boolean required, String alias) {

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
  public VersionProperty(String name, boolean required, String alias, Consumer<VersionIdentifier> validator) {

    super(name, required, alias, validator);
  }

  @Override
  public Class<VersionIdentifier> getValueType() {

    return VersionIdentifier.class;
  }

  @Override
  public VersionIdentifier parse(String valueAsString) {

    return VersionIdentifier.of(valueAsString);
  }

}

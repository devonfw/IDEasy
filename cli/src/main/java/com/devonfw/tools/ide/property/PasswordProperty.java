package com.devonfw.tools.ide.property;

/**
 * {@link Property} with {@link #getValueType() value type} {@link String} representing a password.
 */
public class PasswordProperty extends StringProperty {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public PasswordProperty(String name, boolean required, String alias) {

    super(name, required, alias, false);
  }

}

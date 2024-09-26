package com.devonfw.tools.ide.property;

/**
 * {@link BooleanProperty} for a flag-option (e.g. "--force").
 */
public class FlagProperty extends BooleanProperty {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   */
  public FlagProperty(String name) {
    this(name, false);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   */
  public FlagProperty(String name, boolean required) {
    this(name, required, null);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public FlagProperty(String name, boolean required, String alias) {

    super(name, required, alias);
    assert isOption();
  }

  @Override
  public boolean isExpectValue() {

    return false;
  }

}

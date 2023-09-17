package com.devonfw.tools.ide.property;

/**
 * {@link BooleanProperty} for a keyword (e.g. "install" in "ide install mvn 3.9.4").
 */
public class KeywordProperty extends BooleanProperty {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public KeywordProperty(String name, boolean required, String alias) {

    super(name, required, alias);
    assert (!name.isEmpty() && isValue());
  }

  @Override
  public boolean isExpectValue() {

    return false;
  }

}

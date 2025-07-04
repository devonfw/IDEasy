package com.devonfw.tools.ide.io;

/**
 * Implementation of {@link IniProperty}
 */
public class IniPropertyImpl implements IniProperty {

  String key;
  String value;

  /**
   * creates a new IniPropertyImpl
   *
   * @param key property key
   * @param value property value
   */
  public IniPropertyImpl(String key, String value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "\t" + key + " = " + value;
  }
}

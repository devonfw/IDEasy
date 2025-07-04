package com.devonfw.tools.ide.io;

/**
 * Implementation of {@link IniProperty}
 */
public class IniPropertyImpl implements IniProperty {

  String key;
  String value;
  int indentLevel;

  /**
   * creates a new IniPropertyImpl
   *
   * @param key property key
   * @param value property value
   * @param indentLevel the indentation level
   */
  public IniPropertyImpl(String key, String value, int indentLevel) {
    this.key = key;
    this.value = value;
    this.indentLevel = indentLevel;
  }

  /**
   * creates a new IniPropertyImpl with indentation level 0
   *
   * @param key property key
   * @param value property value
   */
  public IniPropertyImpl(String key, String value) {
    this(key, value, 0);
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
  public int getIndentLevel() {
    return indentLevel;
  }

  @Override
  public String toString() {
    return key + " = " + value;
  }
}

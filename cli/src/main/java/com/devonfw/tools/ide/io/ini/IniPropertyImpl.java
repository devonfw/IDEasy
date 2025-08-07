package com.devonfw.tools.ide.io.ini;

/**
 * Implementation of {@link IniProperty}
 */
public class IniPropertyImpl extends IniProperty {

  private final String key;
  private final String value;

  /**
   * creates a new IniPropertyImpl with indentation level 0
   *
   * @param content String representation of the property in the file
   */
  public IniPropertyImpl(String content) {
    this.setContent(content);
    int index = content.indexOf('=');
    if (index <= 0) {
      throw new IllegalArgumentException("Ini Property declaration must contain a '=' symbol");
    }
    key = content.substring(0, index).trim();
    value = content.substring(index + 1).trim();
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
  public String toString() {
    return this.getContent();
  }
}

package com.devonfw.tools.ide.io.ini;

/**
 * Implementation of {@link IniProperty}
 */
public class IniProperty extends IniElement {

  private final String key;
  private String value;

  /**
   * creates a new IniPropertyImpl with indentation level 0
   *
   * @param content String representation of the property in the file
   */
  public IniProperty(String content) {
    this.setContent(content);
    int index = content.indexOf('=');
    if (index <= 0) {
      throw new IllegalArgumentException("Ini Property declaration must contain a '=' symbol");
    }
    key = content.substring(0, index).trim();
    value = content.substring(index + 1).trim();
  }

  /**
   * @return property key
   */
  public String getKey() {
    return key;
  }

  /**
   * @return property value
   */
  public String getValue() {
    return value;
  }

  /**
   * updates the property value and the content
   *
   * @param value new property value
   */
  public void setValue(String value) {
    this.value = value;
    String indentation = this.getIndentation();
    StringBuilder stringBuilder = new StringBuilder(indentation);
    stringBuilder.append(this.key);
    stringBuilder.append(" = ");
    stringBuilder.append(this.value);
    this.setContent(stringBuilder.toString());
  }

  @Override
  public String toString() {
    return this.getContent();
  }
}

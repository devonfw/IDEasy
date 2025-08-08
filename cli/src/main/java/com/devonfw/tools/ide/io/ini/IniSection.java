package com.devonfw.tools.ide.io.ini;


import java.util.List;

/**
 * container for a section in an {@link IniFile}
 */
public abstract class IniSection extends IniElement {

  /**
   * @return the section name
   */
  public abstract String getName();

  /**
   * @return list of property keys
   */
  public List<String> getPropertyKeys() {
    return null;
  }

  /**
   * @param key the property key
   * @return the property with the given key
   */
  public abstract String getPropertyValue(String key);

  /**
   * Add or update a property in the section
   *
   * @param content the entire file line containing the property
   */
  public abstract void setProperty(String content);

  /**
   * Add or update a property in the section
   *
   * @param key property key
   * @param value property value
   */
  public abstract void setProperty(String key, String value);

  /**
   * Add a comment to the section
   *
   * @param comment the comment
   */
  public abstract void addComment(String comment);
}

package com.devonfw.tools.ide.io;


import java.util.List;

/**
 * container for a section in an {@link IniFile}
 */
public interface IniSection extends IniElement {

  /**
   * @return the section name
   */
  String getName();

  /**
   * @return list of property keys
   */
  List<String> getPropertyKeys();

  /**
   * @param key the property key
   * @return the property with the given key
   */
  IniProperty getProperty(String key);

  /**
   * Add a property to the section
   *
   * @param key property key
   * @param value property value
   * @param indentLevel indentation level
   */
  void setProperty(String key, String value, int indentLevel);

  /**
   * Add a comment to the section
   *
   * @param comment the comment
   * @param indentLevel indentation level
   */
  void addComment(String comment, int indentLevel);
}

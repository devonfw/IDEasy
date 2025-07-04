package com.devonfw.tools.ide.io;

/**
 * Container for a property in an {@link IniFile}
 */
public interface IniProperty extends IniElement {

  /**
   * @return property key
   */
  String getKey();

  /**
   * @return property value
   */
  String getValue();

  /**
   * sets property value
   *
   * @param value new value
   */
  void setValue(String value);
}

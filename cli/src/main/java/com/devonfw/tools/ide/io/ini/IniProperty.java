package com.devonfw.tools.ide.io.ini;

/**
 * Container for a property in an {@link IniFile}
 */
abstract class IniProperty extends IniElement {

  /**
   * @return property key
   */
  public abstract String getKey();

  /**
   * @return property value
   */
  String getValue() {
    return null;
  }
}

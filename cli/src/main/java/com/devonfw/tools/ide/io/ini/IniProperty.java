package com.devonfw.tools.ide.io.ini;

/**
 * Container for a property in an {@link IniFile}
 */
abstract class IniProperty extends IniElement {

  /**
   * @return property key
   */
  String getKey() {
    return null;
  }

  /**
   * @return property value
   */
  String getValue() {
    return null;
  }
}

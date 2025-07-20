package com.devonfw.tools.ide.io;


import java.util.Map;

/**
 * container for a section in an {@link IniFile}
 */
public interface IniSection {

  /**
   * @return the section name
   */
  String getName();

  /**
   * @return map of the section key-value pairs
   */
  Map<String, String> getProperties();
}

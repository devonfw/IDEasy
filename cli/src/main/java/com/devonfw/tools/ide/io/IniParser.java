package com.devonfw.tools.ide.io;

import java.nio.file.Path;
import java.util.HashMap;

/**
 * Interface that allows parsing of .ini files such as .gitignore files
 */
public interface IniParser {

  /**
   * @return String[] the sections
   */
  String[] getSections();

  /**
   * @param section the section
   * @return map of property key-value pairs in the given section
   */
  HashMap<String, String> getPropertiesBySection(String section);

  /**
   * @param section the section
   * @param property the property
   * @return value of the given property in the given section
   */
  String getPropertyValue(String section, String property);

  /**
   * @param section the section to remove
   */
  void removeSection(String section);

  /**
   * @param section the section containing the property
   * @param property the property to remove
   */
  void removeProperty(String section, String property);


  /**
   * @param section the section to add. Does nothing if section exists
   */
  void addSection(String section);

  /**
   * @param section the section to add the property in
   * @param property the property to set. Overrides if existing, adds otherwise
   * @param value the value of the property
   */
  void setProperty(String section, String property, String value);

  /**
   * Write the .ini file to the given path
   *
   * @param path the path to write to
   */
  void write(Path path);
}

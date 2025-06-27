package com.devonfw.tools.ide.io;

/**
 * Interface that allows parsing of .ini files such as .gitignore files
 */
public interface IniFile {

  /**
   * @return the names of the available sections from this {@link IniFile}.
   */
  String[] getSectionNames();

  /**
   * @param section the section to remove
   * @return {@code true} if section existed, {@code false} otherwise
   */
  boolean removeSection(String section);

  /**
   * @param section the section to get
   * @return {@link IniSection} if section exists, null otherwise
   */
  IniSection getSection(String section);

  /**
   * @param section the section to get or create
   * @return existing or newly created {@link IniSection}
   */
  IniSection getOrCreateSection(String section);
}

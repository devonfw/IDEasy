package com.devonfw.tools.ide.io;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation of {@link IniSection}
 */
public class IniSectionImpl implements IniSection {

  private final String name;
  private final Map<String, String> properties;

  /**
   * creates IniSectionImpl with given name and properties
   *
   * @param name section name
   * @param properties section properties
   */
  public IniSectionImpl(String name, Map<String, String> properties) {
    this.name = name;
    this.properties = properties;
  }

  /**
   * creates IniSectionImpl with given name and empty properties
   *
   * @param name section name
   */
  public IniSectionImpl(String name) {
    this(name, new LinkedHashMap<>());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }
}

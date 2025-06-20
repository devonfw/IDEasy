package com.devonfw.tools.ide.io;

import java.util.LinkedHashMap;

/**
 * Implementation of {@link IniFile} preserves order of sections and properties between reading and writing
 */
public class IniFileImpl implements IniFile {

  LinkedHashMap<String, IniSection> iniMap;

  /**
   * creates empty IniFileImpl
   */
  public IniFileImpl() {
    this.iniMap = new LinkedHashMap<>();
  }

  @Override
  public String[] getSectionNames() {
    return iniMap.keySet().toArray(new String[0]);
  }

  @Override
  public boolean removeSection(String section) {
    boolean sectionExists = iniMap.containsKey(section);
    iniMap.remove(section);
    return sectionExists;
  }

  @Override
  public IniSection getSection(String section) {
    return iniMap.get(section);
  }

  @Override
  public IniSection getOrCreateSection(String section) {
    IniSection iniSection;
    if (!iniMap.containsKey(section)) {
      iniSection = new IniSectionImpl(section);
      iniMap.put(section, iniSection);
    } else {
      iniSection = iniMap.get(section);
    }
    return iniSection;
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    for (String configSection : iniMap.keySet()) {
      stringBuilder.append(String.format("[%s]\n", configSection));
      LinkedHashMap<String, String> properties = (LinkedHashMap<String, String>) iniMap.get(configSection).getProperties();
      for (String sectionProperty : properties.keySet()) {
        String propertyValue = properties.get(sectionProperty);
        stringBuilder.append(String.format("\t%s = %s\n", sectionProperty, propertyValue));
      }
    }
    return stringBuilder.toString();
  }
}

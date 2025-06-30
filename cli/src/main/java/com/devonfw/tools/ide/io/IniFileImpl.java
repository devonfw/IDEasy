package com.devonfw.tools.ide.io;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Implementation of {@link IniFile} preserves order of sections and properties between reading and writing
 */
public class IniFileImpl implements IniFile {

  private final Map<String, IniSection> iniMap;

  /**
   * creates empty IniFileImpl
   */
  public IniFileImpl() {
    this.iniMap = new LinkedHashMap<>();
  }

  @Override
  public String[] getSectionNames() {
    return iniMap.keySet().toArray(String[]::new);
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
    return this.iniMap.computeIfAbsent(section, IniSectionImpl::new);
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    for (String configSection : iniMap.keySet()) {
      stringBuilder.append('[');
      stringBuilder.append(configSection);
      stringBuilder.append("]\n");
      Map<String, String> properties = iniMap.get(configSection).getProperties();
      for (Entry<String, String> entry : properties.entrySet()) {
        stringBuilder.append('\t');
        stringBuilder.append(entry.getKey());
        stringBuilder.append(" = ");
        stringBuilder.append(entry.getValue());
        stringBuilder.append('\n');
      }
    }
    return stringBuilder.toString();
  }
}

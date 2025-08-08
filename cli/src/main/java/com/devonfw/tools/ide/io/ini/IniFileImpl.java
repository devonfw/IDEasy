package com.devonfw.tools.ide.io.ini;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link IniFile} preserves order of sections and properties between reading and writing
 */
public class IniFileImpl implements IniFile {

  private final Map<String, IniSection> iniMap;

  private final List<IniElement> fileElements;

  /**
   * creates empty IniFileImpl
   */
  public IniFileImpl() {
    this.iniMap = new LinkedHashMap<>();
    this.fileElements = new LinkedList<>();

    IniSection initialSection = new IniSection("");
    iniMap.put(null, initialSection);
    fileElements.add(initialSection);
  }

  @Override
  public String[] getSectionNames() {
    return iniMap.keySet().stream().filter(Objects::nonNull).toArray(String[]::new);
  }

  @Override
  public boolean removeSection(String section) {
    boolean sectionExists = iniMap.containsKey(section);
    IniSection removedSection = iniMap.remove(section);
    fileElements.remove(removedSection);
    return sectionExists;
  }

  @Override
  public IniSection getSection(String section) {
    return iniMap.get(section);
  }

  @Override
  public IniSection getInitialSection() {
    return iniMap.get(null);
  }

  @Override
  public IniSection getOrCreateSection(String sectionHeading) {
    if (!sectionHeading.contains("[") || !sectionHeading.contains("]")) {
      sectionHeading = "[" + sectionHeading + "]";
    }
    IniSection iniSection = new IniSection(sectionHeading);
    String sectionName = iniSection.getName();
    if (iniMap.containsKey(sectionName)) {
      return iniMap.get(sectionName);
    } else {
      iniMap.put(sectionName, iniSection);
      fileElements.add(iniSection);
      return iniSection;
    }
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    for (IniElement element : fileElements) {
      String elementString = element.toString();
      stringBuilder.append(element);
      if (!elementString.isEmpty()) {
        stringBuilder.append("\n");
      }
    }
    return stringBuilder.toString();
  }
}

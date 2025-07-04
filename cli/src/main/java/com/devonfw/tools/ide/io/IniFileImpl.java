package com.devonfw.tools.ide.io;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link IniFile} preserves order of sections and properties between reading and writing
 */
public class IniFileImpl implements IniFile {

  private final Map<String, IniSection> iniMap;

  private final Map<String, IniProperty> properties;

  private final List<IniElement> fileElements;

  /**
   * creates empty IniFileImpl
   */
  public IniFileImpl() {
    this.iniMap = new LinkedHashMap<>();
    this.fileElements = new LinkedList<>();
    this.properties = new LinkedHashMap<>();
  }

  @Override
  public String[] getSectionNames() {
    return iniMap.keySet().toArray(String[]::new);
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
  public IniSection getOrCreateSection(String section) {
    if (iniMap.containsKey(section)) {
      return iniMap.get(section);
    } else {
      IniSection newSection = new IniSectionImpl(section);
      iniMap.put(section, newSection);
      fileElements.add(newSection);
      return newSection;
    }
  }

  @Override
  public void setProperty(String key, String value) {
    IniProperty property = new IniPropertyImpl(key, value);
    fileElements.add(property);
    properties.put(key, property);
  }

  @Override
  public IniProperty getProperty(String key) {
    return properties.get(key);
  }

  @Override
  public void addComment(String comment, int indentLevel) {
    fileElements.add(new IniCommentImpl(comment, indentLevel));
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    for (IniElement element : fileElements) {
      stringBuilder.append("\t".repeat(Math.max(0, element.getIndentLevel())));
      stringBuilder.append(element).append("\n");
    }
    return stringBuilder.toString();
  }
}

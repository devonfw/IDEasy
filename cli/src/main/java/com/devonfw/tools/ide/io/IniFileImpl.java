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

  private final List<IniElement> fileElements;

  /**
   * creates empty IniFileImpl
   */
  public IniFileImpl() {
    this.iniMap = new LinkedHashMap<>();
    this.fileElements = new LinkedList<>();
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
  public void addProperty(String key, String value) {
    fileElements.add(new IniPropertyImpl(key, value));
  }

  @Override
  public void addComment(String comment) {
    fileElements.add(new IniCommentImpl(comment));
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    for (IniElement element : fileElements) {
      stringBuilder.append(element.toString()).append("\n");
    }
    return stringBuilder.toString();
  }
}

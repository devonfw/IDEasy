package com.devonfw.tools.ide.io;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link IniSection}
 */
public class IniSectionImpl implements IniSection {

  private final String name;
  private final Map<String, IniProperty> properties;
  private final List<IniElement> sectionElements;

  /**
   * creates IniSectionImpl with given name and properties
   *
   * @param name section name
   * @param sectionElements list of elements in section
   */
  public IniSectionImpl(String name, List<IniElement> sectionElements) {
    this.name = name;
    this.sectionElements = sectionElements;
    this.properties = new LinkedHashMap<>();
    for (IniElement element : sectionElements) {
      if (element instanceof IniProperty property) {
        properties.put(property.getKey(), property);
      }
    }
  }

  /**
   * creates IniSectionImpl with given name and empty properties
   *
   * @param name section name
   */
  public IniSectionImpl(String name) {
    this(name, new LinkedList<>());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<String> getPropertyKeys() {
    return properties.keySet().stream().toList();
  }

  @Override
  public IniProperty getProperty(String key) {
    return properties.get(key);
  }

  @Override
  public void setProperty(String key, String value, int indentLevel) {
    IniProperty property = new IniPropertyImpl(key, value, indentLevel);
    sectionElements.add(property);
    properties.put(key, property);
  }

  @Override
  public void addComment(String comment, int indentLevel) {
    sectionElements.add(new IniCommentImpl(comment, indentLevel));
  }

  @Override
  public int getIndentLevel() {
    return 0;
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[").append(name).append("]").append("\n");
    for (int i = 0; i < sectionElements.size(); i++) {
      IniElement element = sectionElements.get(i);
      stringBuilder.append("\t".repeat(Math.max(0, element.getIndentLevel())));
      stringBuilder.append(element);
      if (i + 1 < sectionElements.size()) {
        stringBuilder.append("\n");
      }
    }
    return stringBuilder.toString();
  }
}

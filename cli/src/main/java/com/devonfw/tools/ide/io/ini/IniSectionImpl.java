package com.devonfw.tools.ide.io.ini;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link IniSection}
 */
public class IniSectionImpl extends IniSection {

  private final String name;
  private final Map<String, IniProperty> properties;
  private final List<IniElement> sectionElements;

  /**
   * creates {@link IniSectionImpl} from given section heading
   *
   * @param heading the heading line of the section
   */
  public IniSectionImpl(String heading) {
    this.setContent(heading);
    this.name = heading.replace("[", "").replace("]", "").trim();
    this.properties = new LinkedHashMap<>();
    this.sectionElements = new ArrayList<>();
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
  public String getPropertyValue(String key) {
    return properties.get(key).getValue();
  }

  @Override
  public void setProperty(String content) {
    IniProperty property = new IniPropertyImpl(content);
    sectionElements.add(property);
    properties.put(property.getKey(), property);
  }

  @Override
  public void setProperty(String key, String value) {
    String indentation = this.getIndentation();
    if (this.getPropertyKeys().contains(key)) {
      indentation = properties.get(key).getIndentation();
    } else if (!this.getPropertyKeys().isEmpty()) {
      indentation = properties.get(this.getPropertyKeys().getFirst()).getIndentation();
    }
    StringBuilder stringBuilder = new StringBuilder(indentation);
    stringBuilder.append(key);
    stringBuilder.append(" = ");
    stringBuilder.append(value);
    String propertyContent = stringBuilder.toString();
    setProperty(propertyContent);
  }

  @Override
  public void addComment(String comment) {
    sectionElements.add(new IniCommentImpl(comment));
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    if (!name.isEmpty()) {
      stringBuilder.append(this.getContent());
      stringBuilder.append("\n");
    }
    for (int i = 0; i < sectionElements.size(); i++) {
      IniElement element = sectionElements.get(i);
      stringBuilder.append(element);
      if (i + 1 < sectionElements.size()) {
        stringBuilder.append("\n");
      }
    }
    return stringBuilder.toString();
  }
}

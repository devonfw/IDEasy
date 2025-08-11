package com.devonfw.tools.ide.io.ini;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link IniSection}
 */
public class IniSection extends IniElement {

  private final String name;
  private final Map<String, IniProperty> properties;
  private final List<IniElement> sectionElements;

  /**
   * creates {@link IniSection} from given section heading
   *
   * @param heading the heading line of the section
   */
  public IniSection(String heading) {
    if (!heading.isEmpty() && (!heading.contains("[") || !heading.contains("]"))) {
      throw new IllegalArgumentException("Section name must be surrounded by brackets");
    }
    this.setContent(heading);
    this.name = heading.replace("[", "").replace("]", "").trim();
    this.properties = new LinkedHashMap<>();
    this.sectionElements = new ArrayList<>();
  }

  /**
   * @return the section name
   */
  public String getName() {
    return name;
  }

  /**
   * @return list of property keys
   */
  public List<String> getPropertyKeys() {
    return properties.keySet().stream().toList();
  }

  /**
   * @param key the property key
   * @return the property with the given key
   */
  public String getPropertyValue(String key) {
    return properties.get(key).getValue();
  }

  /**
   * Add or update a property in the section
   *
   * @param content the entire file line containing the property
   */
  public void setProperty(String content) {
    IniProperty property = new IniProperty(content);
    sectionElements.add(property);
    properties.put(property.getKey(), property);
  }

  /**
   * Add or update a property in the section
   *
   * @param key property key
   * @param value property value
   */
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

  /**
   * Add a comment to the section
   *
   * @param comment the comment
   */
  public void addComment(String comment) {
    sectionElements.add(new IniComment(comment));
  }

  @Override
  void write(StringBuilder stringBuilder) {
    if (!name.isEmpty()) {
      super.write(stringBuilder);
    } else {
      stringBuilder.append(this);
    }
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    if (!name.isEmpty()) {
      stringBuilder.append(this.getContent());
      if (!sectionElements.isEmpty()) {
        stringBuilder.append("\n");
      }
    }
    for (IniElement element : sectionElements) {
      stringBuilder.append(element);
      stringBuilder.append("\n");
    }
    return stringBuilder.toString();
  }
}

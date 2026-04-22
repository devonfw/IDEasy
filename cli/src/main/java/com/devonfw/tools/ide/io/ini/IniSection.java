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
   * @param key property key
   * @param value property value
   */
  public void setProperty(String key, String value) {
    IniProperty existingProperty = properties.get(key);
    if (existingProperty == null) {
      String indentation = this.getIndentation();
      if (!this.getPropertyKeys().isEmpty()) {
        indentation = properties.get(this.getPropertyKeys().getFirst()).getIndentation();
      }
      StringBuilder stringBuilder = new StringBuilder(indentation);
      stringBuilder.append(key);
      stringBuilder.append(" = ");
      stringBuilder.append(value);
      IniProperty property = new IniProperty(stringBuilder.toString());
      sectionElements.add(property);
      properties.put(key, property);
    } else {
      existingProperty.setValue(value);
    }
  }

  /**
   * Add or update a property in the section from a property line
   *
   * @param keyValue the property line as it appears in the file
   */
  public void setPropertyLine(String keyValue) {
    IniProperty property = new IniProperty(keyValue);
    setProperty(property.getKey(), property.getValue());
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
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    if (!name.isEmpty()) {
      stringBuilder.append(this.getContent());
      stringBuilder.append("\n");
    }
    for (IniElement element : sectionElements) {
      stringBuilder.append(element);
      stringBuilder.append("\n");
    }
    return stringBuilder.toString();
  }
}

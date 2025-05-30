package com.devonfw.tools.ide.io;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Implementation of {@link IniParser} preserves order of sections and properties between reading and writing
 */
public class IniParserImpl implements IniParser {

  LinkedHashMap<String, LinkedHashMap<String, String>> iniMap;
  IdeContext context;

  /**
   * @param path the path of the file
   * @param context the IdeContext
   */
  public IniParserImpl(Path path, IdeContext context) {
    this.context = context;
    parse(path);
  }

  /**
   * @param content the file content
   * @param context the IdeContext
   */
  public IniParserImpl(String content, IdeContext context) {
    this.context = context;
    parse(content);
  }

  private void parse(String content) {
    iniMap = new LinkedHashMap<>();
    List<String> iniLines = content.lines().toList();
    String currentSection = "";
    for (String line : iniLines) {
      if (line.isEmpty()) {
        continue;
      }
      if (line.startsWith("[")) {
        currentSection = line.replace("[", "").replace("]", "");
        iniMap.put(currentSection, new LinkedHashMap<>());
      } else {
        String[] parts = line.split("=");
        String propertyName = parts[0].trim();
        String propertyValue = parts[1].trim();
        iniMap.get(currentSection).put(propertyName, propertyValue);
      }
    }
  }

  private void parse(Path path) {
    FileAccess fileAccess = this.context.getFileAccess();
    String fileContent = fileAccess.readFileContent(path);

    if (fileContent == null) {
      fileContent = "";
    }

    parse(fileContent);
  }

  @Override
  public String[] getSections() {
    return iniMap.keySet().toArray(new String[0]);
  }

  @Override
  public HashMap<String, String> getPropertiesBySection(String section) {
    return iniMap.get(section);
  }

  @Override
  public String getPropertyValue(String section, String property) {
    return iniMap.get(section).get(property);
  }

  @Override
  public void removeSection(String section) {
    iniMap.remove(section);
  }

  @Override
  public void removeProperty(String section, String property) {
    iniMap.get(section).remove(property);
  }

  @Override
  public void addSection(String section) {
    if (!iniMap.containsKey(section)) {
      iniMap.put(section, new LinkedHashMap<>());
    }
  }

  @Override
  public void setProperty(String section, String property, String value) {
    if (!iniMap.containsKey(section)) {
      addSection(section);
    }
    iniMap.get(section).put(property, value);
  }

  @Override
  public void write(Path path) {
    String iniString = this.toString();
    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.writeFileContent(iniString, path);
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    for (String configSection : iniMap.keySet()) {
      stringBuilder.append(String.format("[%s]\n", configSection));
      LinkedHashMap<String, String> properties = iniMap.get(configSection);
      for (String sectionProperty : properties.keySet()) {
        String propertyValue = properties.get(sectionProperty);
        stringBuilder.append(String.format("\t%s = %s\n", sectionProperty, propertyValue));
      }
    }
    return stringBuilder.toString();
  }
}

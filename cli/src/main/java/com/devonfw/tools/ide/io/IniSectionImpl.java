package com.devonfw.tools.ide.io;

import java.util.LinkedHashMap;
import java.util.Map;

public class IniSectionImpl implements IniSection {

  String name;
  LinkedHashMap<String, String> properties;

  public IniSectionImpl(String name, Map<String, String> properties) {
    this.name = name;
    this.properties = (LinkedHashMap<String, String>) properties;
  }

  public IniSectionImpl(String name) {
    this.name = name;
    this.properties = new LinkedHashMap<>();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }
}

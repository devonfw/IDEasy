package com.devonfw.tools.ide.io;


import java.util.Map;

public interface IniSection {

  String getName();

  Map<String, String> getProperties();
}

package com.devonfw.tools.ide.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class PropertiesFileUtil {

  /**
   * Loads properties from a file at the given path.
   *
   * @param path the path to the properties file
   * @return a Properties object loaded with properties from the file
   */
  public static Properties loadProperties(Path path) {

    Properties properties = new Properties();

    try (var inputStream = Files.newInputStream(path)) {
      properties.load(inputStream);
    } catch (IOException e) {
      throw new IllegalArgumentException(String.format("Cannot read Properties File at %s", path.toString()), e);
    }
    return properties;
  }
}

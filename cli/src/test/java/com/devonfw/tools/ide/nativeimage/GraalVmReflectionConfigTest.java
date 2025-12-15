package com.devonfw.tools.ide.nativeimage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.json.JsonObject;
import com.devonfw.tools.ide.json.JsonVersionItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test to validate that all JSON classes used for deserialization are properly registered in GraalVM reflection
 * configuration. This prevents runtime failures in native image builds where Jackson cannot deserialize JSON without
 * explicit reflection registration.
 */
public class GraalVmReflectionConfigTest {

  private static final String REFLECT_CONFIG_PATH = "META-INF/native-image/com.devonfw.tools.IDEasy/ide-cli/reflect-config.json";

  private static final String BASE_PACKAGE = "com/devonfw/tools/ide";

  /**
   * Tests that all classes implementing {@link JsonObject} or {@link JsonVersionItem} are registered in the GraalVM
   * reflection configuration file. These classes require reflection for Jackson JSON deserialization to work in native
   * image builds.
   *
   * @throws Exception if the test fails
   */
  @Test
  public void testAllJsonClassesRegisteredForReflection() throws Exception {

    // Find all classes implementing JsonObject or JsonVersionItem
    Set<String> jsonClasses = findJsonClasses();

    // Load reflection configuration
    Set<String> registeredClasses = loadReflectionConfig();

    // Find missing classes
    Set<String> missingClasses = new HashSet<>(jsonClasses);
    missingClasses.removeAll(registeredClasses);

    // Generate helpful error message with JSON snippet to add
    if (!missingClasses.isEmpty()) {
      String errorMessage = buildErrorMessage(missingClasses);
      assertThat(missingClasses)
          .as(errorMessage)
          .isEmpty();
    }
  }

  /**
   * Scans the classpath for all classes in the com.devonfw.tools.ide package that implement {@link JsonObject} or
   * {@link JsonVersionItem}.
   *
   * @return set of fully qualified class names
   * @throws Exception if scanning fails
   */
  private Set<String> findJsonClasses() throws Exception {

    Set<String> classes = new HashSet<>();
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    // Get all class files in the base package
    Enumeration<URL> resources = classLoader.getResources(BASE_PACKAGE);
    while (resources.hasMoreElements()) {
      URL resource = resources.nextElement();
      classes.addAll(findClassesInResource(resource, BASE_PACKAGE));
    }

    // Filter for classes implementing JsonObject or JsonVersionItem
    Set<String> jsonClasses = new HashSet<>();
    for (String className : classes) {
      try {
        Class<?> clazz = Class.forName(className);
        if (JsonObject.class.isAssignableFrom(clazz) || JsonVersionItem.class.isAssignableFrom(clazz)) {
          // Don't include the interfaces themselves
          if (!clazz.equals(JsonObject.class) && !clazz.equals(JsonVersionItem.class)) {
            jsonClasses.add(className);
          }
        }
      } catch (ClassNotFoundException | NoClassDefFoundError e) {
        // Skip classes that cannot be loaded (e.g., optional dependencies)
      }
    }

    return jsonClasses;
  }

  /**
   * Finds all class files in a given resource URL (directory or JAR).
   *
   * @param resource the resource URL
   * @param packagePath the package path
   * @return set of fully qualified class names
   * @throws IOException if reading fails
   */
  private Set<String> findClassesInResource(URL resource, String packagePath) throws IOException {

    Set<String> classes = new HashSet<>();
    String protocol = resource.getProtocol();

    if ("file".equals(protocol)) {
      // File system directory - use URI to handle Windows paths correctly
      try {
        Path directory = Paths.get(resource.toURI());
        classes.addAll(findClassesInDirectory(directory, packagePath.replace('/', '.')));
      } catch (Exception e) {
        // Fallback: try to parse the path directly, removing leading slash on Windows
        String path = resource.getPath();
        if (path.startsWith("/") && path.length() > 2 && path.charAt(2) == ':') {
          path = path.substring(1); // Remove leading slash for Windows paths like /C:/...
        }
        Path directory = Paths.get(path);
        classes.addAll(findClassesInDirectory(directory, packagePath.replace('/', '.')));
      }
    } else if ("jar".equals(protocol)) {
      // JAR file
      String jarPath = resource.getPath();
      if (jarPath.contains("!")) {
        jarPath = jarPath.substring(0, jarPath.indexOf("!"));
      }
      if (jarPath.startsWith("file:")) {
        jarPath = jarPath.substring(5);
      }
      // Handle Windows paths
      if (jarPath.startsWith("/") && jarPath.length() > 2 && jarPath.charAt(2) == ':') {
        jarPath = jarPath.substring(1);
      }
      classes.addAll(findClassesInJar(jarPath, packagePath));
    }

    return classes;
  }

  /**
   * Finds all classes in a file system directory.
   *
   * @param directory the directory path
   * @param packageName the package name
   * @return set of fully qualified class names
   * @throws IOException if reading fails
   */
  private Set<String> findClassesInDirectory(Path directory, String packageName) throws IOException {

    Set<String> classes = new HashSet<>();

    if (Files.exists(directory)) {
      Files.walk(directory)
          .filter(path -> path.toString().endsWith(".class"))
          .forEach(path -> {
            String relativePath = directory.relativize(path).toString();
            String className = packageName + "."
                + relativePath.replace('\\', '.').replace('/', '.').replace(".class", "");
            classes.add(className);
          });
    }

    return classes;
  }

  /**
   * Finds all classes in a JAR file.
   *
   * @param jarPath the JAR file path
   * @param packagePath the package path
   * @return set of fully qualified class names
   * @throws IOException if reading fails
   */
  private Set<String> findClassesInJar(String jarPath, String packagePath) throws IOException {

    Set<String> classes = new HashSet<>();

    try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(Paths.get(jarPath)))) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        String name = entry.getName();
        if (name.startsWith(packagePath) && name.endsWith(".class")) {
          String className = name.replace('/', '.').replace(".class", "");
          classes.add(className);
        }
      }
    }

    return classes;
  }

  /**
   * Loads the GraalVM reflection configuration and extracts registered class names.
   *
   * @return set of registered class names
   * @throws Exception if loading fails
   */
  private Set<String> loadReflectionConfig() throws Exception {

    Set<String> registeredClasses = new HashSet<>();
    ObjectMapper mapper = JsonMapping.create();

    InputStream configStream = getClass().getClassLoader().getResourceAsStream(REFLECT_CONFIG_PATH);
    if (configStream == null) {
      throw new IllegalStateException("Could not find reflection config at: " + REFLECT_CONFIG_PATH);
    }

    JsonNode rootNode = mapper.readTree(configStream);
    if (rootNode.isArray()) {
      for (JsonNode node : rootNode) {
        if (node.has("name")) {
          String className = node.get("name").asText();
          registeredClasses.add(className);
        }
      }
    }

    return registeredClasses;
  }

  /**
   * Builds a helpful error message with the exact JSON snippet to add to the reflection config.
   *
   * @param missingClasses set of missing class names
   * @return formatted error message
   */
  private String buildErrorMessage(Set<String> missingClasses) {

    StringBuilder message = new StringBuilder();
    message.append("\n\n");
    message.append("====================================================================================\n");
    message.append("GraalVM Reflection Configuration Error\n");
    message.append("====================================================================================\n");
    message.append("\n");
    message.append("The following JSON classes are NOT registered in the GraalVM reflection config.\n");
    message.append("This will cause Jackson deserialization to fail in native image builds.\n");
    message.append("\n");
    message.append("Missing classes:\n");
    List<String> sortedClasses = missingClasses.stream().sorted().collect(Collectors.toList());
    for (String className : sortedClasses) {
      message.append("  - ").append(className).append("\n");
    }
    message.append("\n");
    message.append("====================================================================================\n");
    message.append("HOW TO FIX\n");
    message.append("====================================================================================\n");
    message.append("\n");
    message.append("Add the following entries to:\n");
    message.append("  cli/src/main/resources/META-INF/native-image/com.devonfw.tools.IDEasy/ide-cli/reflect-config.json\n");
    message.append("\n");
    message.append("Copy this JSON snippet into the array:\n");
    message.append("\n");

    for (String className : sortedClasses) {
      message.append("  {\n");
      message.append("    \"name\": \"").append(className).append("\",\n");
      message.append("    \"allDeclaredConstructors\": true,\n");
      message.append("    \"allPublicConstructors\": true,\n");
      message.append("    \"allDeclaredFields\": true,\n");
      message.append("    \"allDeclaredMethods\": true\n");
      message.append("  },\n");
    }

    message.append("\n");
    message.append("For detailed instructions, see:\n");
    message.append("  documentation/graalvm-build-guide.adoc#reflection-configuration\n");
    message.append("\n");
    message.append("====================================================================================\n");

    return message.toString();
  }
}

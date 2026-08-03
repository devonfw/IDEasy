package com.devonfw.tools.ide.json;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Interface for a mapper to {@link #loadJson(Path) load} or {@link #saveJson(Object, Path) save} a specific type from/to JSON.
 *
 * @param <T> type of the mapped object.
 */
public abstract class JsonObjectMapper<T extends JsonObject> {

  private final ObjectMapper mapper;

  private final Class<T> type;

  /**
   * The constructor.
   *
   * @param type the type of the mapped object.
   */
  protected JsonObjectMapper(Class<T> type) {
    super();
    this.type = type;
    this.mapper = JsonMapping.create();
  }

  /**
   * @param object the {@link JsonObject} to save as JSON.
   * @param path the {@link Path} of the file where to save the JSON.
   */
  public void saveJson(T object, Path path) {

    try (BufferedWriter writer = Files.newBufferedWriter(path)) {
      this.mapper.writerWithDefaultPrettyPrinter().writeValue(writer, object);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to save file " + path, e);
    }
  }

  /**
   * @param path the {@link Path} of the JSON file to load.
   * @return the parsed {@link JsonObject} or {@code null} if the {@link Path} is not an existing file.
   */
  public T loadJson(Path path) {
    T object = null;
    if (Files.isRegularFile(path)) {
      try (BufferedReader reader = Files.newBufferedReader(path)) {
        object = this.mapper.readValue(reader, this.type);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to load " + path, e);
      }
    }
    return object;
  }
}

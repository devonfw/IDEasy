package com.devonfw.tools.ide.json;

import java.nio.file.Path;

public abstract class StandardJsonObjectMapper<T extends JsonObject> extends JsonObjectMapper<T> {

  /**
   * The constructor.
   *
   * @param type the type of the mapped object.
   */
  protected StandardJsonObjectMapper(Class<T> type) {
    super(type);
    assert (getStandardFilename().endsWith(".json"));
  }

  /**
   * @return the standard filename used to {@link #loadJsonFromFolder(Path) load} and {@link #saveJsonToFolder(JsonObject, Path) save} the file. Should have a
   *     ".json" extension.
   */
  public abstract String getStandardFilename();

  /**
   *
   * @param folder the {@link Path} to the directory folder containing the JSON file to load using its {@link #getStandardFilename() standard filename}.
   * @return the parsed {@link JsonObject} or {@code null} if the {@link Path} is not an existing file.
   */
  public T loadJsonFromFolder(Path folder) {

    return loadJson(folder.resolve(getStandardFilename()));
  }

  /**
   * @param object the {@link JsonObject} to save as JSON.
   * @param folder the {@link Path} to the directory folder where to save the JSON file using its {@link #getStandardFilename() standard filename}.
   */
  public void saveJsonToFolder(T object, Path folder) {

    super.saveJson(object, folder.resolve(getStandardFilename()));
  }
}

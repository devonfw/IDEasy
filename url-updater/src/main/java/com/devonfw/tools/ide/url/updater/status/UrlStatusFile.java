package com.devonfw.tools.ide.url.updater.status;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.json.JsonMapping;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Wrapper for the "status.json" file of a tool version. It loads the {@link StatusJson} from disk when created
 * (or starts empty if the file is missing) and only writes it back on {@link #save()} if it was changed.
 */
public class UrlStatusFile {

  /** The filename of a status file. */
  public static final String STATUS_JSON = "status.json";

  private static final ObjectMapper MAPPER = JsonMapping.createWithReflectionSupportForUrlUpdaters();

  private final Path path;

  private StatusJson statusJson;

  private boolean modified;

  /**
   * The constructor.
   *
   * @param path the {@link #getPath() path} to the status.json file.
   */
  public UrlStatusFile(Path path) {

    super();
    this.path = path;
    if (Files.exists(path)) {
      try (BufferedReader reader = Files.newBufferedReader(path)) {
        this.statusJson = MAPPER.readValue(reader, StatusJson.class);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to load " + path, e);
      }
    } else {
      this.statusJson = new StatusJson();
    }
    this.modified = false;
  }

  /**
   * @return the {@link Path} to the status.json file.
   */
  public Path getPath() {

    return this.path;
  }

  /**
   * @return the content of the {@link StatusJson status.json} file.
   */
  public StatusJson getStatusJson() {

    return this.statusJson;
  }

  /**
   * @param statusJson new value of {@link #getStatusJson()}.
   */
  public void setStatusJson(StatusJson statusJson) {

    this.modified = true;
    this.statusJson = statusJson;
  }

  /**
   * Marks this status file as modified so that the next {@link #save()} writes it to disk.
   */
  public void markModified() {

    this.modified = true;
  }

  /**
   * Performs a cleanup and removes all unused entries.
   *
   * @see StatusJson#cleanup()
   */
  public void cleanup() {

    boolean changed = this.statusJson.cleanup();
    if (changed) {
      this.modified = true;
    }
  }

  /**
   * Saves this status file to disk if it has been modified.
   */
  public void save() {

    if (this.modified) {
      try {
        Files.createDirectories(this.path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(this.path)) {
          MAPPER.writeValue(writer, this.statusJson);
        }
      } catch (IOException e) {
        throw new IllegalStateException("Failed to save file " + this.path, e);
      }
      this.modified = false;
    }
  }

  /**
   * Deletes this status file from disk.
   */
  public void delete() {

    try {
      Files.deleteIfExists(this.path);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to delete file " + this.path, e);
    }
    this.modified = false;
  }

}

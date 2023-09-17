package com.devonfw.tools.ide.url.model.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.devonfw.tools.ide.json.mapping.JsonMapping;
import com.devonfw.tools.ide.url.model.file.json.StatusJson;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link UrlFile} for the "status.json" file.
 */
public class UrlStatusFile extends AbstractUrlFile<UrlVersion> {

  /**
   * Constant {@link UrlStatusFile#getName() filename}.
   */
  public static final String STATUS_JSON = "status.json";

  private static final ObjectMapper MAPPER = JsonMapping.create();

  private StatusJson statusJson = new StatusJson();

  /**
   * The constructor.
   *
   * @param parent the {@link #getParent() parent folder}.
   */
  public UrlStatusFile(UrlVersion parent) {

    super(parent, STATUS_JSON);
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

  @Override
  protected void doLoad() {

    Path path = getPath();
    if (Files.exists(path)) {
      try (BufferedReader reader = Files.newBufferedReader(path)) {
        this.statusJson = MAPPER.readValue(reader, StatusJson.class);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to load " + path, e);
      }
    } else {
      this.statusJson = new StatusJson();
    }
  }

  @Override
  protected void doSave() {

    try (BufferedWriter writer = Files.newBufferedWriter(getPath(), StandardOpenOption.CREATE)) {
      MAPPER.writeValue(writer, this.statusJson);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to save file " + getPath(), e);
    }
  }

}

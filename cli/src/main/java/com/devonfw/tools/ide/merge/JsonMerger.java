package com.devonfw.tools.ide.merge;

import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;

/**
 * Implementation of {@link FileMerger} for JSON.
 */
public class JsonMerger extends FileMerger {

  /**
   * The constructor.
   *
   * @param context the {@link #context}.
   */
  public JsonMerger(IdeContext context) {

    super(context);
  }

  @Override
  public void merge(Path setup, Path update, EnvironmentVariables variables, Path workspace) {

    JsonStructure json = null;
    boolean updateFileExists = Files.exists(update);
    if (Files.exists(workspace)) {
      if (!updateFileExists) {
        return; // nothing to do ...
      }
      json = load(workspace);
    } else if (Files.exists(setup)) {
      json = load(setup);
    }
    JsonStructure mergeJson = null;
    if (updateFileExists) {
      if (json == null) {
        json = load(update);
      } else {
        mergeJson = load(update);
      }
    }
    Status status = new Status();
    JsonStructure result = (JsonStructure) mergeAndResolve(json, mergeJson, variables, status, workspace.getFileName());
    if (status.updated) {
      save(result, workspace);
      this.context.debug("Saved created/updated file {}", workspace);
    } else {
      this.context.trace("No changes for file {}", workspace);
    }
  }

  private static JsonStructure load(Path file) {

    try (Reader reader = Files.newBufferedReader(file)) {
      JsonReader jsonReader = Json.createReader(reader);
      return jsonReader.read();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to read JSON from " + file, e);
    }
  }

  private static void save(JsonStructure json, Path file) {

    ensureParentDirectoryExists(file);
    try (OutputStream out = Files.newOutputStream(file)) {

      Map<String, Object> config = new HashMap<>();
      config.put(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
      // JSON-P API sucks: no way to set the indentation string
      // preferred would be two spaces, implementation has four whitespaces hardcoded
      // See org.glassfish.json.JsonPrettyGeneratorImpl
      // when will they ever learn...?
      JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(config);
      JsonWriter jsonWriter = jsonWriterFactory.createWriter(out);
      jsonWriter.write(json);
      jsonWriter.close();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to save JSON to " + file, e);
    }
  }

  @Override
  public void inverseMerge(Path workspace, EnvironmentVariables variables, boolean addNewProperties, Path updateFile) {

    if (!Files.exists(workspace) || !Files.exists(updateFile)) {
      return;
    }
    JsonStructure updateDocument = load(updateFile);
    JsonStructure workspaceDocument = load(workspace);
    Status status = new Status(addNewProperties);
    JsonStructure result = (JsonStructure) mergeAndResolve(workspaceDocument, updateDocument, variables, status,
        workspace.getFileName());
    if (status.updated) {
      save(result, updateFile);
      this.context.debug("Saved changes from {} to {}", workspace.getFileName(), updateFile);
    } else {
      this.context.trace("No changes for {}", updateFile);
    }
  }

  private JsonValue mergeAndResolve(JsonValue json, JsonValue mergeJson, EnvironmentVariables variables, Status status,
      Object src) {

    if (json == null) {
      if (mergeJson == null) {
        return null;
      } else {
        return mergeAndResolve(mergeJson, null, variables, status, src);
      }
    } else {
      if (mergeJson == null) {
        status.updated = true; // JSON to merge does not exist and needs to be created
      }
      switch (json.getValueType()) {
        case OBJECT:
          return mergeAndResolveObject((JsonObject) json, (JsonObject) mergeJson, variables, status, src);
        case ARRAY:
          return mergeAndResolveArray((JsonArray) json, (JsonArray) mergeJson, variables, status, src);
        case STRING:
          return mergeAndResolveString((JsonString) json, (JsonString) mergeJson, variables, status, src);
        case NUMBER:
        case FALSE:
        case TRUE:
        case NULL:
          return mergeAndResolveNativeType(json, mergeJson, variables, status);
        default:
          this.context.error("Undefined JSON type {}", json.getClass());
          return null;
      }
    }
  }

  private JsonObject mergeAndResolveObject(JsonObject json, JsonObject mergeJson, EnvironmentVariables variables,
      Status status, Object src) {

    // json = workspace/setup
    // mergeJson = update
    JsonObjectBuilder builder = Json.createObjectBuilder();
    Set<String> mergeKeySet = Collections.emptySet();
    if (mergeJson != null) {
      mergeKeySet = mergeJson.keySet();
      for (String key : mergeKeySet) {
        JsonValue mergeValue = mergeJson.get(key);
        JsonValue value = json.get(key);
        value = mergeAndResolve(value, mergeValue, variables, status, src);
        builder.add(key, value);
      }
    }
    if (status.addNewProperties || !status.inverse) {
      for (String key : json.keySet()) {
        if (!mergeKeySet.contains(key)) {
          JsonValue value = json.get(key);
          value = mergeAndResolve(value, null, variables, status, src);
          builder.add(key, value);
          if (status.inverse) {
            // added new property on inverse merge...
            status.updated = true;
          }
        }
      }
    }
    return builder.build();
  }

  private JsonArray mergeAndResolveArray(JsonArray json, JsonArray mergeJson, EnvironmentVariables variables,
      Status status, Object src) {

    JsonArrayBuilder builder = Json.createArrayBuilder();
    // KISS: Merging JSON arrays could be very complex. We simply let mergeJson override json...
    JsonArray source = json;
    if (mergeJson != null) {
      source = mergeJson;
    }
    for (JsonValue value : source) {
      JsonValue resolvedValue = mergeAndResolve(value, null, variables, status, src);
      builder.add(resolvedValue);
    }
    return builder.build();
  }

  private JsonString mergeAndResolveString(JsonString json, JsonString mergeJson, EnvironmentVariables variables,
      Status status, Object src) {

    JsonString jsonString = json;
    if (mergeJson != null) {
      jsonString = mergeJson;
    }
    String string = jsonString.getString();
    String resolvedString;
    if (status.inverse) {
      resolvedString = variables.inverseResolve(string, src);
    } else {
      resolvedString = variables.resolve(string, src);
    }
    if (!resolvedString.equals(string)) {
      status.updated = true;
    }
    return Json.createValue(resolvedString);
  }

  private JsonValue mergeAndResolveNativeType(JsonValue json, JsonValue mergeJson, EnvironmentVariables variables,
      Status status) {

    if (mergeJson == null) {
      return json;
    } else {
      return mergeJson;
    }
  }

  private static class Status {

    /** {@code true} for inverse merge, {@code false} otherwise (for regular forward merge). */
    private final boolean inverse;

    private final boolean addNewProperties;

    private boolean updated;

    /**
     * The constructor.
     */
    public Status() {

      this(false, false);
    }

    /**
     * The constructor.
     *
     * @param addNewProperties - {@code true} to add new properties from workspace on reverse merge, {@code false}
     *        otherwise.
     */
    public Status(boolean addNewProperties) {

      this(true, addNewProperties);
    }

    private Status(boolean inverse, boolean addNewProperties) {

      super();
      this.inverse = inverse;
      this.addNewProperties = addNewProperties;
      this.updated = false;
    }

  }

}

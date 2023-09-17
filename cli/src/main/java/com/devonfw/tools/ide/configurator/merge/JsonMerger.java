package com.devonfw.tools.ide.configurator.merge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
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

import com.devonfw.tools.ide.configurator.resolve.VariableResolver;
import com.devonfw.tools.ide.logging.Log;

/**
 * Implementation of {@link FileTypeMerger} for JSON.
 *
 * @since 3.0.0
 */
public class JsonMerger extends FileTypeMerger {

  @Override
  public void merge(File setupFile, File updateFile, VariableResolver resolver, File workspaceFile) {

    JsonStructure json = null;
    boolean updateFileExists = updateFile.exists();
    if (workspaceFile.exists()) {
      if (!updateFileExists) {
        return; // nothing to do ...
      }
      json = load(workspaceFile);
    } else if (setupFile.exists()) {
      json = load(setupFile);
    }
    JsonStructure mergeJson = null;
    if (updateFileExists) {
      if (json == null) {
        json = load(updateFile);
      } else {
        mergeJson = load(updateFile);
      }
    }
    Status status = new Status();
    JsonStructure result = (JsonStructure) mergeAndResolve(json, mergeJson, resolver, status);
    if (status.updated) {
      save(result, workspaceFile);
      Log.debug("Saving created/updated file: " + workspaceFile.getAbsolutePath());
    } else {
      Log.trace("No changes for file: " + workspaceFile.getAbsolutePath());
    }
  }

  private static JsonStructure load(File file) {

    try (FileInputStream in = new FileInputStream(file);
        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {

      JsonReader jsonReader = Json.createReader(new BufferedReader(reader));
      return jsonReader.read();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to read JSON from " + file.getPath(), e);
    }
  }

  private static void save(JsonStructure json, File file) {

    ensureParentDirecotryExists(file);
    try (FileOutputStream out = new FileOutputStream(file)) {

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
      throw new IllegalStateException("Failed to save JSON to " + file.getPath(), e);
    }
  }

  @Override
  public void inverseMerge(File workspaceFile, VariableResolver resolver, boolean addNewProperties, File updateFile) {

    if (!workspaceFile.exists() || !updateFile.exists()) {
      return;
    }
    JsonStructure updateDocument = load(updateFile);
    JsonStructure workspaceDocument = load(workspaceFile);
    Status status = new Status(addNewProperties);
    JsonStructure result = (JsonStructure) mergeAndResolve(workspaceDocument, updateDocument, resolver, status);
    if (status.updated) {
      save(result, updateFile);
      Log.debug("Saved changes from " + workspaceFile.getName() + " to " + updateFile.getAbsolutePath());
    } else {
      Log.trace("No changes for " + updateFile.getAbsolutePath());
    }
  }

  private JsonValue mergeAndResolve(JsonValue json, JsonValue mergeJson, VariableResolver resolver, Status status) {

    if (json == null) {
      if (mergeJson == null) {
        return null;
      } else {
        return mergeAndResolve(mergeJson, null, resolver, status);
      }
    } else {
      if (mergeJson == null) {
        status.updated = true; // JSON to merge does not exist and needs to be created
      }
      switch (json.getValueType()) {
        case OBJECT:
          return mergeAndResolveObject((JsonObject) json, (JsonObject) mergeJson, resolver, status);
        case ARRAY:
          return mergeAndResolveArray((JsonArray) json, (JsonArray) mergeJson, resolver, status);
        case STRING:
          return mergeAndResolveString((JsonString) json, (JsonString) mergeJson, resolver, status);
        case NUMBER:
        case FALSE:
        case TRUE:
        case NULL:
          return mergeAndResolveNativeType(json, mergeJson, resolver, status);
        default:
          Log.err("Undefined JSON type: " + json.getClass());
          return null;
      }
    }
  }

  private JsonObject mergeAndResolveObject(JsonObject json, JsonObject mergeJson, VariableResolver resolver,
      Status status) {

    // json = workspace/setup
    // mergeJson = update
    JsonObjectBuilder builder = Json.createObjectBuilder();
    Set<String> mergeKeySet = Collections.emptySet();
    if (mergeJson != null) {
      mergeKeySet = mergeJson.keySet();
      for (String key : mergeKeySet) {
        JsonValue mergeValue = mergeJson.get(key);
        JsonValue value = json.get(key);
        value = mergeAndResolve(value, mergeValue, resolver, status);
        builder.add(key, value);
      }
    }
    if (status.addNewProperties || !status.inverse) {
      for (String key : json.keySet()) {
        if (!mergeKeySet.contains(key)) {
          JsonValue value = json.get(key);
          value = mergeAndResolve(value, null, resolver, status);
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

  private JsonArray mergeAndResolveArray(JsonArray json, JsonArray mergeJson, VariableResolver resolver,
      Status status) {

    JsonArrayBuilder builder = Json.createArrayBuilder();
    // KISS: Merging JSON arrays could be very complex. We simply let mergeJson override json...
    JsonArray source = json;
    if (mergeJson != null) {
      source = mergeJson;
    }
    for (JsonValue value : source) {
      JsonValue resolvedValue = mergeAndResolve(value, null, resolver, status);
      builder.add(resolvedValue);
    }
    return builder.build();
  }

  private JsonString mergeAndResolveString(JsonString json, JsonString mergeJson, VariableResolver resolver,
      Status status) {

    JsonString jsonString = json;
    if (mergeJson != null) {
      jsonString = mergeJson;
    }
    String string = jsonString.getString();
    String resolvedString;
    if (status.inverse) {
      resolvedString = resolver.inverseResolve(string);
    } else {
      resolvedString = resolver.resolve(string);
    }
    if (!resolvedString.equals(string)) {
      status.updated = true;
    }
    return Json.createValue(resolvedString);
  }

  private JsonValue mergeAndResolveNativeType(JsonValue json, JsonValue mergeJson, VariableResolver resolver,
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

package com.devonfw.tools.ide.merge;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

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
  protected void doMerge(Path setup, Path update, EnvironmentVariables variables, Path workspace) {

    JsonStructure json = null;
    Path template = setup;
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
      template = update;
    }
    Status status = new Status();
    JsonStructure result = (JsonStructure) mergeAndResolve(json, mergeJson, variables, status, template.toString());
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
      return switch (json.getValueType()) {
        case OBJECT -> mergeAndResolveObject((JsonObject) json, (JsonObject) mergeJson, variables, status, src);
        case ARRAY -> mergeAndResolveArray((JsonArray) json, (JsonArray) mergeJson, variables, status, src);
        case STRING -> mergeAndResolveString((JsonString) json, (JsonString) mergeJson, variables, status, src);
        case NUMBER, FALSE, TRUE, NULL -> mergeAndResolveNativeType(json, mergeJson, variables, status);
        default -> {
          this.context.error("Undefined JSON type {}", json.getClass());
          yield null;
        }
      };
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
      resolvedString = variables.resolve(string, src, this.legacySupport);
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

  @Override
  protected boolean doUpgrade(Path workspaceFile) throws Exception {

    JsonNode jsonNode;
    ObjectMapper mapper = new ObjectMapper();
    try (Reader reader = Files.newBufferedReader(workspaceFile)) {
      jsonNode = mapper.reader().readTree(reader);
    }
    JsonNode migratedNode = upgradeJsonNode(jsonNode);
    boolean modified = (migratedNode != jsonNode);
    if (migratedNode == null) {
      migratedNode = jsonNode;
    }
    if (modified) {
      try (Writer writer = Files.newBufferedWriter(workspaceFile)) {
        mapper.writer(new JsonPrettyPrinter()).writeValue(writer, migratedNode);
      }
    }
    return modified;
  }

  /**
   * @param jsonNode the {@link JsonNode} to upgrade.
   * @return the given {@link JsonNode} if unmodified after upgrade. Otherwise, a new migrated {@link JsonNode} or {@code null} if the given {@link JsonNode}
   *     was mutable and the migration could be applied directly.
   */
  private JsonNode upgradeJsonNode(JsonNode jsonNode) {

    if (jsonNode instanceof ArrayNode jsonArray) {
      return upgradeJsonArray(jsonArray);
    } else if (jsonNode instanceof ObjectNode jsonObject) {
      return upgradeJsonObject(jsonObject);
    } else if (jsonNode instanceof TextNode jsonString) {
      return upgradeJsonString(jsonString);
    } else {
      assert jsonNode.isValueNode();
      return jsonNode;
    }
  }

  private ObjectNode upgradeJsonObject(ObjectNode jsonObject) {

    ObjectNode result = jsonObject;
    Iterator<String> fieldNames = jsonObject.fieldNames();
    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();
      JsonNode child = jsonObject.get(fieldName);
      JsonNode migratedChild = upgradeJsonNode(child);
      if (migratedChild != child) {
        result = null;
        if (migratedChild != null) {
          jsonObject.put(fieldName, migratedChild);
        }
      }
    }
    return result;
  }

  private ArrayNode upgradeJsonArray(ArrayNode jsonArray) {

    ArrayNode result = jsonArray;
    int size = jsonArray.size();
    for (int i = 0; i < size; i++) {
      JsonNode child = jsonArray.get(i);
      JsonNode migratedChild = upgradeJsonNode(child);
      if (migratedChild != child) {
        result = null;
        if (migratedChild != null) {
          jsonArray.set(i, migratedChild);
        }
      }
    }
    return result;
  }

  private JsonNode upgradeJsonString(TextNode jsonString) {

    String text = jsonString.textValue();
    String migratedText = upgradeWorkspaceContent(text);
    if (migratedText.equals(text)) {
      return jsonString;
    } else {
      return new TextNode(migratedText);
    }
  }

  private static class Status {

    /**
     * {@code true} for inverse merge, {@code false} otherwise (for regular forward merge).
     */
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
     * @param addNewProperties - {@code true} to add new properties from workspace on reverse merge, {@code false} otherwise.
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

  /**
   * Extends {@link DefaultPrettyPrinter} to get nicely formatted JSON output.
   */
  private static class JsonPrettyPrinter extends DefaultPrettyPrinter {

    public JsonPrettyPrinter() {
      DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("  ", "\n");
      indentObjectsWith(indenter);
      indentArraysWith(indenter);
      _objectFieldValueSeparatorWithSpaces = ": ";
    }

    private JsonPrettyPrinter(JsonPrettyPrinter pp) {
      super(pp);
    }

    @Override
    public void writeEndArray(com.fasterxml.jackson.core.JsonGenerator g, int nrOfValues) throws IOException {

      if (!_arrayIndenter.isInline()) {
        _nesting--;
      }
      if (nrOfValues > 0) {
        _arrayIndenter.writeIndentation(g, _nesting);
      }
      g.writeRaw(']');
    }

    @Override
    public DefaultPrettyPrinter createInstance() {
      return new JsonPrettyPrinter(this);
    }
  }
}

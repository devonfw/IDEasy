package com.devonfw.tools.ide.tool.openrewrite;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.json.JsonBuilder;
import com.devonfw.tools.ide.json.JsonObjectDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;

/**
 * {@link JsonObjectDeserializer} for {@link RecipeWrapper}.
 */
public class RecipeWrapperJsonDeserializer extends JsonObjectDeserializer<RecipeWrapper> {

  private static final Logger LOG = LoggerFactory.getLogger(RecipeWrapperJsonDeserializer.class);

  @Override
  protected JsonBuilder<RecipeWrapper> createBuilder() {

    return new RecipeWrapperBuilder();
  }

  private class RecipeWrapperBuilder extends JsonBuilder<RecipeWrapper> {

    private String id;
    private String description;
    private String url;
    private Map<String, String> properties;

    @Override
    public void setProperty(String property, JsonParser p, DeserializationContext ctxt) throws IOException {

      switch (property) {
        case "id" -> {
          this.id = readValueAsString(p, property, this.id);
        }
        case "description" -> {
          this.description = readValueAsString(p, property, this.description);
        }
        case "url" -> {
          this.url = readValueAsString(p, property, this.url);
        }
        case "properties" -> {
          this.properties = readProperties(p, property);
        }
        default -> {
          LOG.warn("Unknown recipe property: {}", property);
          super.setProperty(property, p, ctxt);
        }
      }
    }

    /**
     * Reads the {@code properties} object (a map of {@code key -> value} string pairs). A missing
     * {@code properties} object yields an empty map, since a recipe without properties is valid.
     *
     * @param p the {@link JsonParser} positioned at the start of the {@code properties} value.
     * @param property the property name (for duplicate detection).
     * @return the read properties, never {@code null}.
     * @throws IOException on I/O errors.
     */
    private Map<String, String> readProperties(JsonParser p, String property) throws IOException {

      if (p.getCurrentToken() != JsonToken.START_OBJECT) {
        return new LinkedHashMap<>();
      }
      Map<String, String> result = new LinkedHashMap<>();
      JsonToken token = p.nextToken();
      while (token == JsonToken.FIELD_NAME) {
        String key = p.currentName();
        p.nextToken();
        result.put(key, readValueAsString(p, key, null));
        token = p.nextToken();
      }
      return result;
    }

    @Override
    public RecipeWrapper build() {

      Map<String, String> props = this.properties == null ? new LinkedHashMap<>() : this.properties;
      return new RecipeWrapper(this.id, this.description, this.url, props);
    }
  }
}

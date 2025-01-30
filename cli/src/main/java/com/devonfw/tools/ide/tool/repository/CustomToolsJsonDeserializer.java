package com.devonfw.tools.ide.tool.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * {@link JsonDeserializer} for {@link CustomToolsJson}.
 */
public class CustomToolsJsonDeserializer extends JsonDeserializer<CustomToolsJson> {

  private static final String INVALID_CUSTOM_TOOLS = "Invalid JSON for custom tools!";

  @Override
  public CustomToolsJson deserialize(JsonParser p, DeserializationContext context) throws IOException {

    JsonToken token = p.getCurrentToken();
    if (token == JsonToken.START_OBJECT) {
      token = p.nextToken();
      String url = null;
      List<CustomToolJson> tools = new ArrayList<>();
      while (token == JsonToken.FIELD_NAME) {
        String property = p.currentName();
        if (property.equals(CustomToolsJson.PROPERTY_URL)) {
          token = p.nextToken();
          assert token == JsonToken.VALUE_STRING;
          url = p.getValueAsString();
        } else if (property.equals(CustomToolsJson.PROPERTY_TOOLS)) {
          token = p.nextToken();
          if (token == JsonToken.START_ARRAY) {
            token = p.nextToken();
            while (token != JsonToken.END_ARRAY) {
              CustomToolJson customToolJson = p.readValueAs(CustomToolJson.class);
              tools.add(customToolJson);
              token = p.nextToken();
            }
          }
        } else {
          // ignore unknown property
          // currently cannot log here due to https://github.com/devonfw/IDEasy/issues/404
        }
        token = p.nextToken();
      }
      if ((url != null) && !tools.isEmpty()) {
        return new CustomToolsJson(url, tools);
      }
    }
    throw new IllegalStateException(INVALID_CUSTOM_TOOLS);
  }

}

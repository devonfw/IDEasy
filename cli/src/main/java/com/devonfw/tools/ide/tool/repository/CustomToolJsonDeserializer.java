package com.devonfw.tools.ide.tool.repository;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * {@link JsonDeserializer} for {@link CustomToolJson}.
 */
public class CustomToolJsonDeserializer extends JsonDeserializer<CustomToolJson> {

  private static final String INVALID_CUSTOM_TOOL = "Invalid JSON for custom tool!";

  @Override
  public CustomToolJson deserialize(JsonParser p, DeserializationContext context) throws IOException {

    JsonToken token = p.getCurrentToken();
    if (token == JsonToken.START_OBJECT) {
      token = p.nextToken();
      String name = null;
      String version = null;
      boolean osAgnostic = true;
      boolean archAgnostic = true;
      String url = null;
      while (token == JsonToken.FIELD_NAME) {
        String property = p.currentName();
        if (property.equals(CustomToolJson.PROPERTY_NAME)) {
          token = p.nextToken();
          assert token == JsonToken.VALUE_STRING;
          name = p.getValueAsString();
        } else if (property.equals(CustomToolJson.PROPERTY_VERSION)) {
          token = p.nextToken();
          assert token == JsonToken.VALUE_STRING;
          version = p.getValueAsString();
        } else if (property.equals(CustomToolJson.PROPERTY_OS_AGNOSTIC)) {
          token = p.nextToken();
          osAgnostic = parseBoolean(token, CustomToolJson.PROPERTY_OS_AGNOSTIC);
        } else if (property.equals(CustomToolJson.PROPERTY_ARCH_AGNOSTIC)) {
          token = p.nextToken();
          archAgnostic = parseBoolean(token, CustomToolJson.PROPERTY_ARCH_AGNOSTIC);
        } else if (property.equals(CustomToolJson.PROPERTY_URL)) {
          token = p.nextToken();
          assert token == JsonToken.VALUE_STRING;
          url = p.getValueAsString();
        } else {
          // ignore unknown property
          // currently cannot log here due to https://github.com/devonfw/IDEasy/issues/404
        }
        token = p.nextToken();
      }
      if ((name != null) && (version != null)) {
        return new CustomToolJson(name, version, osAgnostic, archAgnostic, url);
      }
    }
    throw new IllegalStateException(INVALID_CUSTOM_TOOL);
  }

  private boolean parseBoolean(JsonToken token, String name) {
    if (token == JsonToken.VALUE_TRUE) {
      return true;
    } else if (token == JsonToken.VALUE_FALSE) {
      return false;
    } else {
      throw new IllegalStateException(INVALID_CUSTOM_TOOL + " Property " + name + " must have boolean value (true or false).");
    }
  }

}

package com.devonfw.tools.ide.json;

import java.io.IOException;

import com.devonfw.tools.ide.version.BoundaryType;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * {@link JsonDeserializer} for {@link VersionRange}.
 */
public class VersionRangeDeserializer extends JsonDeserializer<VersionRange> {

  @Override
  public VersionRange deserialize(JsonParser p, DeserializationContext context) throws IOException, JacksonException {

    JsonToken token = p.getCurrentToken();
    if (token == JsonToken.VALUE_STRING) {
      return VersionRange.of(p.getValueAsString());
    } else if (token == JsonToken.START_OBJECT) {
      token = p.nextToken();
      VersionIdentifier min = null;
      VersionIdentifier max = null;
      BoundaryType type = null;
      while (token == JsonToken.FIELD_NAME) {
        String name = p.currentName();
        token = p.nextToken();
        String value = p.getValueAsString();
        if (VersionRange.PROPERTY_MIN.equals(name)) {
          min = VersionIdentifier.of(value);
        } else if (VersionRange.PROPERTY_MAX.equals(name)) {
          max = VersionIdentifier.of(value);
        } else if (VersionRange.PROPERTY_TYPE.equals(name)) {
          type = BoundaryType.valueOf(value);
        }
        p.nextToken();
      }
      if (token == JsonToken.END_OBJECT) {
        p.nextToken();
      } else {
        throw new IllegalStateException(token.toString());
      }
      return VersionRange.of(min, max, type);
    } else {
      throw new IllegalArgumentException("Invalid JSON for VersionRange!");
    }
  }

}

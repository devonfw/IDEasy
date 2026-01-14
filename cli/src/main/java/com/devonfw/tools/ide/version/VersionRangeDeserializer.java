package com.devonfw.tools.ide.version;

import java.io.IOException;

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
      return VersionRange.of(p.getValueAsString(), true);
    } else {
      throw new IllegalArgumentException("Invalid JSON for VersionRange!");
    }
  }

}

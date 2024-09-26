package com.devonfw.tools.ide.json;

import java.io.IOException;

import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * {@link JsonDeserializer} for {@link ToolDependency}.
 */
public class ToolDependencyDeserializer extends JsonDeserializer<ToolDependency> {

  @Override
  public ToolDependency deserialize(JsonParser p, DeserializationContext context) throws IOException {

    JsonToken token = p.getCurrentToken();
    if (token == JsonToken.START_OBJECT) {
      token = p.nextToken();
      String tool = null;
      VersionRange versionRange = null;
      while (token == JsonToken.FIELD_NAME) {
        String property = p.currentName();
        if (property.equals("tool")) {
          token = p.nextToken();
          assert token == JsonToken.VALUE_STRING;
          tool = p.getValueAsString();
        } else if (property.equals("versionRange")) {
          token = p.nextToken();
          assert (token == JsonToken.START_OBJECT) || (token == JsonToken.VALUE_STRING);
          versionRange = p.readValueAs(VersionRange.class);
        } else {
          // ignore unknown property
          // currently cannot log here due to https://github.com/devonfw/IDEasy/issues/404
        }
        token = p.nextToken();
      }
      if ((tool != null) && (versionRange != null)) {
        return new ToolDependency(tool, versionRange);
      }
    }
    throw new IllegalArgumentException("Invalid JSON for DependencyInfo!");
  }

}

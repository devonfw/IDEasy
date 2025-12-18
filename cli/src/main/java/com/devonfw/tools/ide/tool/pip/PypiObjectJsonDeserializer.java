package com.devonfw.tools.ide.tool.pip;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * {@link JsonDeserializer} for {@link PypiObject}.
 */
public class PypiObjectJsonDeserializer extends JsonDeserializer<PypiObject> {

  @Override
  public PypiObject deserialize(JsonParser p, DeserializationContext deserializationContext) throws IOException {

    List<VersionIdentifier> releases = null;
    JsonToken token = p.getCurrentToken();
    if (token == JsonToken.START_OBJECT) {
      token = p.nextToken();
      while (token == JsonToken.FIELD_NAME) {
        String property = p.currentName();
        if (property.equals(PypiObject.PROPERTY_RELEASES)) {
          releases = parseReleases(p);
        } else {// currently cannot log here due to https://github.com/devonfw/IDEasy/issues/404
          //LOG.debug("Ignoring unexpected property {}", property);
          JsonMapping.skipCurrentField(p);
        }
        token = p.nextToken();
      }
      if (releases == null) {
        releases = List.of();
      }
      return new PypiObject(releases);
    } else if (token == JsonToken.VALUE_NULL) {
      return null;
    } else {
      throw new IllegalStateException("Unexpected token " + token);
    }
  }

  private List<VersionIdentifier> parseReleases(JsonParser p) throws IOException {
    List<VersionIdentifier> releases = new ArrayList<>();
    JsonToken token = p.nextToken();
    if (token == JsonToken.START_OBJECT) {
      token = p.nextToken();
      while (token == JsonToken.FIELD_NAME) {
        String property = p.currentName();
        VersionIdentifier version = VersionIdentifier.of(property);
        releases.add(version);
        JsonMapping.skipCurrentField(p);
        token = p.nextToken();
      }
    } else if (token != JsonToken.VALUE_NULL) {
      throw new IllegalStateException("Unexpected token " + token);
    }
    return releases;
  }
}

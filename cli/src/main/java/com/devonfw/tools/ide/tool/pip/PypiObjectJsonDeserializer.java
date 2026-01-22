package com.devonfw.tools.ide.tool.pip;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.devonfw.tools.ide.json.JsonBuilder;
import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.json.JsonObjectDeserializer;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * {@link JsonDeserializer} for {@link PypiObject}.
 */
public class PypiObjectJsonDeserializer extends JsonObjectDeserializer<PypiObject> {

  @Override
  protected JsonBuilder<PypiObject> createBuilder() {

    return new PypiObjectBuilder();
  }

  private List<VersionIdentifier> parseReleases(JsonParser p) throws IOException {
    List<VersionIdentifier> releases = new ArrayList<>();
    JsonToken token = p.getCurrentToken();
    if (token == JsonToken.START_OBJECT) {
      token = p.nextToken();
      while (token == JsonToken.FIELD_NAME) {
        String property = p.currentName();
        token = p.nextToken();
        VersionIdentifier version = VersionIdentifier.of(property);
        releases.add(version);
        JsonMapping.skipCurrentField(p, null);
        token = p.nextToken();
      }
    } else if (token != JsonToken.VALUE_NULL) {
      throw new IllegalStateException("Unexpected token " + token);
    }
    return releases;
  }

  private class PypiObjectBuilder extends JsonBuilder<PypiObject> {

    private List<VersionIdentifier> releases;

    @Override
    public void setProperty(String property, JsonParser p, DeserializationContext ctxt) throws IOException {

      if (property.equals(PypiObject.PROPERTY_RELEASES)) {
        assert (this.releases == null);
        this.releases = parseReleases(p);
      } else {
        super.setProperty(property, p, ctxt);
      }
    }

    @Override
    public PypiObject build() {

      if (this.releases == null) {
        this.releases = List.of();
      }
      return new PypiObject(this.releases);
    }
  }
}

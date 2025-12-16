package com.devonfw.tools.ide.url.model.file.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * {@link JsonDeserializer} for {@link Cve}.
 */
public class CveJsonDeserializer extends JsonDeserializer<Cve> {

  // private static final Logger LOG = LoggerFactory.getLogger(ToolSecurityJsonDeserializer.class);

  @Override
  public Cve deserialize(JsonParser p, DeserializationContext deserializationContext) throws IOException {

    JsonToken token = p.getCurrentToken();
    if (token == JsonToken.START_OBJECT) {
      String id = null;
      double severity = 0;
      List<VersionRange> versions = null;
      token = p.nextToken();
      while (token == JsonToken.FIELD_NAME) {
        String property = p.currentName();
        switch (property) {
          case Cve.PROPERTY_ID -> {
            token = p.nextToken();
            assert token == JsonToken.VALUE_STRING;
            assert id == null;
            id = p.getValueAsString();
          }
          case Cve.PROPERTY_SEVERITY -> {
            token = p.nextToken();
            assert token.isNumeric();
            assert severity == 0;
            severity = p.getValueAsDouble();
          }
          case Cve.PROPERTY_VERSIONS -> {
            assert versions == null;
            versions = parseVersions(p);
          }
          default -> {
            // currently cannot log here due to https://github.com/devonfw/IDEasy/issues/404
            //LOG.debug("Ignoring unexpected property {}", property);
            JsonMapping.skipCurrentField(p);
          }
        }
        token = p.nextToken();
      }
      assert id != null;
      assert severity != 0;
      assert versions != null;
      return new Cve(id, severity, versions);
    } else if (token == JsonToken.VALUE_NULL) {
      return null;
    } else {
      throw new IllegalStateException("Unexpected token " + token);
    }
  }

  private static List<VersionRange> parseVersions(JsonParser p) throws IOException {
    JsonToken token = p.nextToken();
    if (token == JsonToken.START_ARRAY) {
      List<VersionRange> versions = new ArrayList<>();
      token = p.nextToken();
      while (token != JsonToken.END_ARRAY) {
        VersionRange versionRange = p.readValueAs(VersionRange.class);
        versions.add(versionRange);
        token = p.nextToken();
      }
      return versions;
    } else if (token == JsonToken.VALUE_NULL) {
      return null;
    } else {
      throw new IllegalStateException("Unexpected token " + token);
    }
  }
}

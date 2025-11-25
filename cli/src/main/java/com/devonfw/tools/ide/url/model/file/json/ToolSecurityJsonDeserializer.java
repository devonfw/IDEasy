package com.devonfw.tools.ide.url.model.file.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * {@link JsonDeserializer} for {@link ToolSecurity}.
 */
public class ToolSecurityJsonDeserializer extends JsonDeserializer<ToolSecurity> {

  // private static final Logger LOG = LoggerFactory.getLogger(ToolSecurityJsonDeserializer.class);

  @Override
  public ToolSecurity deserialize(JsonParser p, DeserializationContext deserializationContext) throws IOException {

    JsonToken token = p.getCurrentToken();
    if (token == JsonToken.START_OBJECT) {
      List<Cve> issues = null;
      token = p.nextToken();
      while (token == JsonToken.FIELD_NAME) {
        String property = p.currentName();
        if (property.equals(ToolSecurity.PROPERTY_ISSUES)) {
          assert (issues == null);
          issues = parseIssues(p);
          token = p.nextToken();
        } else {
          // currently cannot log here due to https://github.com/devonfw/IDEasy/issues/404
          //LOG.debug("Ignoring unexpected property {}", property);
          p.skipChildren();
          token = p.nextToken();
        }
      }
      assert (issues != null);
      return new ToolSecurity(Collections.unmodifiableList(issues));
    } else if (token == JsonToken.VALUE_NULL) {
      return null;
    } else {
      throw new IllegalStateException("Unexpected token " + token);
    }
  }

  private static List<Cve> parseIssues(JsonParser p) throws IOException {
    JsonToken token = p.nextToken();
    if (token == JsonToken.START_ARRAY) {
      List<Cve> issues = new ArrayList<>();
      token = p.nextToken();
      while (token != JsonToken.END_ARRAY) {
        Cve cve = p.readValueAs(Cve.class);
        issues.add(cve);
        token = p.nextToken();
      }
      return issues;
    } else if (token == JsonToken.VALUE_NULL) {
      return null;
    } else {
      throw new IllegalStateException("Unexpected token " + token);
    }
  }
}

package com.devonfw.tools.ide.url.model.file.json;

import java.io.IOException;
import java.util.List;

import com.devonfw.tools.ide.json.JsonBuilder;
import com.devonfw.tools.ide.json.JsonObjectDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

/**
 * {@link JsonObjectDeserializer} for {@link ToolSecurity}.
 */
public class ToolSecurityJsonDeserializer extends JsonObjectDeserializer<ToolSecurity> {

  @Override
  protected JsonBuilder<ToolSecurity> createBuilder() {

    return new ToolSecurityBuilder();
  }

  private class ToolSecurityBuilder extends JsonBuilder<ToolSecurity> {

    private List<Cve> issues;

    @Override
    public void setProperty(String property, JsonParser p, DeserializationContext ctxt) throws IOException {

      if (property.equals(ToolSecurity.PROPERTY_ISSUES)) {
        this.issues = readArray(p, Cve.class, property, this.issues);
      } else {
        super.setProperty(property, p, ctxt);
      }
    }

    @Override
    public ToolSecurity build() {

      return new ToolSecurity(this.issues);
    }
  }
}

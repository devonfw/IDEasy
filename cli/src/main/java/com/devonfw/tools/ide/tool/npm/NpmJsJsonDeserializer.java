package com.devonfw.tools.ide.tool.npm;

import java.io.IOException;
import java.util.Objects;

import com.devonfw.tools.ide.json.JsonBuilder;
import com.devonfw.tools.ide.json.JsonObjectDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

/**
 * {@link JsonObjectDeserializer} for {@link NpmJs}.
 */
public class NpmJsJsonDeserializer extends JsonObjectDeserializer<NpmJs> {

  @Override
  protected JsonBuilder<NpmJs> createBuilder() {

    return new NpmJsBuilder();
  }

  private class NpmJsBuilder extends JsonBuilder<NpmJs> {

    private NpmJsVersions versions;

    @Override
    public void setProperty(String property, JsonParser p, DeserializationContext ctxt) throws IOException {

      if (property.equals(NpmJs.PROPERTY_VERSIONS)) {
        this.versions = readValue(p, NpmJsVersions.class, property, this.versions);
      } else {
        super.setProperty(property, p, ctxt);
      }
    }

    @Override
    public NpmJs build() {

      Objects.requireNonNull(this.versions);
      return new NpmJs(this.versions);
    }
  }
}

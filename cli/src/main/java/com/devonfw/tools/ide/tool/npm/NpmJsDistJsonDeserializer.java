package com.devonfw.tools.ide.tool.npm;

import java.io.IOException;
import java.util.Objects;

import com.devonfw.tools.ide.json.JsonBuilder;
import com.devonfw.tools.ide.json.JsonObjectDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

/**
 * {@link JsonObjectDeserializer} for {@link NpmJsDist}.
 */
public class NpmJsDistJsonDeserializer extends JsonObjectDeserializer<NpmJsDist> {

  @Override
  protected JsonBuilder<NpmJsDist> createBuilder() {

    return new NpmJsDistBuilder();
  }

  private class NpmJsDistBuilder extends JsonBuilder<NpmJsDist> {

    private String tarball;

    @Override
    public void setProperty(String property, JsonParser p, DeserializationContext ctxt) throws IOException {

      if (property.equals(NpmJsDist.PROPERTY_TARBALL)) {
        this.tarball = readValueAsString(p, property, this.tarball);
      } else {
        super.setProperty(property, p, ctxt);
      }
    }

    @Override
    public NpmJsDist build() {

      Objects.requireNonNull(this.tarball);
      return new NpmJsDist(this.tarball);
    }
  }
}

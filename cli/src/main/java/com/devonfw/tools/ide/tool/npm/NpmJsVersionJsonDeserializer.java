package com.devonfw.tools.ide.tool.npm;

import java.io.IOException;
import java.util.Objects;

import com.devonfw.tools.ide.json.JsonBuilder;
import com.devonfw.tools.ide.json.JsonObjectDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

/**
 * {@link JsonObjectDeserializer} for {@link NpmJsVersion}.
 */
public class NpmJsVersionJsonDeserializer extends JsonObjectDeserializer<NpmJsVersion> {

  @Override
  protected JsonBuilder<NpmJsVersion> createBuilder() {

    return new NpmJsVersionBuilder();
  }

  private class NpmJsVersionBuilder extends JsonBuilder<NpmJsVersion> {

    private String version;
    private NpmJsDist dist;

    @Override
    public void setProperty(String property, JsonParser p, DeserializationContext ctxt) throws IOException {

      if (property.equals(NpmJsVersion.PROPERTY_VERSION)) {
        assert (version == null);
        this.version = readValueAsString(p, property, this.version);
      } else if (property.equals(NpmJsVersion.PROPERTY_DIST)) {
        this.dist = readValue(p, NpmJsDist.class, property, this.dist);
      } else {
        super.setProperty(property, p, ctxt);
      }
    }

    @Override
    public NpmJsVersion build() {

      Objects.requireNonNull(this.version);
      Objects.requireNonNull(this.dist);
      return new NpmJsVersion(this.version, this.dist);
    }
  }
}

package com.devonfw.tools.ide.url.model.file.json;

import java.io.IOException;
import java.util.List;

import com.devonfw.tools.ide.json.JsonBuilder;
import com.devonfw.tools.ide.json.JsonObjectDeserializer;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

/**
 * {@link JsonObjectDeserializer} for {@link Cve}.
 */
public class CveJsonDeserializer extends JsonObjectDeserializer<Cve> {

  @Override
  protected JsonBuilder<Cve> createBuilder() {

    return new CveBuilder();
  }

  private class CveBuilder extends JsonBuilder<Cve> {

    private String id;
    private Double severity;
    private List<VersionRange> versions;

    @Override
    public void setProperty(String property, JsonParser p, DeserializationContext ctxt) throws IOException {

      switch (property) {
        case Cve.PROPERTY_ID -> {
          this.id = readValueAsString(p, property, this.id);
        }
        case Cve.PROPERTY_SEVERITY -> {
          this.severity = readValueAsDouble(p, property, this.severity);
        }
        case Cve.PROPERTY_VERSIONS -> {
          this.versions = readArray(p, VersionRange.class, property, this.versions);
        }
        default -> {
          super.setProperty(property, p, ctxt);
        }
      }
    }

    @Override
    public Cve build() {

      return new Cve(this.id, this.severity, this.versions);
    }
  }
}

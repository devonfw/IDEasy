package com.devonfw.tools.ide.tool.extra;

import java.io.IOException;
import java.util.Objects;

import com.devonfw.tools.ide.json.JsonBuilder;
import com.devonfw.tools.ide.json.JsonObjectDeserializer;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

/**
 * {@link JsonObjectDeserializer} for {@link ExtraTools}.
 */
public class ExtraToolInstallationJsonDeserializer extends JsonObjectDeserializer<ExtraToolInstallation> {

  private String name;

  @Override
  public ExtraToolInstallation deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

    this.name = p.currentName();
    p.nextToken();
    return super.deserialize(p, ctxt);
  }

  @Override
  protected JsonBuilder<ExtraToolInstallation> createBuilder() {

    return new ExtraToolInstallationBuilder();
  }

  private class ExtraToolInstallationBuilder extends JsonBuilder<ExtraToolInstallation> {

    private String version;
    private String edition;

    @Override
    public void setProperty(String property, JsonParser p, DeserializationContext ctxt) throws IOException {

      if (property.equals(ExtraToolInstallation.PROPERTY_VERSION)) {
        this.version = readValueAsString(p, property, this.version);
      } else if (property.equals(ExtraToolInstallation.PROPERTY_EDITION)) {
        this.edition = readValueAsString(p, property, this.edition);
      } else {
        super.setProperty(property, p, ctxt);
      }
    }

    @Override
    public ExtraToolInstallation build() {

      Objects.requireNonNull(name);
      Objects.requireNonNull(this.version);
      return new ExtraToolInstallation(name, VersionIdentifier.of(this.version), this.edition);
    }
  }
}

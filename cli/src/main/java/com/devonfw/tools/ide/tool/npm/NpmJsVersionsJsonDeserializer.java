package com.devonfw.tools.ide.tool.npm;

import java.io.IOException;

import com.devonfw.tools.ide.json.JsonBuilder;
import com.devonfw.tools.ide.json.JsonObjectDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

/**
 * {@link JsonObjectDeserializer} for {@link NpmJsVersions}.
 */
public class NpmJsVersionsJsonDeserializer extends JsonObjectDeserializer<NpmJsVersions> {

  @Override
  protected JsonBuilder<NpmJsVersions> createBuilder() {

    return new NpmJsVersionsBuidler();
  }

  private class NpmJsVersionsBuidler extends JsonBuilder<NpmJsVersions> {

    private final NpmJsVersions versions = new NpmJsVersions();

    @Override
    public void setProperty(String property, JsonParser p, DeserializationContext ctxt) throws IOException {

      String version = property; // done to make this explicit, ignore IDE warnings
      NpmJsVersion npmJsVersion = p.readValueAs(NpmJsVersion.class);
      versions.getVersionMap().put(version, npmJsVersion);
      super.setProperty(property, p, ctxt);
    }

    @Override
    public NpmJsVersions build() {

      return this.versions;
    }
  }
}

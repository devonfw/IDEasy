package com.devonfw.tools.ide.tool.extra;

import java.io.IOException;

import com.devonfw.tools.ide.json.JsonBuilder;
import com.devonfw.tools.ide.json.JsonObjectDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;

/**
 * {@link JsonObjectDeserializer} for {@link ExtraTools}.
 */
public class ExtraToolsJsonDeserializer extends JsonObjectDeserializer<ExtraTools> {

  @Override
  protected JsonBuilder<ExtraTools> createBuilder() {

    return new ExtraToolsBuilder();
  }

  private class ExtraToolsBuilder extends JsonBuilder<ExtraTools> {

    private final ExtraTools result = new ExtraTools();

    @Override
    public void setProperty(String property, JsonParser p, DeserializationContext ctxt) throws IOException {

      String tool = property; // done to make this explicit, ignore IDE warnings
      JsonToken token = p.getCurrentToken();
      if (token == JsonToken.START_OBJECT) {
        token = p.nextToken();
        while (token == JsonToken.FIELD_NAME) {
          ExtraToolInstallation installation = p.readValueAs(ExtraToolInstallation.class);
          this.result.addExtraInstallations(tool, installation);
          token = p.nextToken();
        }
        assert (token == JsonToken.END_OBJECT);
      }
    }

    @Override
    public ExtraTools build() {

      return this.result.asImmutable();
    }
  }
}

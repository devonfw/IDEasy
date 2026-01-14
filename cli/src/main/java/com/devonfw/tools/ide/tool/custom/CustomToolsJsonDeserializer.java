package com.devonfw.tools.ide.tool.custom;

import java.io.IOException;
import java.util.List;

import com.devonfw.tools.ide.json.JsonBuilder;
import com.devonfw.tools.ide.json.JsonObjectDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

/**
 * {@link JsonObjectDeserializer} for {@link CustomTools}.
 */
public class CustomToolsJsonDeserializer extends JsonObjectDeserializer<CustomTools> {

  @Override
  protected JsonBuilder<CustomTools> createBuilder() {

    return new CustomToolsBuilder();
  }

  private class CustomToolsBuilder extends JsonBuilder<CustomTools> {

    private String url;
    private List<CustomTool> tools;

    @Override
    public void setProperty(String property, JsonParser p, DeserializationContext ctxt) throws IOException {

      if (property.equals(CustomTools.PROPERTY_URL)) {
        this.url = readValueAsString(p, property, this.url);
      } else if (property.equals(CustomTools.PROPERTY_TOOLS)) {
        this.tools = readArray(p, CustomTool.class, property, this.tools);
      } else {
        super.setProperty(property, p, ctxt);
      }
    }

    @Override
    public CustomTools build() {

      return new CustomTools(this.url, this.tools);
    }
  }

}

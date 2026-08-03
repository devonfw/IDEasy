package com.devonfw.tools.ide.tool.custom;

import java.io.IOException;
import java.util.Objects;

import com.devonfw.tools.ide.json.JsonBuilder;
import com.devonfw.tools.ide.json.JsonObjectDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

/**
 * {@link JsonObjectDeserializer} for {@link CustomTool}.
 */
public class CustomToolJsonDeserializer extends JsonObjectDeserializer<CustomTool> {

  @Override
  protected JsonBuilder<CustomTool> createBuilder() {

    return new CustomToolBuilder();
  }

  private class CustomToolBuilder extends JsonBuilder<CustomTool> {

    private String name;
    private String version;
    private Boolean osAgnostic;
    private Boolean archAgnostic;
    private String url;

    @Override
    public void setProperty(String property, JsonParser p, DeserializationContext ctxt) throws IOException {

      switch (property) {
        case CustomTool.PROPERTY_NAME -> {
          this.name = readValueAsString(p, property, this.name);
        }
        case CustomTool.PROPERTY_VERSION -> {
          this.version = readValueAsString(p, property, this.version);
        }
        case CustomTool.PROPERTY_OS_AGNOSTIC -> {
          this.osAgnostic = readValueAsBoolean(p, property, this.osAgnostic);
        }
        case CustomTool.PROPERTY_ARCH_AGNOSTIC -> {
          this.archAgnostic = readValueAsBoolean(p, property, this.archAgnostic);
        }
        case CustomTool.PROPERTY_URL -> {
          this.url = readValueAsString(p, property, this.url);
        }
        default -> {
          super.setProperty(property, p, ctxt);
        }
      }
    }

    @Override
    public CustomTool build() {

      Objects.requireNonNull(this.name);
      Objects.requireNonNull(this.version);
      if (this.osAgnostic == null) {
        this.osAgnostic = Boolean.TRUE;
      }
      if (this.archAgnostic == null) {
        this.archAgnostic = Boolean.TRUE;
      }
      return new CustomTool(this.name, this.version, this.osAgnostic, this.archAgnostic, this.url);
    }
  }

}

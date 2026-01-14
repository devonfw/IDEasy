package com.devonfw.tools.ide.tool.custom;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * {@link JsonDeserializer} for {@link CustomTools}.
 */
public class CustomToolsJsonSerializer extends JsonSerializer<CustomTools> {

  @Override
  public void serialize(CustomTools customTools, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
    if (customTools == null) {
      return;
    }
    jgen.writeStartObject();
    jgen.writeFieldName(CustomTools.PROPERTY_URL);
    jgen.writeString(customTools.url());
    jgen.writeFieldName(CustomTools.PROPERTY_TOOLS);
    jgen.writeStartArray();
    List<CustomTool> tools = customTools.tools();
    for (CustomTool tool : tools) {
      jgen.writeObject(tool);
    }
    jgen.writeEndArray();
    jgen.writeEndObject();
  }

}

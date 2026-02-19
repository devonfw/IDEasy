package com.devonfw.tools.ide.tool.custom;

import java.io.IOException;
import java.util.List;

import com.devonfw.tools.ide.json.JsonObjectSerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * {@link JsonObjectSerializer} for {@link CustomTools}.
 */
public class CustomToolsJsonSerializer extends JsonObjectSerializer<CustomTools> {

  @Override
  protected void serializeProperties(CustomTools customTools, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
    jgen.writeFieldName(CustomTools.PROPERTY_URL);
    jgen.writeString(customTools.url());
    jgen.writeFieldName(CustomTools.PROPERTY_TOOLS);
    jgen.writeStartArray();
    List<CustomTool> tools = customTools.tools();
    for (CustomTool tool : tools) {
      jgen.writeObject(tool);
    }
    jgen.writeEndArray();
  }

}

package com.devonfw.tools.ide.tool.custom;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * {@link JsonDeserializer} for {@link CustomToolsJson}.
 */
public class CustomToolsJsonSerializer extends JsonSerializer<CustomToolsJson> {

  @Override
  public void serialize(CustomToolsJson customToolsJson, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
    if (customToolsJson == null) {
      return;
    }
    jgen.writeStartObject();
    jgen.writeFieldName(CustomToolsJson.PROPERTY_URL);
    jgen.writeString(customToolsJson.url());
    jgen.writeFieldName(CustomToolsJson.PROPERTY_TOOLS);
    jgen.writeStartArray();
    List<CustomToolJson> tools = customToolsJson.tools();
    for (CustomToolJson tool : tools) {
      jgen.writeObject(tool);
    }
    jgen.writeEndArray();
    jgen.writeEndObject();
  }

}

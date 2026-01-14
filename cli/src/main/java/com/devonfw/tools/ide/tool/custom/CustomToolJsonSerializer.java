package com.devonfw.tools.ide.tool.custom;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * {@link JsonDeserializer} for {@link CustomTool}.
 */
public class CustomToolJsonSerializer extends JsonSerializer<CustomTool> {

  @Override
  public void serialize(CustomTool customTool, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
    if (customTool == null) {
      return;
    }
    jgen.writeStartObject();
    jgen.writeFieldName(CustomTool.PROPERTY_NAME);
    jgen.writeString(customTool.name());
    jgen.writeFieldName(CustomTool.PROPERTY_VERSION);
    jgen.writeString(customTool.version());
    jgen.writeFieldName(CustomTool.PROPERTY_OS_AGNOSTIC);
    jgen.writeBoolean(customTool.osAgnostic());
    jgen.writeFieldName(CustomTool.PROPERTY_ARCH_AGNOSTIC);
    jgen.writeBoolean(customTool.archAgnostic());
    String url = customTool.url();
    if ((url != null) && !url.isBlank()) {
      jgen.writeFieldName(CustomTool.PROPERTY_URL);
      jgen.writeString(url);
    }
    jgen.writeEndObject();
  }

}

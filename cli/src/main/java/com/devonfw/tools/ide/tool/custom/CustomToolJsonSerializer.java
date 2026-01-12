package com.devonfw.tools.ide.tool.custom;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * {@link JsonDeserializer} for {@link CustomToolJson}.
 */
public class CustomToolJsonSerializer extends JsonSerializer<CustomToolJson> {

  @Override
  public void serialize(CustomToolJson customToolJson, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
    if (customToolJson == null) {
      return;
    }
    jgen.writeStartObject();
    jgen.writeFieldName(CustomToolJson.PROPERTY_NAME);
    jgen.writeString(customToolJson.name());
    jgen.writeFieldName(CustomToolJson.PROPERTY_VERSION);
    jgen.writeString(customToolJson.version());
    jgen.writeFieldName(CustomToolJson.PROPERTY_OS_AGNOSTIC);
    jgen.writeBoolean(customToolJson.osAgnostic());
    jgen.writeFieldName(CustomToolJson.PROPERTY_ARCH_AGNOSTIC);
    jgen.writeBoolean(customToolJson.archAgnostic());
    String url = customToolJson.url();
    if ((url != null) && !url.isBlank()) {
      jgen.writeFieldName(CustomToolJson.PROPERTY_URL);
      jgen.writeString(url);
    }
    jgen.writeEndObject();
  }

}

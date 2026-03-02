package com.devonfw.tools.ide.url.model.file.json;

import java.io.IOException;

import com.devonfw.tools.ide.json.JsonObjectSerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * {@link JsonObjectSerializer} for {@link Cve}.
 */
public class CveJsonSerializer extends JsonObjectSerializer<Cve> {

  @Override
  protected void serializeProperties(Cve cve, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
    jgen.writeStringField(Cve.PROPERTY_ID, cve.id());
    jgen.writeNumberField(Cve.PROPERTY_SEVERITY, cve.severity());
    jgen.writeFieldName(Cve.PROPERTY_VERSIONS);
    writeArray(cve.versions(), jgen);
  }
}

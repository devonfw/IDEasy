package com.devonfw.tools.ide.url.model.file.json;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.devonfw.tools.ide.json.JsonObjectSerializer;
import com.devonfw.tools.ide.version.VersionRange;
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
    Map<String, List<VersionRange>> conditions = cve.conditions();
    if (!conditions.isEmpty()) {
      jgen.writeFieldName(Cve.PROPERTY_CONDITIONS);
      jgen.writeStartObject();
      for (Map.Entry<String, List<VersionRange>> condition : conditions.entrySet()) {
        jgen.writeFieldName(condition.getKey());
        writeArray(condition.getValue(), jgen);
      }
      jgen.writeEndObject();
    }
  }
}

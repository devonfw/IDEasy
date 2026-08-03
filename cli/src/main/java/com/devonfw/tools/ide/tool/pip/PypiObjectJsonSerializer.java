package com.devonfw.tools.ide.tool.pip;

import java.io.IOException;

import com.devonfw.tools.ide.json.JsonObjectSerializer;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * {@link JsonObjectSerializer} for {@link PypiObject}.
 */
public class PypiObjectJsonSerializer extends JsonObjectSerializer<PypiObject> {

  @Override
  protected void serializeProperties(PypiObject pypiObject, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
    jgen.writeFieldName(PypiObject.PROPERTY_RELEASES);
    jgen.writeStartObject();
    for (VersionIdentifier release : pypiObject.releases()) {
      jgen.writeFieldName(release.toString());
      jgen.writeStartArray();
      jgen.writeEndArray();
    }
    jgen.writeEndObject();
  }

}

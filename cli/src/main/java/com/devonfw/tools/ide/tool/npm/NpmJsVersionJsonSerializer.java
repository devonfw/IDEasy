package com.devonfw.tools.ide.tool.npm;

import java.io.IOException;

import com.devonfw.tools.ide.json.JsonObjectSerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * {@link JsonObjectSerializer} for {@link NpmJsVersion}.
 */
public class NpmJsVersionJsonSerializer extends JsonObjectSerializer<NpmJsVersion> {

  @Override
  protected void serializeProperties(NpmJsVersion npmJsVersion, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
    jgen.writeFieldName(NpmJsVersion.PROPERTY_VERSION);
    jgen.writeString(npmJsVersion.version());
    jgen.writeFieldName(NpmJsVersion.PROPERTY_DIST);
    jgen.writeObject(npmJsVersion.dist());
  }

}

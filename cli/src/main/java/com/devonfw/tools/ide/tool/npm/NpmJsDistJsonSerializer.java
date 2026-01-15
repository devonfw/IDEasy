package com.devonfw.tools.ide.tool.npm;

import java.io.IOException;

import com.devonfw.tools.ide.json.JsonObjectSerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * {@link JsonObjectSerializer} for {@link NpmJsDist}.
 */
public class NpmJsDistJsonSerializer extends JsonObjectSerializer<NpmJsDist> {

  @Override
  protected void serializeProperties(NpmJsDist npmJsDist, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
    jgen.writeFieldName(NpmJsDist.PROPERTY_TARBALL);
    jgen.writeString(npmJsDist.tarball());
  }

}

package com.devonfw.tools.ide.tool.npm;

import java.io.IOException;

import com.devonfw.tools.ide.json.JsonObjectSerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * {@link JsonObjectSerializer} for {@link NpmJsVersions}.
 */
public class NpmJsVersionsJsonSerializer extends JsonObjectSerializer<NpmJsVersions> {

  @Override
  protected void serializeProperties(NpmJsVersions npmJsVersions, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
    jgen.writeObject(npmJsVersions.getVersionMap());
  }

}

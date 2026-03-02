package com.devonfw.tools.ide.tool.npm;

import java.io.IOException;

import com.devonfw.tools.ide.json.JsonObjectSerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * {@link JsonObjectSerializer} for {@link NpmJs}.
 */
public class NpmJsJsonSerializer extends JsonObjectSerializer<NpmJs> {

  @Override
  protected void serializeProperties(NpmJs npmJs, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
    jgen.writeFieldName(NpmJs.PROPERTY_VERSIONS);
    jgen.writeObject(npmJs.versions().getVersionMap());
  }

}

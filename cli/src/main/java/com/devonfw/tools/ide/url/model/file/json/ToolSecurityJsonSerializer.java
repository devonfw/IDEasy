package com.devonfw.tools.ide.url.model.file.json;

import java.io.IOException;

import com.devonfw.tools.ide.json.JsonObjectSerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * {@link JsonObjectSerializer} for {@link ToolSecurity}.
 */
public class ToolSecurityJsonSerializer extends JsonObjectSerializer<ToolSecurity> {

  @Override
  protected void serializeProperties(ToolSecurity toolSecurity, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {

    jgen.writeFieldName(ToolSecurity.PROPERTY_ISSUES);
    writeArray(toolSecurity.getIssues(), jgen);
  }
}

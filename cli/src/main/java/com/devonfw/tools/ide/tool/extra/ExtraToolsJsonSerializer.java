package com.devonfw.tools.ide.tool.extra;

import java.io.IOException;
import java.util.List;

import com.devonfw.tools.ide.json.JsonObjectSerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * {@link JsonObjectSerializer} for {@link ExtraTools}.
 */
public class ExtraToolsJsonSerializer extends JsonObjectSerializer<ExtraTools> {

  @Override
  protected void serializeProperties(ExtraTools extraTools, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
    List<String> toolNames = extraTools.getSortedToolNames();
    for (String tool : toolNames) {
      jgen.writeFieldName(tool);
      jgen.writeStartObject();
      List<ExtraToolInstallation> extraInstallations = extraTools.getExtraInstallations(tool);
      for (ExtraToolInstallation installation : extraInstallations) {
        jgen.writeObject(installation);
      }
      jgen.writeEndObject();
    }
  }

}

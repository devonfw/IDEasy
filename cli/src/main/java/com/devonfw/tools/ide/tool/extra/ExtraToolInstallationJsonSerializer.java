package com.devonfw.tools.ide.tool.extra;

import java.io.IOException;

import com.devonfw.tools.ide.json.JsonObjectSerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * {@link JsonObjectSerializer} for {@link ExtraToolInstallation}.
 */
public class ExtraToolInstallationJsonSerializer extends JsonObjectSerializer<ExtraToolInstallation> {

  @Override
  public void serialize(ExtraToolInstallation installation, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
    if (installation != null) {
      jgen.writeFieldName(installation.name());
    }
    super.serialize(installation, jgen, serializerProvider);
  }

  @Override
  protected void serializeProperties(ExtraToolInstallation installation, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {

    jgen.writeFieldName(ExtraToolInstallation.PROPERTY_VERSION);
    jgen.writeString(installation.version().toString());
    String edition = installation.edition();
    if (edition != null) {
      jgen.writeFieldName(ExtraToolInstallation.PROPERTY_EDITION);
      jgen.writeString(edition);
    }
  }
}

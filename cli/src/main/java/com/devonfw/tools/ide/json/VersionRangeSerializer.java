package com.devonfw.tools.ide.json;

import java.io.IOException;

import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class VersionRangeSerializer extends JsonSerializer<VersionRange> {

  @Override
  public void serialize(VersionRange versionRange, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

    jsonGenerator.writeString(versionRange.toString());
  }
}

package com.devonfw.tools.ide.json;

import java.io.IOException;

import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class VersionRangeSerializer extends JsonSerializer<VersionRange> {

  @Override
  public void serialize(VersionRange versionRange, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

    //jsonGenerator.writeStartObject();
    //jsonGenerator.writeFieldName("versions");
    //jsonGenerator.writeStartArray();
    jsonGenerator.writeString(versionRange.toString());
    //jsonGenerator.writeEndArray();
    //jsonGenerator.writeEndObject();

  }

//  @Override
//  public void serialize(UrlSecurityWarningsJson urlSecurityWarningsJson, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
//      throws IOException {
//
//    jsonGenerator.writeStartObject();
//    jsonGenerator.writeFieldName("Issues");
//    jsonGenerator.writeStartArray();
//    for (UrlSecurityWarning urlSecurityWarning : urlSecurityWarningsJson.getWarnings()) {
//      jsonGenerator.writeStartObject();
//      jsonGenerator.writeFieldName("Id");
//      jsonGenerator.writeString(urlSecurityWarning.getCveName());
//      jsonGenerator.writeFieldName("severity");
//      jsonGenerator.writeString(urlSecurityWarning.getSeverity().toString());
//      jsonGenerator.writeFieldName("versions");
//      String versionRange = urlSecurityWarning.getVersionRange().toString();
//      jsonGenerator.writeStartArray();
//      jsonGenerator.writeString(versionRange);
//      jsonGenerator.writeEndArray();
//    }
//    jsonGenerator.writeEndArray();
//    jsonGenerator.writeEndObject();
//  }
}

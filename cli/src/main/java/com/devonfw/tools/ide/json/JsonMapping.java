package com.devonfw.tools.ide.json;

import java.io.IOException;

import com.devonfw.tools.ide.tool.custom.CustomToolJson;
import com.devonfw.tools.ide.tool.custom.CustomToolJsonDeserializer;
import com.devonfw.tools.ide.tool.custom.CustomToolJsonSerializer;
import com.devonfw.tools.ide.tool.custom.CustomToolsJson;
import com.devonfw.tools.ide.tool.custom.CustomToolsJsonDeserializer;
import com.devonfw.tools.ide.tool.custom.CustomToolsJsonSerializer;
import com.devonfw.tools.ide.tool.pip.PypiObject;
import com.devonfw.tools.ide.tool.pip.PypiObjectJsonDeserializer;
import com.devonfw.tools.ide.url.model.file.json.Cve;
import com.devonfw.tools.ide.url.model.file.json.CveJsonDeserializer;
import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.url.model.file.json.ToolSecurity;
import com.devonfw.tools.ide.url.model.file.json.ToolSecurityJsonDeserializer;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Simple factory for Jackson {@link ObjectMapper} to read and write JSON with centralized mapping configuration.
 */
public class JsonMapping {

  /**
   * @return a new instance of {@link ObjectMapper} pre-configured for reasonable JSON mapping.
   */
  public static ObjectMapper create() {

    ObjectMapper mapper = new ObjectMapper();
    mapper = mapper.registerModule(new JavaTimeModule());
    mapper = mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper = mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper = mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper = mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    SimpleModule customModule = new SimpleModule();
    customModule.addDeserializer(VersionIdentifier.class, new VersionIdentifierDeserializer());
    customModule.addDeserializer(VersionRange.class, new VersionRangeDeserializer());
    customModule.addSerializer(VersionRange.class, new VersionRangeSerializer());
    customModule.addKeyDeserializer(VersionRange.class, new VersionRangeKeyDeserializer());

    customModule.addDeserializer(ToolDependency.class, new ToolDependencyDeserializer());
    customModule.addDeserializer(ToolSecurity.class, new ToolSecurityJsonDeserializer());
    customModule.addDeserializer(Cve.class, new CveJsonDeserializer());

    customModule.addDeserializer(PypiObject.class, new PypiObjectJsonDeserializer());

    customModule.addDeserializer(CustomToolJson.class, new CustomToolJsonDeserializer());
    customModule.addSerializer(CustomToolJson.class, new CustomToolJsonSerializer());
    customModule.addDeserializer(CustomToolsJson.class, new CustomToolsJsonDeserializer());
    customModule.addSerializer(CustomToolsJson.class, new CustomToolsJsonSerializer());
    mapper = mapper.registerModule(customModule);
    return mapper;
  }

  /**
   * @param p the {@link JsonParser}.
   * @throws IOException in case of an error.
   */
  public static void skipCurrentField(JsonParser p) throws IOException {

    JsonToken jsonToken = p.nextToken();
    if ((jsonToken == JsonToken.START_OBJECT) || (jsonToken == JsonToken.START_ARRAY)) {
      p.skipChildren();
    }
  }

}

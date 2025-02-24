package com.devonfw.tools.ide.json;

import com.devonfw.tools.ide.tool.repository.CustomToolJson;
import com.devonfw.tools.ide.tool.repository.CustomToolJsonDeserializer;
import com.devonfw.tools.ide.tool.repository.CustomToolJsonSerializer;
import com.devonfw.tools.ide.tool.repository.CustomToolsJson;
import com.devonfw.tools.ide.tool.repository.CustomToolsJsonDeserializer;
import com.devonfw.tools.ide.tool.repository.CustomToolsJsonSerializer;
import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.annotation.JsonInclude;
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
    customModule.addDeserializer(ToolDependency.class, new ToolDependencyDeserializer());
    customModule.addDeserializer(CustomToolJson.class, new CustomToolJsonDeserializer());
    customModule.addDeserializer(CustomToolsJson.class, new CustomToolsJsonDeserializer());
    customModule.addSerializer(CustomToolJson.class, new CustomToolJsonSerializer());
    customModule.addSerializer(CustomToolsJson.class, new CustomToolsJsonSerializer());
    customModule.addKeyDeserializer(VersionRange.class, new VersionRangeKeyDeserializer());
    mapper = mapper.registerModule(customModule);
    return mapper;
  }

}

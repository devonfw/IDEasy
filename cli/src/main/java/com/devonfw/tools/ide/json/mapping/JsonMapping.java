package com.devonfw.tools.ide.json.mapping;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
    return mapper;
  }

}
